package undiy.aibot.fake

import cats.effect.IO
import fs2.Stream
import undiy.aibot.ai.AIService
import undiy.aibot.context.model.ContextMessage
import undiy.aibot.fake.FakeData.{sampleResponse, sampleStreamResponse}

class FakeAIService extends AIService[IO] {
  override def makeCompletion(prompt: String): IO[String] = IO(sampleResponse)

  override def makeChatCompletion(messages: List[ContextMessage]): IO[String] = IO(sampleResponse)

  override def makeCompletionStreamed(prompt: String): Stream[IO, String] = sampleStreamResponse

  override def makeChatCompletionStreamed(messages: List[ContextMessage]): Stream[IO, String] = sampleStreamResponse
}
