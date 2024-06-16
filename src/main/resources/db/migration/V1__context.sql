CREATE TABLE context_chat (
  id int8 PRIMARY KEY,
  title varchar NOT NULL
);

CREATE TABLE context_user (
  id int8 PRIMARY KEY,
  is_bot bool,
  username varchar NOT NULL
);

CREATE TABLE context_message (
  id SERIAL PRIMARY KEY,
  date timestamp NOT NULL,
  chat_id int8 REFERENCES context_chat,
  user_id int8 REFERENCES context_user
);
