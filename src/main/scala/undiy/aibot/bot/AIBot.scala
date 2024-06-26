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

/** Telegram bot implementation
  * @param config
  *   telegram bot configuration
  * @param async$F$0
  *   async typeclass
  * @param parallel$F$1
  *   parallel typeclass
  * @param bot
  *   telegram bot API
  * @param aiService
  *   AI service
  * @param contextService
  *   Context service
  * @tparam F
  *   effect type
  */
class AIBot[F[_]: Async: Parallel](val config: BotConfig)(using
    bot: Api[F],
    aiService: AIService[F],
    contextService: ContextService[F]
) extends LongPollBot[F](bot)
    with AIBotRequestHelper[F]
    with AIBotCommands[F] {

  private val logger = org.log4s.getLogger

  private def onCommand(msg: Message, command: String, args: String): F[Unit] = {
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
    requestChatCompletion(
      msg = msg,
      onResponse = response =>
        for {
          newMessage <- sendMessage(
            chatId = ChatIntId(msg.chat.id),
            text = response
          ).exec
          _ <- contextService.saveContextMessage(newMessage.toContextMessage)
        } yield {}
    )
  }

  private def onChatMessage(msg: Message): F[Unit] = {
    // TODO cache botUser
    getMe().exec.flatMap { botUser =>
      if (msg.hasMentionForUser(botUser)) {
        // request chat completion
        requestChatCompletion(
          msg = msg,
          onResponse = { response =>
            val user = msg.from.get
            val responseEntities = MessageEntities()
              .textMention(
                user.username match {
                  case Some(username) => s"@$username"
                  case None           => user.firstName
                },
                user
              )
              .plain(" ")
              .plain(response)
            for {
              newMessage <- sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = responseEntities.toPlainText(),
                entities = responseEntities.toTelegramEntities()
              ).exec
              _ <- contextService.saveContextMessage(
                newMessage.toContextMessage
              )
            } yield {}
          }
        )
      } else {
        // just save context message
        contextService.saveContextMessage(msg.toContextMessage)
      }
    }
  }

  override def onMessage(msg: Message): F[Unit] = {
    msg.text match {
      // handle only messages with non-empty text
      case Some(text) if text.trim.nonEmpty =>
        msg.getCommand match {
          case Some((command, args)) => onCommand(msg, command, args)
          case None =>
            if (msg.chat.isPrivate) {
              onPrivateChatMessage(msg)
            } else {
              onChatMessage(msg)
            }
        }
      case _ => Async[F].unit
    }
  }

  // Unfortunately, telegram bot API don't provide updates for deleted messages, only about edited
  override def onEditedMessage(msg: Message): F[Unit] = {
    // only update regular messages, skip commands
    if (!msg.hasCommand) {
      contextService.saveContextMessage(msg.toContextMessage)
    } else {
      Async[F].unit
    }
  }

  override def start(): F[Unit] = for {
    _ <- Async[F].delay(logger.info("Setting bot description..."))
    _ <- setMyShortDescription(config.about).exec
    _ <- setMyDescription(config.description).exec
    _ = logger.info("Setting bot commands...")
    _ <- AIBotCommand.setCommands()
    _ = logger.info("Ready to start bot")
    _ <- super.start()
  } yield {}
}

object AIBot {

  /** Starts telegram bot
    * @param config
    *   telegram bot configuration
    * @param x$2
    *   AI service
    * @param x$3
    *   Context service
    * @tparam F
    *   effect type
    * @return
    *   async (shouldn't terminate while the bot is running)
    */
  def start[F[_]: Async: Parallel](config: BotConfig)(using AIService[F], ContextService[F]): F[Unit] = {
    BlazeClientBuilder[F].resource.use { httpClient =>
      val http = Logger(
        logBody = config.log.body,
        logHeaders = config.log.headers
      )(httpClient)

      given Api[F] = BotApi(http, baseUrl = s"https://api.telegram.org/bot${config.token}")

      AIBot[F](config).start()
    }
  }
}
