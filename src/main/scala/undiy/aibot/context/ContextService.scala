package undiy.aibot.context

import undiy.aibot.context.model.ContextMessage

trait ContextService[F[_]] {
  def saveContextMessage(message: ContextMessage): F[Unit]

  def getContextMessages(chatId: Int, limit: Int = 100): F[List[ContextMessage]]
}

