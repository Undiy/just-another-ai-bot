package undiy.aibot.ai

trait AIService[F[_]] {
  def makeCompletion(prompt: String): F[String]
}
