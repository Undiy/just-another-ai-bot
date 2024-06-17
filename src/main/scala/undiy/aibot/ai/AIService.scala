package undiy.aibot.ai

import undiy.aibot.context.model.ContextMessage

trait AIService[F[_]] {
  def makeCompletion(prompt: String): F[String]

  def makeChatCompletion(messages: List[ContextMessage]): F[String]
}
