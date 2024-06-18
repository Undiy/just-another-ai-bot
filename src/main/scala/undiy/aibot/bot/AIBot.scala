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
import undiy.aibot.context.ContextService
import undiy.aibot.bot.TelegramModelExt.*

class AIBot[F[_]: Async: Parallel](using
    bot: Api[F],
    aiService: AIService[F],
    contextService: ContextService[F]
) extends LongPollBot[F](bot) {

  private val logger = org.log4s.getLogger

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
      case Some(text) if !text.isBlank => {
        if (msg.hasCommand) {
          onCommand(msg)
        } else if (msg.chat.isPrivate) {
          onPrivateChatMessage(msg)
        } else {
          onChatMessage(msg)
        }
      }
      case _ => summon[Async[F]].unit
    }
  }

  override def onEditedMessage(msg: Message): F[Unit] = {
    // only update regular messages, skip commands
    if (!msg.hasCommand) {
      contextService.saveContextMessage(msg.toContextMessage)
    } else {
      summon[Async[F]].unit
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
