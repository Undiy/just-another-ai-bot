package undiy.aibot.context

import cats.effect.Async
import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import skunk.data.Completion
import skunk.implicits.sql
import undiy.aibot.context.model.{ContextChat, ContextMessage, ContextUser}

/** Context service implementation backed by PostgreSQL db
  * @param async$F$0
  *   async typeclass
  * @param session
  *   skunk session
  * @tparam F
  *   effect type
  */
final class DbContextService[F[_]: Async](using session: Session[F])
    extends ContextService[F] {

  private val messageDecoder: Decoder[ContextMessage] =
    (int4 *: text *: timestamptz *: int8 *: varchar.opt *: int8 *: bool *: varchar.opt)
      .map {
        case (
              messageId,
              content,
              createdAt,
              chatId,
              chatTitle,
              userId,
              isBot,
              username
            ) =>
          ContextMessage(
            messageId = messageId,
            content = content,
            createdAt = createdAt,
            chat = ContextChat(chatId, chatTitle),
            user = ContextUser(userId, isBot, username)
          )
      }

  private val queryMessages: Query[Long, ContextMessage] =
    sql"""
        SELECT context_messages.message_id, context_messages.content, context_messages.created_at, context_chats.id, context_chats.title, context_users.id, context_users.is_bot, context_users.username
        FROM context_messages
        INNER JOIN context_chats on context_messages.chat_id = context_chats.id
        INNER JOIN context_users on context_messages.user_id = context_users.id
        WHERE context_chats.id = $int8
        ORDER BY context_messages.created_at DESC
        """.query(messageDecoder)

  private val queryMessagesWithLimit: Query[(Long, Int), ContextMessage] =
    sql"""
      SELECT context_messages.message_id, context_messages.content, context_messages.created_at, context_chats.id, context_chats.title, context_users.id, context_users.is_bot, context_users.username
      FROM context_messages
      INNER JOIN context_chats on context_messages.chat_id = context_chats.id
      INNER JOIN context_users on context_messages.user_id = context_users.id
      WHERE context_chats.id = $int8
      ORDER BY context_messages.created_at DESC LIMIT $int4
      """.query(messageDecoder)

  private val insertChat: Command[ContextChat] =
    sql"""
      INSERT INTO context_chats (id, title)
      VALUES ($int8, ${varchar.opt})
      ON CONFLICT(id) DO NOTHING
    """.command
      .to[ContextChat]

  private val insertUser: Command[ContextUser] =
    sql"""
      INSERT INTO context_users (id, is_bot, username)
      VALUES ($int8, $bool, ${varchar.opt})
      ON CONFLICT(id) DO NOTHING
    """.command
      .to[ContextUser]

  private val insertMessage: Command[ContextMessage] =
    sql"""
      INSERT INTO context_messages (message_id, content, created_at, chat_id, user_id)
      VALUES ($int4, $text, $timestamptz, $int8, $int8)
      ON CONFLICT (chat_id, message_id) DO UPDATE
      SET content = EXCLUDED.content
    """.command
      .contramap {
        case ContextMessage(messageId, content, createdAt, chat, user) =>
          messageId *: content *: createdAt *: chat.id *: user.id *: EmptyTuple
      }

  private val deleteMessages: Command[Long] =
    sql"""
      DELETE FROM context_messages
      WHERE chat_id = $int8
    """.command

  override def saveContextMessage(message: ContextMessage): F[Unit] = {
    for {
      _ <- session.execute(insertChat)(message.chat)
      _ <- session.execute(insertUser)(message.user)
      _ <- session.execute(insertMessage)(message)
    } yield {}
  }

  override def getContextMessages(
      chatId: Long,
      limit: Option[Int]
  ): F[List[ContextMessage]] = limit match {
    case Some(limit) => session.execute(queryMessagesWithLimit)(chatId, limit)
    case None        => session.execute(queryMessages)(chatId)
  }

  override def deleteContextMessages(chatId: Long): F[Unit] =
    session.execute(deleteMessages)(chatId).void
}
