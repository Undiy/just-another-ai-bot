package undiy.aibot

import ai.{AIService, OpenAIService}
import cats.effect.{ExitCode, IO, IOApp}

object App extends IOApp {
  private val logger = org.log4s.getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Start!")

    for {
      config <- Config.load
      given AIService[IO] = OpenAIService(config.ai)
      _ <- AIBot.start[IO](config.bot)
    } yield {
      ExitCode.Success
    }
  }
}
