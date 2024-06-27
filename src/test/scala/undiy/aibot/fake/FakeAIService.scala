package undiy.aibot.fake

import cats.effect.IO
import fs2.Stream
import undiy.aibot.ai.AIService
import undiy.aibot.context.model.ContextMessage
import undiy.aibot.fake.FakeAIService.*

class FakeAIService extends AIService[IO] {
  override def makeCompletion(prompt: String): IO[String] = IO(sampleResponse)

  override def makeChatCompletion(messages: List[ContextMessage]): IO[String] = IO(sampleResponse)

  override def makeCompletionStreamed(prompt: String): Stream[IO, String] = sampleStreamResponse

  override def makeChatCompletionStreamed(messages: List[ContextMessage]): Stream[IO, String] = sampleStreamResponse
}

object FakeAIService {
  val sampleResponse: String = """
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Duis ultricies lacus sed turpis tincidunt id aliquet. Euismod quis viverra nibh cras pulvinar mattis nunc. Sollicitudin aliquam ultrices sagittis orci. Laoreet id donec ultrices tincidunt arcu non sodales neque. Dolor sit amet consectetur adipiscing elit pellentesque habitant morbi tristique. Commodo sed egestas egestas fringilla phasellus. Cursus risus at ultrices mi. Massa vitae tortor condimentum lacinia quis. Integer feugiat scelerisque varius morbi. Dui accumsan sit amet nulla facilisi morbi tempus iaculis. Magnis dis parturient montes nascetur ridiculus mus. Accumsan in nisl nisi scelerisque eu. Volutpat ac tincidunt vitae semper quis lectus nulla. Tristique senectus et netus et malesuada fames ac. Enim tortor at auctor urna nunc id cursus metus. Pellentesque elit ullamcorper dignissim cras tincidunt lobortis feugiat vivamus. Malesuada fames ac turpis egestas maecenas pharetra.
      |
      |Ullamcorper sit amet risus nullam eget felis eget nunc. Nec nam aliquam sem et tortor consequat id. Ligula ullamcorper malesuada proin libero nunc consequat. Sed adipiscing diam donec adipiscing tristique risus nec feugiat in. Id consectetur purus ut faucibus pulvinar elementum integer. Lorem ipsum dolor sit amet consectetur adipiscing elit duis tristique. Ipsum dolor sit amet consectetur adipiscing elit duis tristique sollicitudin. Ut lectus arcu bibendum at varius vel. Id diam vel quam elementum pulvinar etiam non quam. Aliquam malesuada bibendum arcu vitae elementum curabitur vitae nunc. Porta lorem mollis aliquam ut porttitor leo. Non sodales neque sodales ut etiam sit amet nisl purus. Donec ac odio tempor orci dapibus ultrices in iaculis nunc. Sed augue lacus viverra vitae congue eu consequat. A scelerisque purus semper eget duis at tellus at. Et malesuada fames ac turpis egestas sed tempus urna et. Quis vel eros donec ac odio. Amet porttitor eget dolor morbi non arcu risus. Varius vel pharetra vel turpis nunc. Nunc sed augue lacus viverra vitae congue eu consequat.
      |
      |Nullam non nisi est sit. Nulla aliquet porttitor lacus luctus accumsan tortor posuere ac. Lectus vestibulum mattis ullamcorper velit sed. Sodales neque sodales ut etiam sit amet nisl purus in. Quis varius quam quisque id diam vel quam elementum pulvinar. Viverra nibh cras pulvinar mattis nunc sed blandit libero volutpat. Neque volutpat ac tincidunt vitae semper quis. Turpis massa tincidunt dui ut ornare lectus sit amet. Vel quam elementum pulvinar etiam non quam lacus. Luctus accumsan tortor posuere ac ut. Nullam eget felis eget nunc lobortis mattis. Egestas pretium aenean pharetra magna ac placerat vestibulum lectus mauris. Non diam phasellus vestibulum lorem sed risus ultricies.
      |""".stripMargin

  def sampleStreamResponse: Stream[IO, String] =
    Stream.fromIterator[IO].apply[String](sampleResponse.iterator.map(c => c.toString), 3)
}
