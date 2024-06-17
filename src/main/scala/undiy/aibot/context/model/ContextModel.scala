package undiy.aibot.context.model

import java.time.OffsetDateTime

final case class ContextChat(id: Long, title: Option[String])

final case class ContextUser(id: Long, isBot: Boolean, username: Option[String])

final case class ContextMessage(
    messageId: Int,
    content: String,
    createdAt: OffsetDateTime,
    chat: ContextChat,
    user: ContextUser
)
