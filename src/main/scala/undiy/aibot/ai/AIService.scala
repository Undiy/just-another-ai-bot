package ai

import cats.effect.Async


trait AIService[F[_]: Async] {
  def makeCompletion(prompt: String): F[String]
}
