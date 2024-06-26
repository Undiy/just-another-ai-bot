package undiy.aibot

import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.syntax.all.*
import org.typelevel.otel4s.trace.Tracer
import skunk.Session
import undiy.aibot.ai.{AIService, OpenAIService}
import undiy.aibot.bot.AIBot
import undiy.aibot.context.{ContextService, DbContextService}

object App extends IOApp {
  private val logger = org.log4s.getLogger

  given Tracer[IO] = Tracer.noop[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Start!")

    (
      IO(Config.load()),
      Async[IO].executionContext
    ).flatMapN { case (config, ec) =>
      logger.info("Config loaded")
      Database
        .init[IO](config.db)
        .use { session =>
          logger.info("DB connection ready")
          given Session[IO] = session

          given AIService[IO] = OpenAIService(config.ai)(using Async[IO], ec)

          given ContextService[IO] = DbContextService[IO]

          AIBot.start[IO](config.bot).as(ExitCode.Success)
        }
    }
  }
}
