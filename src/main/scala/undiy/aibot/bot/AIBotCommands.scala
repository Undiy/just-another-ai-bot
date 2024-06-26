package undiy.aibot.bot

import cats.effect.Async
import cats.syntax.all.*
import telegramium.bots.*
import telegramium.bots.high.Api
import telegramium.bots.high.Methods.{sendMessage, setMyCommands}
import telegramium.bots.high.implicits.*
import telegramium.bots.high.keyboards.{InlineKeyboardButtons, InlineKeyboardMarkups}
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
          action = (msg, _) =>
            AIBotButton.sendButtons(
              msg = msg,
              text = "Are you sure you want to reset context? WARNING: this cannot be undone",
              buttons = List(
                AIBotButton.ResetContext,
                AIBotButton.No
              )
            )
        )

    private def toBotCommand: BotCommand = BotCommand(command, description)
  }

  object AIBotCommand {
    def fromString(command: String): Option[AIBotCommand] = AIBotCommand.values.find(_.command == command)

    def setCommands(): F[Boolean] = setMyCommands(
      commands = AIBotCommand.values.map(_.toBotCommand).toList
    ).exec
  }

  enum AIBotButton(val title: String, val data: String, val action: Message => F[Option[String]]) {
    case No extends AIBotButton(title = "No", data = "no", action = _ => Async[F].pure(None))
    case ResetContext
        extends AIBotButton(
          title = "OK",
          data = "resetcontext",
          action = msg => contextService.deleteContextMessages(msg.chat.id).as(Some("Context was reset"))
        )
  }

  object AIBotButton {
    def fromQuery(query: CallbackQuery): Option[AIBotButton] = for {
      data <- query.data
      button <- values.find(_.data == data)
    } yield button

    def sendButtons(msg: Message, text: String, buttons: List[AIBotButton]): F[Unit] = sendMessage(
      chatId = ChatIntId(msg.chat.id),
      text = text,
      replyMarkup = Some(
        InlineKeyboardMarkups.singleRow(
          buttons.map(callback => InlineKeyboardButtons.callbackData(callback.title, callback.data))
        )
      )
    ).exec.void
  }
}
