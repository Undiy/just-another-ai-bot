package undiy.aibot.fake

import cats.effect.IO
import cats.effect.kernel.Ref
import telegramium.bots.client.Method
import telegramium.bots.high.Api
import telegramium.bots.{Chat, Message, User}

class FakeBotApi(val chat: Ref[IO, Chat], val lastMessageId: Ref[IO, Int]) extends Api[IO] {

  override def execute[Res](method: Method[Res]): IO[Res] = method.payload.name match {
    case "sendMessage" =>
      for {
        chat <- chat.get
        messageId <- lastMessageId.updateAndGet(_ + 1)
      } yield FakeData.makeMessage(42, chat).asInstanceOf[Res]
    case "deleteMessage" => IO.pure(true.asInstanceOf[Res])
    case name            => IO.raiseError(RuntimeException(s"Unhandled method: $name"))
  }

}

object FakeBotApi {
  def apply(chat: Chat, lastMessageId: Int = 0): IO[FakeBotApi] = for {
    chat <- Ref[IO].of(chat)
    lastMessageId <- Ref[IO].of(lastMessageId)
  } yield new FakeBotApi(chat, lastMessageId)
}
