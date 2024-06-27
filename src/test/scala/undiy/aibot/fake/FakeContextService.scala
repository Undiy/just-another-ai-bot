package undiy.aibot.fake

import cats.effect.IO
import cats.effect.kernel.Ref
import undiy.aibot.context.ContextService
import undiy.aibot.context.model.ContextMessage

import scala.collection.immutable.TreeMap

class FakeContextService(
    val state: Ref[IO, List[ContextMessage]]
) extends ContextService[IO] {

  override def saveContextMessage(message: ContextMessage): IO[Unit] = state.update(message :: _)

  override def getContextMessages(chatId: Long, limit: Option[Int]): IO[List[ContextMessage]] = {
    state.get.map { state =>
      val chatMessages = state.filter(_.chat.id == chatId)
      limit match {
        case Some(limit) => chatMessages.take(limit)
        case None        => chatMessages
      }
    }
  }

  override def deleteContextMessages(chatId: Long): IO[Unit] = state.update(_.filterNot(_.chat.id == chatId))
}

object FakeContextService {
  def apply(): IO[FakeContextService] = Ref[IO].of(List.empty[ContextMessage]).map(new FakeContextService(_))
}
