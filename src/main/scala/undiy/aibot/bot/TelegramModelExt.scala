package undiy.aibot.bot

import telegramium.bots.*
import undiy.aibot.context.model.{ContextChat, ContextMessage, ContextUser}

import java.time.{Instant, OffsetDateTime, ZoneOffset}

object TelegramModelExt {
  extension (msg: Message) {
    def toContextMessage: ContextMessage = ContextMessage(
      messageId = msg.messageId,
      content = msg.text.getOrElse(""),
      createdAt = OffsetDateTime
        .ofInstant(Instant.ofEpochSecond(msg.date), ZoneOffset.UTC),
      chat = msg.chat.toContextChat,
      user = msg.from.get.toContextUser
    )

    def hasCommand: Boolean = msg.entities.exists({
      case command: BotCommandMessageEntity => true
      case _                                => false
    })

    def getEntityContent(messageEntity: MessageEntity): String =
      msg.text.get.slice(
        messageEntity.offset,
        messageEntity.offset + messageEntity.length
      )

    def hasMentionForUser(user: User): Boolean = {
      msg.entities.exists {
        case entity @ MentionMessageEntity(offset, length) =>
          msg.getEntityContent(entity) == s"@${user.username.getOrElse("")}"
        case TextMentionMessageEntity(offset, length, mentioned) =>
          mentioned == user
        case _ => false
      }
    }
  }

  extension (user: User) {
    def toContextUser: ContextUser = user match {
      case User(id, isBot, _, _, username, _, _, _, _, _, _, _) =>
        ContextUser(id, isBot, username)
    }
  }

  extension (chat: Chat) {
    def toContextChat: ContextChat = chat match {
      case Chat(id, _, title, _, _, _, _) => ContextChat(id, title)
    }

    def isPrivate: Boolean = chat.`type` == "private"
  }
}
