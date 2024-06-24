package undiy.aibot.ai

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import cats.effect.Async
import fs2.Stream
import io.cequence.openaiscala.domain.settings.CreateChatCompletionSettings
import io.cequence.openaiscala.domain.{
  AssistantMessage,
  SystemMessage,
  UserMessage
}
import io.cequence.openaiscala.service.OpenAIChatCompletionServiceFactory
import io.cequence.openaiscala.service.OpenAIStreamedServiceImplicits.given
import undiy.aibot.AIConfig
import undiy.aibot.context.model.ContextMessage

import scala.concurrent.ExecutionContext

/** AIService implementation that makes request to openai-compatible chat
  * completion API
  * @param config
  *   configuration
  * @param async$F$0
  *   async typeclass
  * @param ec
  *   execution context
  * @tparam F
  *   effect type
  */
final class OpenAIService[F[_]: Async](
    config: AIConfig
)(using ec: ExecutionContext)
    extends AIService[F] {

  private val logger = org.log4s.getLogger

  private given Materializer = Materializer(ActorSystem("openai-client-system"))

  private val aiService = OpenAIChatCompletionServiceFactory.withStreaming(
    coreUrl = config.baseUrl,
    authHeaders = Seq(("Authorization", s"Bearer ${config.apiKey}"))
  )

  override def makeCompletion(prompt: String): F[String] = Async[F].fromFuture(
    Async[F].delay(
      aiService
        .createChatCompletion(
          messages = Seq(
            SystemMessage("You are a kind helpful assistant."),
            UserMessage(prompt)
          ),
          settings = CreateChatCompletionSettings(
            model = config.model,
            max_tokens = config.maxTokens
          )
        )
        .map { chatCompletion =>
          logger.info(s"Response: ${chatCompletion.choices.head}")
          chatCompletion.choices.head.message.content
        }
    )
  )

  override def makeCompletionStreamed(prompt: String): Stream[F, String] = {
    aiService
      .createChatCompletionStreamed(
        messages = Seq(
          SystemMessage("You are a kind helpful assistant."),
          UserMessage(prompt)
        ),
        settings = CreateChatCompletionSettings(
          model = config.model,
          max_tokens = config.maxTokens
        )
      )
      .mapConcat { chatCompletion =>
        logger.info(s"Response: ${chatCompletion.choices.head}")
        chatCompletion.choices.head.delta.content
      }
      .toStream
  }

  override def makeChatCompletion(messages: List[ContextMessage]): F[String] =
    Async[F].fromFuture(
      Async[F].delay({
        val chatMessages =
          SystemMessage("You are a helpful chat bot") :: messages.map({ m =>
            if (m.user.isBot) {
              AssistantMessage(m.content, m.user.username)
            } else {
              UserMessage(m.content, m.user.username)
            }
          })

        logger.info(s"Messages:\n${chatMessages.mkString("\n")}")

        aiService
          .createChatCompletion(
            messages = chatMessages,
            settings = CreateChatCompletionSettings(
              model = config.model,
              max_tokens = config.maxTokens
            )
          )
          .map { chatCompletion =>

            logger.info(s"Response: ${chatCompletion.choices.head}")

            chatCompletion.choices.head.message.content
          }
      })
    )

  override def makeChatCompletionStreamed(
      messages: List[ContextMessage]
  ): Stream[F, String] = {
    val chatMessages =
      SystemMessage("You are a helpful chat bot") :: messages.map({ m =>
        if (m.user.isBot) {
          AssistantMessage(m.content, m.user.username)
        } else {
          UserMessage(m.content, m.user.username)
        }
      })

    logger.info(s"Messages:\n${chatMessages.mkString("\n")}")

    aiService
      .createChatCompletionStreamed(
        messages = chatMessages,
        settings = CreateChatCompletionSettings(
          model = config.model,
          max_tokens = config.maxTokens
        )
      )
      .mapConcat { chatCompletion =>
        logger.info(s"Response: ${chatCompletion.choices.head}")
        chatCompletion.choices.head.delta.content
      }
      .toStream
  }

  // based on https://github.com/krasserm/streamz/blob/master/streamz-converter/src/main/scala/streamz/converter/Converter.scala
  // the library has no artifacts for scala 3, so I've implemented converter by example
  extension [A](source: Source[A, NotUsed]) {
    private def toStream: Stream[F, A] = Stream.force {
      Async[F].delay {
        val subscriber = source.toMat(Sink.queue[A]())(Keep.right).run()
        val pull = Async[F].fromFuture(Async[F].delay(subscriber.pull()))
        val cancel = Async[F].delay(subscriber.cancel())
        Stream.repeatEval(pull).unNoneTerminate.onFinalize(cancel)
      }
    }
  }
}
