package undiy.aibot.ai

import fs2.Stream
import undiy.aibot.context.model.ContextMessage

trait AIService[F[_]] {
  def makeCompletion(prompt: String): F[String]

  def makeChatCompletion(messages: List[ContextMessage]): F[String]

  def makeCompletionStreamed(prompt: String): Stream[F, String]

  def makeChatCompletionStreamed(messages: List[ContextMessage]): Stream[F, String]
}
