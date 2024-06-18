package undiy.aibot.bot

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.*
import telegramium.bots.high.implicits.*
import telegramium.bots.high.messageentities.MessageEntities
import telegramium.bots.high.{Api, BotApi, LongPollBot}
import undiy.aibot.BotConfig
import undiy.aibot.ai.AIService
import undiy.aibot.bot.TelegramModelExt.*
import undiy.aibot.context.ContextService

class AIBot[F[_]: Async: Parallel](using
    bot: Api[F],
    aiService: AIService[F],
    contextService: ContextService[F]
) extends LongPollBot[F](bot) {

  private val logger = org.log4s.getLogger

  private enum AIBotCommand(
      val command: String,
      val description: String,
      val action: (Message, String) => F[Unit]
  ) {
    case Prompt
        extends AIBotCommand(
          command = "prompt",
          description =
            "Make a simple prompt with no additional context (message history)",
          // TODO refactor this part
          action = (msg, prompt) => {
            if (!prompt.isBlank) {
              for {
                thinkingMessage <- sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = "\uD83E\uDD14" // thinking emoji
                ).exec
                response <- aiService.makeCompletion(prompt)
                _ <- deleteMessage(
                  chatId = ChatIntId(msg.chat.id),
                  messageId = thinkingMessage.messageId
                ).exec
                newMessage <- sendMessage(
                  chatId = ChatIntId(msg.chat.id),
                  text = response
                ).exec
              } yield {}
            } else {
              sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text =
                  "Please write the actual prompt after the /prompt command"
              ).exec.void
            }
          }
        )

    case ResetContext
        extends AIBotCommand(
          command = "resetcontext",
          description =
            "Deletes all the bot's message context for this chat. WARNING: this cannot be undone",
          // TODO add confirmation
          action = (msg, _) => contextService.deleteContextMessages(msg.chat.id)
        )

    private def toBotCommand: BotCommand = BotCommand(command, description)
  }

  private object AIBotCommand {
    def fromString(command: String): Option[AIBotCommand] =
      values.find(_.command == command)

    def setCommands(): F[Boolean] = setMyCommands(
      commands = values.map(_.toBotCommand).toList
    ).exec
  }

  private def onCommand(
      msg: Message,
      command: String,
      args: String
  ): F[Unit] = {
    AIBotCommand.fromString(command) match {
      case Some(botCommand) => botCommand.action(msg, args)
      case None =>
        sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = s"Unknown command: $command"
        ).exec.void
    }
  }

  private def onPrivateChatMessage(msg: Message): F[Unit] = {
    for {
      thinkingMessage <- sendMessage(
        chatId = ChatIntId(msg.chat.id),
        text = "\uD83E\uDD14" // thinking emoji
      ).exec
      _ <- contextService.saveContextMessage(msg.toContextMessage)
      contextMessages <- contextService.getContextMessages(msg.chat.id)
      response <- aiService.makeChatCompletion(contextMessages.reverse)
      _ <- deleteMessage(
        chatId = ChatIntId(msg.chat.id),
        messageId = thinkingMessage.messageId
      ).exec
      newMessage <- sendMessage(
        chatId = ChatIntId(msg.chat.id),
        text = response
      ).exec
      _ <- contextService.saveContextMessage(newMessage.toContextMessage)
    } yield {}
  }

  private def onChatMessage(msg: Message): F[Unit] = {
    // TODO cache botUser
    getMe().exec.flatMap({ botUser =>
      if (msg.hasMentionForUser(botUser)) {
        // request chat completion
        for {
          thinkingMessage <- sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = "\uD83E\uDD14" // thinking emoji
          ).exec
          _ <- contextService.saveContextMessage(msg.toContextMessage)
          contextMessages <- contextService.getContextMessages(msg.chat.id)
          response <- aiService.makeChatCompletion(contextMessages.reverse)
          _ <- deleteMessage(
            chatId = ChatIntId(msg.chat.id),
            messageId = thinkingMessage.messageId
          ).exec
          responseEntities = MessageEntities()
            .mention(s"@${msg.from.get.username.getOrElse("")}")
            .plain(" ")
            .plain(response)

          newMessage <- sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = responseEntities.toPlainText(),
            entities = responseEntities.toTelegramEntities()
          ).exec
          _ <- contextService.saveContextMessage(newMessage.toContextMessage)
        } yield {}
      } else {
        // just save context message
        contextService.saveContextMessage(msg.toContextMessage)
      }
    })
  }

  override def onMessage(msg: Message): F[Unit] = {
    msg.text match {
      // handle only messages with non-empty text
      case Some(text) if !text.isBlank =>
        msg.getCommand match {
          case Some((command, args)) => onCommand(msg, command, args)
          case None =>
            if (msg.chat.isPrivate) {
              onPrivateChatMessage(msg)
            } else {
              onChatMessage(msg)
            }
        }
      case _ => summon[Async[F]].unit
    }
  }

  // Unfortunately, telegram bot API don't provide updates for deleted messages, only about edited
  override def onEditedMessage(msg: Message): F[Unit] = {
    // only update regular messages, skip commands
    if (!msg.hasCommand) {
      contextService.saveContextMessage(msg.toContextMessage)
    } else {
      summon[Async[F]].unit
    }
  }

  override def start(): F[Unit] = for {
    _ <- AIBotCommand.setCommands()
    _ <- super.start()
  } yield {}
}

object AIBot {
  def start[F[_]: Async: Parallel](
      config: BotConfig
  )(using AIService[F], ContextService[F]): F[Unit] = {
    BlazeClientBuilder[F].resource.use { httpClient =>
      val http = Logger(
        logBody = config.log.body,
        logHeaders = config.log.headers
      )(httpClient)

      given Api[F] =
        BotApi(http, baseUrl = s"https://api.telegram.org/bot${config.token}")

      AIBot[F].start()
    }
  }
}