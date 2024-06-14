package undiy.aibot

import ai.AIService
import cats.Parallel
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.high.implicits.*
import telegramium.bots.high.{Api, BotApi, LongPollBot}
import telegramium.bots.{ChatIntId, Message}

class AIBot[F[_] : Async : Parallel](using bot: Api[F], aiService: AIService[F])
    extends LongPollBot[F](bot) {
    override def onMessage(msg: Message): F[Unit] = {
      msg.text match {
        case Some(prompt) if !prompt.isBlank => {
          for {
            m <- sendMessage(
              chatId = ChatIntId(msg.chat.id),
              text = "\uD83E\uDD14" // thinking emoji
            ).exec
            result <- aiService.makeCompletion(prompt)
            _ <- (
              deleteMessage(
                chatId = ChatIntId(msg.chat.id),
                messageId = m.messageId
              ).exec,
              sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = result
              ).exec
            ).parMapN((_, _) => ())
          } yield {}
        }
        case _ => summon[Async[F]].unit
      }
    }
}

object AIBot {
  def start[F[_] : Async : Parallel](config: BotConfig)(using AIService[F]): F[Unit] = {
    BlazeClientBuilder[F].resource.use { httpClient =>
      val http = Logger(logBody = config.log.body, logHeaders = config.log.headers)(httpClient)

      given Api[F] =
        BotApi(http, baseUrl = s"https://api.telegram.org/bot${config.token}")

      AIBot[F].start()
    }
  }
}