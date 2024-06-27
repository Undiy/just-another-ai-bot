package undiy.aibot.bot

import cats.effect.{Async, IO}
import munit.CatsEffectSuite
import undiy.aibot.BotConfig
import undiy.aibot.bot.TelegramModelExt.*
import undiy.aibot.fake.FakeData.*
import undiy.aibot.fake.{FakeAIService, FakeBotApi, FakeContextService}

class RequestHelperTests extends CatsEffectSuite {

  val initMessageId = 1

  test("test requestCompletion") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = false)
      }
      _ <- helper.requestCompletion(
        chat = chat,
        prompt = sampleRequest,
        onResponse = response => IO(response).assertEquals(sampleResponse)
      )
      // no context should be saved
      _ <- contextService.state.get.assert(_.isEmpty)
      // one message should be sent
      _ <- api.lastMessageId.get.assert(_ == initMessageId + 1)
    } yield {}
  }

  test("test requestCompletion streamed") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = true)
      }
      _ <- helper.requestCompletion(
        chat = chat,
        prompt = sampleRequest,
        onResponse = response => IO(response).assert(sampleResponseParts.contains(_))
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1)
      // no context should be saved
      _ <- contextService.state.get.assert(_.isEmpty)
    } yield {}
  }

  test("test requestChatCompletion") {
    val msg = makeMessage(initMessageId, chat, user, Some(sampleRequest))

    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = false)
      }
      _ <- helper.requestChatCompletion(
        msg = msg,
        onResponse = response => IO(response).assertEquals(sampleResponse)
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1)
      // one message should be saved in context
      _ <- contextService.state.get.assertEquals(List(msg.toContextMessage))
    } yield {}
  }

  test("test requestChatCompletion streamed") {
    val msg = makeMessage(initMessageId, chat, user, Some(sampleRequest))

    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(initMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = true)
      }
      _ <- helper.requestChatCompletion(
        msg = msg,
        onResponse = response => IO(response).assert(sampleResponseParts.contains(_))
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(initMessageId + 1)
      // one message should be saved in context
      _ <- contextService.state.get.assertEquals(List(msg.toContextMessage))
    } yield {}
  }
}
