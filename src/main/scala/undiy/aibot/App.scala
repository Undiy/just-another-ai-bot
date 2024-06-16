package undiy.aibot

import cats.effect.{ExitCode, IO, IOApp}
import org.typelevel.otel4s.trace.Tracer
import skunk.Session
import undiy.aibot.ai.{AIService, OpenAIService}
import undiy.aibot.context.{ContextService, DbContextService}

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
          given Session[IO] = session
          given AIService[IO] = OpenAIService(config.ai)
          given ContextService[IO] = DbContextService[IO]

          AIBot.start[IO](config.bot).as(ExitCode.Success)
        })
    })
  }
}
