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
    val request = makeMessage(
      id = initMessageId,
      chat = chat,
      user = user,
      text = Some(sampleRequest)
    )
    val response = makeMessage(
      id = initMessageId + 2,
      chat = chat,
      user = botUser,
      text = Some(sampleResponse)
    )

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
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
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
    val request = makeMessage(
      id = initMessageId,
      chat = chat,
      user = user,
      text = Some(sampleRequest)
    )
    val responses = sampleResponseParts.zipWithIndex
      .map { (responsePart, i) =>
        makeMessage(
          id = initMessageId + 2 + i,
          chat = chat,
          user = botUser,
          text = Some(responsePart)
        )
      }

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
      _ <- bot.onMessage(request)
      // one message + one for each response part should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1 + sampleResponseParts.size)
      // messages should be saved in context (request & response parts)
      _ <- contextService.state.get.assertEquals(
        (request :: responses).reverse
          .map(_.toContextMessage)
      )
    } yield {}
  }

  test("test bot for plain group chat message") {
    val msg = makeMessage(initMessageId, groupChat, user, Some(sampleRequest))

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
      _ <- bot.onMessage(msg)
      // no messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId)
      // message should be saved in context
      _ <- contextService.state.get.assertEquals(List(msg.toContextMessage))
    } yield {}
  }

  test("test bot for group chat message with bot mention") {
    val request = makeMessageWithMention(
      id = initMessageId,
      chat = groupChat,
      user = user,
      mentionUser = botUser,
      text = sampleRequest
    )
    val response = makeMessageWithMention(
      id = initMessageId + 2,
      chat = groupChat,
      user = botUser,
      mentionUser = user,
      text = sampleResponse
    )

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
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
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
    val request = makeMessageWithMention(
      id = initMessageId,
      chat = groupChat,
      user = user,
      mentionUser = botUser,
      text = sampleRequest
    )
    val responses = sampleResponseParts.zipWithIndex
      .map { (responsePart, i) =>
        makeMessageWithMention(
          id = initMessageId + 2 + i,
          chat = groupChat,
          user = botUser,
          mentionUser = user,
          text = responsePart
        )
      }

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
      _ <- bot.onMessage(request)
      // one message + one for each response part should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1 + sampleResponseParts.size)
      // messages should be saved in context (request & response parts)
      _ <- contextService.state.get.assertEquals(
        (request :: responses).reverse
          .map(_.toContextMessage)
      )
    } yield {}
  }

  test("test bot private chat prompt command") {
    val request = makeMessageWithCommand(
      id = initMessageId,
      chat = chat,
      user = user,
      command = "/prompt",
      args = sampleRequest
    )

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
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
      // no messages should be saved in context
      _ <- contextService.state.get.assert(_.isEmpty)
    } yield {}
  }

  test("test bot group chat prompt command") {
    val request = makeMessageWithCommand(
      id = initMessageId,
      chat = groupChat,
      user = user,
      command = "/prompt",
      args = sampleRequest
    )

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
      _ <- bot.onMessage(request)
      // two messages should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 2)
      // no messages should be saved in context
      _ <- contextService.state.get.assert(_.isEmpty)
    } yield {}
  }
}
