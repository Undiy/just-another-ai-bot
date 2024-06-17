CREATE TABLE context_chats (
  id int8 PRIMARY KEY,
  title varchar
);

CREATE TABLE context_users (
  id int8 PRIMARY KEY,
  is_bot bool,
  username varchar
);

CREATE TABLE context_messages (
  id SERIAL PRIMARY KEY,
  message_id int4 NOT NULL,
  content text NOT NULL,
  created_at timestamptz NOT NULL,
  chat_id int8 REFERENCES context_chats,
  user_id int8 REFERENCES context_users
);

CREATE UNIQUE INDEX context_messages_unique_idx on context_messages (chat_id, message_id);
