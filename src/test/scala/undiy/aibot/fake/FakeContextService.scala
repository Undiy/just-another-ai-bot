package undiy.aibot.fake

import cats.effect.IO
import cats.effect.kernel.Ref
import undiy.aibot.context.ContextService
import undiy.aibot.context.model.ContextMessage

import scala.collection.immutable.TreeMap

class FakeContextService(
    val state: Ref[IO, TreeMap[Long, ContextMessage]]
) extends ContextService[IO] {

  override def saveContextMessage(message: ContextMessage): IO[Unit] =
    state.update(_.updated(message.messageId, message))

  override def getContextMessages(chatId: Long, limit: Option[Int]): IO[List[ContextMessage]] = {
    state.get.map { state =>
      val entries = state.values.filter(_.chat.id == chatId)
      (limit match {
        case Some(limit) => entries.take(limit)
        case None        => entries
      }).toList
    }
  }

  override def deleteContextMessages(chatId: Long): IO[Unit] = state.update(_.filterNot((id, _) => id == chatId))
}

object FakeContextService {
  def apply(): IO[FakeContextService] = for {
    state <- Ref[IO].of(TreeMap.empty[Long, ContextMessage](using Ordering[Long].reverse))
  } yield new FakeContextService(state)
}
