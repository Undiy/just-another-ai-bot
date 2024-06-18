package undiy.aibot.context

import undiy.aibot.context.model.{ContextChat, ContextMessage}

trait ContextService[F[_]] {
  def saveContextMessage(message: ContextMessage): F[Unit]

  def getContextMessages(chatId: Long, limit: Int = 100): F[List[ContextMessage]]

  def deleteContextMessages(chatId: Long): F[Unit]
}

