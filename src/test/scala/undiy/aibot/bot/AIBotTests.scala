package undiy.aibot.bot

import cats.Parallel
import cats.effect.{Async, IO}
import munit.CatsEffectSuite
import telegramium.bots.Message
import undiy.aibot.BotConfig
import undiy.aibot.bot.TelegramModelExt.*
import undiy.aibot.fake.FakeData.*
import undiy.aibot.fake.{FakeAIService, FakeBotApi, FakeContextService}

class AIBotTests extends CatsEffectSuite {
  val initMessageId = 1

  test("test bot private chat message") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      bot = new AIBot(BotConfig(token = "", streaming = false))(using
        Async[IO],
        Parallel[IO],
        api,
        aiService,
        contextService
      )
      request = makeMessage(
        id = initMessageId,
        chat = chat,
        user = user,
        text = Some(sampleRequest)
      )
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
      response = makeMessage(
        id = initMessageId + 2,
        chat = chat,
        user = botUser,
        text = Some(sampleResponse)
      )
      // two message should be saved in context (request & response)
      _ <- contextService.state.get.assertEquals(
        List(
          response,
          request
        ).map(_.toContextMessage)
      )
    } yield {}
  }

  test("test bot private chat message (streamed)") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      bot = new AIBot(BotConfig(token = "", streaming = true))(using
        Async[IO],
        Parallel[IO],
        api,
        aiService,
        contextService
      )
      request = makeMessage(
        id = initMessageId,
        chat = chat,
        user = user,
        text = Some(sampleRequest)
      )
      _ <- bot.onMessage(request)
      // one message + one for each response part should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1 + sampleResponseParts.size)
      responses = sampleResponseParts.zipWithIndex
        .map { (responsePart, i) =>
          makeMessage(
            id = initMessageId + 2 + i,
            chat = chat,
            user = botUser,
            text = Some(responsePart)
          )
        }
      // messages should be saved in context (request & response parts)
      _ <- contextService.state.get.assertEquals(
        (request :: responses).reverse
          .map(_.toContextMessage)
      )
    } yield {}
  }

  test("test bot for plain group chat message") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      bot = new AIBot(BotConfig(token = "", streaming = false))(using
        Async[IO],
        Parallel[IO],
        api,
        aiService,
        contextService
      )
      msg = makeMessage(initMessageId, groupChat, user, Some(sampleRequest))
      _ <- bot.onMessage(msg)
      // no messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId)
      // message should be saved in context
      _ <- contextService.state.get.assertEquals(List(msg.toContextMessage))
    } yield {}
  }

  test("test bot for group chat message with bot mention") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      bot = new AIBot(BotConfig(token = "", streaming = false))(using
        Async[IO],
        Parallel[IO],
        api,
        aiService,
        contextService
      )
      request = makeMessageWithMention(
        id = initMessageId,
        chat = groupChat,
        user = user,
        mentionUser = botUser,
        text = sampleRequest
      )
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
      response = makeMessageWithMention(
        id = initMessageId + 2,
        chat = groupChat,
        user = botUser,
        mentionUser = user,
        text = sampleResponse
      )
      // two message should be saved in context (request & response)
      _ <- contextService.state.get.assertEquals(
        List(
          response,
          request
        ).map(_.toContextMessage)
      )
    } yield {}
  }

  test("test bot for group chat message with bot mention (streamed)") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      bot = new AIBot(BotConfig(token = "", streaming = true))(using
        Async[IO],
        Parallel[IO],
        api,
        aiService,
        contextService
      )
      request = makeMessageWithMention(
        id = initMessageId,
        chat = groupChat,
        user = user,
        mentionUser = botUser,
        text = sampleRequest
      )
      _ <- bot.onMessage(request)
      // one message + one for each response part should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1 + sampleResponseParts.size)
      responses = sampleResponseParts.zipWithIndex
        .map { (responsePart, i) =>
          makeMessageWithMention(
            id = initMessageId + 2 + i,
            chat = groupChat,
            user = botUser,
            mentionUser = user,
            text = responsePart
          )
        }
      // messages should be saved in context (request & response parts)
      _ <- contextService.state.get.assertEquals(
        (request :: responses).reverse
          .map(_.toContextMessage)
      )
    } yield {}
  }
}
