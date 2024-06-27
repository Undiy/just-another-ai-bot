package undiy.aibot.fake

import cats.effect.IO
import cats.effect.kernel.Ref
import io.circe.Json
import io.circe.syntax.*
import telegramium.bots
import telegramium.bots.client.*
import telegramium.bots.client.CirceImplicits.given
import telegramium.bots.high.Api
import telegramium.bots.{Chat, ChatIntId, ChatStrId}
import undiy.aibot.fake.FakeData.*

import scala.collection.immutable.Map

class FakeBotApi(val lastMessageId: Ref[IO, Int]) extends Api[IO] {

  override def execute[Res](method: Method[Res]): IO[Res] = method.payload.name match {
    case "sendMessage" =>
      lastMessageId.updateAndGet(_ + 1).map { messageId =>
        val sendMessageReq: SendMessageReq = method.payload.json.as[SendMessageReq].toTry.get
        makeMessage(
          id = messageId,
          chat = sendMessageReq.chatId match {
            case ChatIntId(chat.id)      => chat
            case ChatIntId(groupChat.id) => groupChat
          },
          user = botUser,
          text = Some(sendMessageReq.text),
          entities = sendMessageReq.entities
        ).asInstanceOf[Res]
      }
    case "deleteMessage" => IO.pure(true.asInstanceOf[Res])
    case "getMe"         => IO.pure(botUser.asInstanceOf[Res])
    case name            => IO.raiseError(RuntimeException(s"Unhandled method: $name"))
  }

}

object FakeBotApi {
  def apply(initialMessageId: Int = 1): IO[FakeBotApi] = Ref[IO].of(initialMessageId).map(new FakeBotApi(_))
}
