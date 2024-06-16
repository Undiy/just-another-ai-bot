package undiy.aibot.context.model

import java.time.LocalDateTime

final case class ContextChat(id: Long, title: String)

final case class ContextUser(id: Long, isBot: Boolean, username: String)

final case class ContextMessage(
    messageId: Int,
    content: String,
    createdAt: LocalDateTime,
    chat: ContextChat,
    user: ContextUser
)
