package undiy.aibot.bot

import telegramium.bots.*
import undiy.aibot.context.model.{ContextChat, ContextMessage, ContextUser}

import java.time.{Instant, OffsetDateTime, ZoneOffset}

/** Extensions for telegram API models
  */
object TelegramModelExt {
  extension (msg: Message) {
    def toContextMessage: ContextMessage = {
//      val rawText = msg.text.getOrElse("")
//      // model doesn't handle telegram mentions (e.g. @username ) well, so let's strip the @ char
//      val processedText = msg.entities.foldLeft(rawText)((text, me) => me match {
//        case MentionMessageEntity(offset, _) => text.updated(offset, ' ')
//        case _ => text
//      })

      ContextMessage(
        messageId = msg.messageId,
        content = msg.text.getOrElse(""),
        createdAt = OffsetDateTime
          .ofInstant(Instant.ofEpochSecond(msg.date), ZoneOffset.UTC),
        chat = msg.chat.toContextChat,
        user = msg.from.get.toContextUser
      )
    }

    def hasCommand: Boolean = msg.entities.exists({
      case command: BotCommandMessageEntity => true
      case _                                => false
    })

    // command and the rest of text as arg
    def getCommand: Option[(String, String)] = msg.entities
      .find({
        case command: BotCommandMessageEntity => true
        case _                                => false
      })
      .map(entity =>
        (
          getEntityContent(entity)
            .drop(1) // remove slash
            .takeWhile(_ != '@'), // remove explicit bot username, if present
          msg.text.get.drop(entity.offset + entity.length)
        )
      )

    private def getEntityContent(messageEntity: MessageEntity): String =
      msg.text.get.slice(
        messageEntity.offset,
        messageEntity.offset + messageEntity.length
      )

    def hasMentionForUser(user: User): Boolean = {
      msg.entities.exists {
        case entity @ MentionMessageEntity(offset, length) =>
          msg.getEntityContent(entity).drop(1) == user.username.getOrElse("")
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
