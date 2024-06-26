package undiy.aibot.bot

import cats.effect.Async
import cats.syntax.all.*
import telegramium.bots.high.Api
import telegramium.bots.high.Methods.{sendMessage, setMyCommands}
import telegramium.bots.high.implicits.*
import telegramium.bots.{BotCommand, ChatIntId, Message}
import undiy.aibot.context.ContextService

trait AIBotCommands[F[_]: Async: Api](using contextService: ContextService[F]) extends AIBotRequestHelper[F] {

  enum AIBotCommand(
      val command: String,
      val description: String,
      val action: (Message, String) => F[Unit]
  ) {
    case Prompt
        extends AIBotCommand(
          command = "prompt",
          description = "Make a simple prompt with no additional context (message history)",
          action = (msg, prompt) => {
            if (prompt.trim.nonEmpty) {
              requestCompletion(
                chat = msg.chat,
                prompt = prompt,
                onResponse = response =>
                  sendMessage(
                    chatId = ChatIntId(msg.chat.id),
                    text = response
                  ).exec.void
              )
            } else {
              sendMessage(
                chatId = ChatIntId(msg.chat.id),
                text = "Please write the actual prompt after the /prompt command"
              ).exec.void
            }
          }
        )

    case ResetContext
        extends AIBotCommand(
          command = "resetcontext",
          description = "Deletes all the bot's message context for this chat. WARNING: this cannot be undone",
          // TODO add confirmation
          action = (msg, _) => contextService.deleteContextMessages(msg.chat.id)
        )

    private def toBotCommand: BotCommand = BotCommand(command, description)
  }

  object AIBotCommand {
    def fromString(command: String): Option[AIBotCommand] = AIBotCommand.values.find(_.command == command)

    def setCommands(): F[Boolean] = setMyCommands(
      commands = AIBotCommand.values.map(_.toBotCommand).toList
    ).exec
  }
}
