package undiy.aibot

import cats.effect.{Async, IO}
import munit.CatsEffectSuite
import undiy.aibot.bot.AIBotRequestHelper
import undiy.aibot.bot.TelegramModelExt.*
import undiy.aibot.fake.FakeData.*
import undiy.aibot.fake.{FakeAIService, FakeBotApi, FakeContextService}

import scala.collection.immutable.TreeSet

class RequestHelperTests extends CatsEffectSuite {

  val lastMessageId = 1

  test("test requestCompletion") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(chat, lastMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = false)
      }
      _ <- helper.requestCompletion(
        chat = chat,
        prompt = sampleRequest,
        onResponse = response => IO(response).assertEquals(FakeAIService.sampleResponse)
      )
      // no context should be saved
      _ <- contextService.state.get.assert(_.isEmpty)
      // one message should be sent
      _ <- api.lastMessageId.get.assert(_ == lastMessageId + 1)
    } yield {}
  }

  test("test requestCompletion streamed") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(chat, lastMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = true)
      }
      _ <- helper.requestCompletion(
        chat = chat,
        prompt = sampleRequest,
        onResponse = response => IO(response).assert(FakeAIService.sampleResponse.split('\n').map(_.trim).contains(_))
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(lastMessageId + 1)
      // no context should be saved
      _ <- contextService.state.get.assert(_.isEmpty)
    } yield {}
  }

  test("test requestChatCompletion") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(chat, lastMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = false)
      }
      msg = makeMessage(lastMessageId, chat, user, Some(sampleRequest))
      _ <- helper.requestChatCompletion(
        msg = msg,
        onResponse = response => IO(response).assertEquals(FakeAIService.sampleResponse)
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(lastMessageId + 1)
      // one message should be saved in context
      _ <- contextService.state.get.map(_.keySet).assert(TreeSet(lastMessageId).iterator.sameElements(_))
      _ <- contextService.state.get.map(_(lastMessageId)).assertEquals(msg.toContextMessage)
    } yield {}
  }

  test("test requestChatCompletion streamed") {
    for {
      contextService <- FakeContextService()
      aiService = FakeAIService()
      api <- FakeBotApi(chat, lastMessageId)
      helper = new AIBotRequestHelper(using Async[IO], api, aiService, contextService) {
        val config: BotConfig = BotConfig(token = "", streaming = true)
      }
      msg = makeMessage(lastMessageId, chat, user, Some(sampleRequest))
      _ <- helper.requestChatCompletion(
        msg = msg,
        onResponse = response => IO(response).assert(FakeAIService.sampleResponse.split('\n').map(_.trim).contains(_))
      )
      // one message should be sent
      _ <- api.lastMessageId.get.assertEquals(lastMessageId + 1)
      // one message should be saved in context
      _ <- contextService.state.get.map(_.keySet).assert(TreeSet(lastMessageId).iterator.sameElements(_))
      _ <- contextService.state.get.map(_(lastMessageId)).assertEquals(msg.toContextMessage)
    } yield {}
  }
}
