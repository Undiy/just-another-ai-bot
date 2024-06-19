package undiy.aibot.ai

import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.effect.IO
import io.cequence.openaiscala.domain.settings.CreateChatCompletionSettings
import io.cequence.openaiscala.domain.{
  AssistantMessage,
  SystemMessage,
  UserMessage
}
import io.cequence.openaiscala.service.OpenAIChatCompletionServiceFactory
import undiy.aibot.AIConfig
import undiy.aibot.context.model.ContextMessage

import scala.concurrent.ExecutionContext

final class OpenAIService(
    config: AIConfig
)(using ec: ExecutionContext)
    extends AIService[IO] {

  private val logger = org.log4s.getLogger

  private given Materializer = Materializer(ActorSystem("openai-client-system"))

  private val aiService = OpenAIChatCompletionServiceFactory(
    coreUrl = config.baseUrl,
    authHeaders = Seq(("Authorization", s"Bearer ${config.apiKey}"))
  )

  override def makeCompletion(prompt: String): IO[String] = IO.fromFuture(
    IO(
      aiService
        .createChatCompletion(
          messages = Seq(
            SystemMessage("You are a kind helpful assistant."),
            UserMessage(prompt)
          ),
          // TODO add configurable limit
          settings = CreateChatCompletionSettings(
            model = config.model
          )
        )
        .map { chatCompletion =>
          logger.info(s"Response: ${chatCompletion.choices.head}")
          chatCompletion.choices.head.message.content
        }
    )
  )

  override def makeChatCompletion(messages: List[ContextMessage]): IO[String] =
    IO.fromFuture(
      IO {
        val chatMessages =
          SystemMessage("You are a helpful chat bot") :: messages.map({ m =>
            if (m.user.isBot) {
              AssistantMessage(m.content, m.user.username)
            } else {
              UserMessage(m.content, m.user.username)
            }
          })

        logger.info(s"Mesages:\n${chatMessages.mkString("\n")}")

        aiService
          .createChatCompletion(
            messages = chatMessages,
            // TODO add configurable limit
            settings = CreateChatCompletionSettings(
              model = config.model
            )
          )
          .map { chatCompletion =>

            logger.info(s"Response: ${chatCompletion.choices.head}")

            chatCompletion.choices.head.message.content
          }
      }
    )
}
