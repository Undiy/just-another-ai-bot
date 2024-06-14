package ai

import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.effect.IO
import io.cequence.openaiscala.domain.settings.CreateChatCompletionSettings
import io.cequence.openaiscala.domain.{SystemMessage, UserMessage}
import io.cequence.openaiscala.service.OpenAIChatCompletionServiceFactory
import undiy.aibot.AIConfig

import scala.concurrent.ExecutionContext

class OpenAIService(
    config: AIConfig
) extends AIService[IO] {

  private val system = ActorSystem("openai-client-system")

  private given ExecutionContext = system.dispatcher

  private given Materializer = Materializer(system)

  private val aiService = OpenAIChatCompletionServiceFactory(
    coreUrl = config.baseUrl,
    authHeaders = Seq(("Authorization", s"Bearer ${config.apiKey}"))
  )

  override def makeCompletion(prompt: String): IO[String] = {
    IO.fromFuture(
      IO(
        aiService
          .createChatCompletion(
            messages = Seq(
              SystemMessage("You are a kind helpful assistant."),
              UserMessage(prompt)
            ),
            settings = CreateChatCompletionSettings(
              model = config.model,
              max_tokens = Some(250)
            )
          )
          .map { chatCompletion =>
            // trim the response if it didn't fit into limit
            val response = chatCompletion.choices.head.message.content
            response.take(response.lastIndexOf('\n'))
          }
      )
    )
  }
}
