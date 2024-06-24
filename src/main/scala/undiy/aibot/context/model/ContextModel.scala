package undiy.aibot.context.model

import java.time.OffsetDateTime

/** A chat that could contain context messages
  * @param id
  *   [external] chat id
  * @param title
  *   chat title (optional)
  */
final case class ContextChat(id: Long, title: Option[String])

/** An author of context message
  * @param id
  *   [external] user id
  * @param isBot
  *   true, if an author is bot
  * @param username
  *   author's username (optional)
  */
final case class ContextUser(id: Long, isBot: Boolean, username: Option[String])

/** A single message
  * @param messageId
  *   [external] message id
  * @param content
  *   textual content
  * @param createdAt
  *   date & time of message creation
  * @param chat
  *   chat, for which the message belongs
  * @param user
  *   message author
  */
final case class ContextMessage(
    messageId: Int,
    content: String,
    createdAt: OffsetDateTime,
    chat: ContextChat,
    user: ContextUser
)
