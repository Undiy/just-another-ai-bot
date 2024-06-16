package undiy.aibot

import pureconfig.generic.*
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

import scala.deriving.Mirror

// support for default values of case clases https://github.com/pureconfig/pureconfig/issues/1673#issuecomment-2158978556
extension (c: ConfigReader.type)
  inline def derived[A: Mirror.Of: ProductHint: CoproductHint]
      : ConfigReader[A] = deriveReader[A]

case class BotConfig(
    token: String,
    log: BotRequestLogConfig = BotRequestLogConfig()
)

case class BotRequestLogConfig(
    body: Boolean = false,
    headers: Boolean = false
)

case class AIConfig(
    baseUrl: String,
    apiKey: String,
    model: String
)

case class DbConfig(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
)

case class Config(
    bot: BotConfig,
    ai: AIConfig,
    db: DbConfig
) derives ConfigReader

object Config {
  def load(): Config = ConfigSource.default.loadOrThrow[Config]
}
