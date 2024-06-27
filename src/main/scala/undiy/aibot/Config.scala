package undiy.aibot

import pureconfig.generic.*
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

import scala.deriving.Mirror

// support for default values of case clases https://github.com/pureconfig/pureconfig/issues/1673#issuecomment-2158978556
extension (c: ConfigReader.type)
  inline def derived[A: Mirror.Of: ProductHint: CoproductHint]
      : ConfigReader[A] = deriveReader[A]

/** Telegram bot configuration
  * @param token
  *   telegram bot token
  * @param streaming
  *   if true, streaming requests to AI service would be used (needs to be
  *   supported by AI API)
  * @param about
  *   content of telegram bot "about" field
  * @param description
  *   telegram bot description
  * @param maxContextMessages
  *   max number of messages that would be used as a context for chat
  *   completions
  * @param messages
  *   templates for telegram bot replies
  * @param log
  *   telegram bot client logging settings
  */
case class BotConfig(
    token: String,
    streaming: Boolean = true,
    about: Option[String] = None,
    description: Option[String] = None,
    maxContextMessages: Option[Int] = None,
    messages: BotMessages = BotMessages(),
    log: BotRequestLogConfig = BotRequestLogConfig()
)

/** Templates for telegram bot replies
  * @param processing
  *   text for message that would be sent by bot when it excepts a prompt
  * @param error
  *   text for message that would be sent by bot when error occurs
  */
case class BotMessages(
    processing: String = "\uD83E\uDD14", // thinking emoji
    error: String = "Something went wrong, try again later."
)

/** Telegram bot client logging settings
  * @param body
  *   print request body in log
  * @param headers
  *   print request headers in log
  */
case class BotRequestLogConfig(
    body: Boolean = false,
    headers: Boolean = false
)

/** AI service configuration
  * @param baseUrl
  *   url of openai-compatible API
  * @param apiKey
  *   key for AI API
  * @param model
  *   model to be used for completions
  * @param maxTokens
  *   max tokens for context and AI service reply; if the answer won't fit it
  *   would be truncated
  */
case class AIConfig(
    baseUrl: String,
    apiKey: String,
    model: String,
    maxTokens: Option[Int]
)

/** Database connection config
  * @param host
  *   db host
  * @param port
  *   db port
  * @param database
  *   db name
  * @param user
  *   db user
  * @param password
  *   db password
  */
case class DbConfig(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String
)

/** Root application config
  * @param bot
  *   telegram bot config
  * @param ai
  *   AI service config
  * @param db
  *   database connection config
  */
case class Config(
    bot: BotConfig,
    ai: AIConfig,
    db: DbConfig
) derives ConfigReader

object Config {
  def load(): Config = ConfigSource.default.loadOrThrow[Config]
}
