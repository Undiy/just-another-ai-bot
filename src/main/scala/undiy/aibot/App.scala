package undiy.aibot

import ai.{AIService, OpenAIService}
import cats.effect.{ExitCode, IO, IOApp}
import org.typelevel.otel4s.trace.Tracer

object App extends IOApp {
  private val logger = org.log4s.getLogger

  given Tracer[IO] = Tracer.noop[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Start!")

    IO {
      Config.load()
    }.flatMap(config => {
      Database
        .init[IO](config.db)
        .use(session => {
          given AIService[IO] = OpenAIService(config.ai)

          AIBot.start[IO](config.bot).as(ExitCode.Success)
        })
    })
  }
}
