package undiy.aibot.context

import undiy.aibot.context.model.ContextMessage

/** A service to store and load chat messages context
  * @tparam F
  *   effect type
  */
trait ContextService[F[_]] {
  def saveContextMessage(message: ContextMessage): F[Unit]

  def getContextMessages(chatId: Long, limit: Option[Int]): F[List[ContextMessage]]

  def deleteContextMessages(chatId: Long): F[Unit]
}
