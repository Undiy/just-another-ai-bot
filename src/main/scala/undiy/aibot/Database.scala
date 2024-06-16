package undiy.aibot

import cats.effect.std.Console
import cats.effect.{Async, Resource, Sync}
import dumbo.{ConnectionConfig, Dumbo}
import fs2.io.net.Network
import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object Database {
  private val logger = org.log4s.getLogger

  def init[F[_]: Async: Tracer: Network: Console](
      config: DbConfig
  ): Resource[F, Session[F]] = {

    for {
      migration <- Resource.eval(
        Dumbo
          .withResourcesIn[F]("db/migration")
          .apply(
            connection = ConnectionConfig(
              host = config.host,
              port = config.port,
              user = config.user,
              database = config.database,
              password = Some(config.password),
              ssl =
                skunk.SSL.None // skunk.SSL config, default is skunk.SSL.None
            )
          )
          .runMigration
      )

      _ = logger.info(
        s"Migration completed with ${migration.migrationsExecuted} migrations"
      )

      session <- Session.single[F](
        host = config.host,
        port = config.port,
        user = config.user,
        database = config.database,
        password = Some(config.password)
      )
    } yield {
      session
    }
  }
}
