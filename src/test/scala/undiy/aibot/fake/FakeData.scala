package undiy.aibot.fake

import telegramium.bots.{Chat, Message, User}

object FakeData {
  val sampleRequest = "What is the answer to life, the universe, and everything?"
  val chat: Chat = Chat(42, "private")
  val user: User = User(id = 42, isBot = false, firstName = "test_first_name", username = Some("test"))

  def makeMessage(id: Int, chat: Chat, user: User = user, text: Option[String] = None): Message = Message(
    messageId = id,
    date = System.currentTimeMillis().toInt / 1000,
    chat = chat,
    from = Some(user),
    text = text
  )
}
