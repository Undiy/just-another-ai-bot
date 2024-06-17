package undiy.aibot

import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.*
import telegramium.bots.high.implicits.*
import telegramium.bots.high.messageentities.MessageEntities
import telegramium.bots.high.{Api, BotApi, LongPollBot}
import undiy.aibot.ai.AIService
import undiy.aibot.context.ContextService
import undiy.aibot.context.model.{ContextChat, ContextMessage, ContextUser}

import java.time.{Instant, OffsetDateTime, ZoneOffset}

class AIBot[F[_]: Async: Parallel](using
    bot: Api[F],
    aiService: AIService[F],
    contextService: ContextService[F]
) extends LongPollBot[F](bot) {

  private val logger = org.log4s.getLogger

  extension (msg: Message) {
    private def toContextMessage: ContextMessage = ContextMessage(
      messageId = msg.messageId,
      content = msg.text.getOrElse(""),
      createdAt = OffsetDateTime
        .ofInstant(Instant.ofEpochSecond(msg.date), ZoneOffset.UTC),
      chat = msg.chat.toContextChat,
      user = msg.from.get.toContextUser
    )

    private def hasCommand: Boolean = msg.entities.exists({
      case command: BotCommandMessageEntity => true
      case _                                => false
    })

    private def isPrivate: Boolean = msg.chat.`type` == "private"

    private def getEntityContent(messageEntity: MessageEntity): String =
      msg.text.get.slice(
        messageEntity.offset,
        messageEntity.offset + messageEntity.length
      )
  }

  extension (user: User) {
    private def toContextUser: ContextUser = user match {
      case User(id, isBot, _, _, username, _, _, _, _, _, _, _) =>
        ContextUser(id, isBot, username)
    }
  }

  extension (chat: Chat) {
    private def toContextChat: ContextChat = chat match {
      case Chat(id, _, title, _, _, _, _) => ContextChat(id, title)
    }
  }

  private def onCommand(msg: Message): F[Unit] = {
    // TODO
    summon[Async[F]].unit
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
    getMe().exec.flatMap({ botUser =>
      if (
        msg.entities.exists {
          case entity @ MentionMessageEntity(offset, length) =>
            msg.getEntityContent(entity) == s"@${botUser.username.getOrElse("")}"
          case TextMentionMessageEntity(offset, length, user) => user == botUser
          case _                                              => false
        }
      ) {
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
      case Some(prompt) if !prompt.isBlank => {
        if (msg.hasCommand) {
          onCommand(msg)
        } else if (msg.isPrivate) {
          onPrivateChatMessage(msg)
        } else {
          onChatMessage(msg)
        }
      }
      case _ => summon[Async[F]].unit
    }
  }
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
