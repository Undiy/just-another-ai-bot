package undiy.aibot.bot

import cats.effect.Async
import cats.syntax.all.*
import fs2.Stream
import telegramium.bots.high.Api
import telegramium.bots.high.Methods.{deleteMessage, sendMessage}
import telegramium.bots.high.implicits.*
import telegramium.bots.{Chat, ChatIntId, Message}
import undiy.aibot.BotConfig
import undiy.aibot.ai.AIService
import undiy.aibot.bot.TelegramModelExt.*
import undiy.aibot.context.ContextService

trait AIBotRequestHelper[F[_]: Async: Api](using
    aiService: AIService[F],
    contextService: ContextService[F]
) {
  private val logger = org.log4s.getLogger

  val config: BotConfig

  def requestCompletion(chat: Chat, prompt: String, onResponse: String => F[Unit]): F[Unit] = {
    if (config.streaming) {
      performAIServiceStreamedRequest(
        chat = chat,
        request = Async[F].pure(aiService.makeCompletionStreamed(prompt)),
        onResponsePart = onResponse
      )
    } else {
      performAIServiceRequest(
        chat = chat,
        request = aiService.makeCompletion(prompt),
        onResponse = onResponse
      )
    }
  }

  def requestChatCompletion(msg: Message, onResponse: String => F[Unit]): F[Unit] = {
    if (config.streaming) {
      performAIServiceStreamedRequest(
        chat = msg.chat,
        request = for {
          _ <- contextService.saveContextMessage(msg.toContextMessage)
          contextMessages <- contextService
            .getContextMessages(msg.chat.id, config.maxContextMessages)
        } yield aiService.makeChatCompletionStreamed(contextMessages.reverse),
        onResponsePart = onResponse
      )
    } else {
      performAIServiceRequest(
        chat = msg.chat,
        for {
          _ <- contextService.saveContextMessage(msg.toContextMessage)
          contextMessages <- contextService
            .getContextMessages(msg.chat.id, config.maxContextMessages)
          response <- aiService.makeChatCompletion(contextMessages.reverse)
        } yield response,
        onResponse = onResponse
      )
    }
  }

  private def performAIServiceRequest(chat: Chat, request: F[String], onResponse: String => F[Unit]): F[Unit] = for {
    thinkingMessage <- sendMessage(
      chatId = ChatIntId(chat.id),
      text = config.messages.processing
    ).exec
    _ <- request.redeemWith(
      { e =>
        logger.warn(e)("Failed to perform AI request")
        sendMessage(
          chatId = ChatIntId(chat.id),
          text = config.messages.error
        ).exec.void
      },
      // we're interested only in non-empty response
      response => if (response.trim.nonEmpty) onResponse(response) else Async[F].unit
    )
    _ <- deleteMessage(
      chatId = ChatIntId(chat.id),
      messageId = thinkingMessage.messageId
    ).exec
  } yield {}

  private def performAIServiceStreamedRequest(
      chat: Chat,
      request: F[Stream[F, String]],
      onResponsePart: String => F[Unit]
  ): F[Unit] = for {
    thinkingMessage <- sendMessage(
      chatId = ChatIntId(chat.id),
      text = config.messages.processing
    ).exec
    stream <- request
    _ <- stream
      // stream come in tokens, gluing them back to paragraphs
      // 1. split on newlines leaving the separators
      .flatMap { s => Stream(s.split("\n", -1).flatMap(List("\n", _)).drop(1)*) }
      // 2. group text separated by newlines in chunks
      .groupAdjacentBy(_ == "\n")
      // 3. glue the chunks
      .map { case (_, chunk) => Stream.chunk(chunk).compile.string }
      .filter(_.trim.nonEmpty)
      .foreach(onResponsePart)
      .compile
      .drain
      .recoverWith { e =>
        logger.warn(e)("Failed to perform AI request")
        sendMessage(
          chatId = ChatIntId(chat.id),
          text = config.messages.error
        ).exec.void
      }
    _ <- deleteMessage(
      chatId = ChatIntId(chat.id),
      messageId = thinkingMessage.messageId
    ).exec
  } yield {}
}
