package undiy.aibot.ai

import fs2.Stream
import undiy.aibot.context.model.ContextMessage

/** Service that makes request to AI service
  * @tparam F
  *   effect type
  */
trait AIService[F[_]] {

  /** Make completion based on prompt with no additional context
    * @param prompt
    *   a textual request to AI assistant
    * @return
    *   async result with text response
    */
  def makeCompletion(prompt: String): F[String]

  /** Make completion for a context of chat messages
    * @param messages
    *   list of context messages
    * @return
    *   async result with text response
    */
  def makeChatCompletion(messages: List[ContextMessage]): F[String]

  /** Make completion based on prompt with no additional context, streamed
    * version
    * @param prompt
    *   a textual request to AI assistant
    * @return
    *   a streamed response
    */
  def makeCompletionStreamed(prompt: String): Stream[F, String]

  /** Make completion for a context of chat messages, streamed version
    * @param messages
    *   list of context messages
    * @return
    *   a streamed response
    */
  def makeChatCompletionStreamed(
      messages: List[ContextMessage]
  ): Stream[F, String]
}
