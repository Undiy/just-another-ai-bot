# Just Another AI Bot
Telegram bot that forwards requests to AI API and keeps track of chat messages context

## Features

In private AI bot maintains dialog with user, answering every message; in group chats it responds to a message with bot's mention, but all the messages are saved for context.
Bot has two commands: `/prompt` for a simple textual prompt with no additional context and `/resetcontext` that clears the context of accumulated messages (couldn't be reverted) 

Bot keeps track of edited messages and updates saved context accordingly. It doesn't handle deleted messages, since we have no updates for that in Telegram bot API for now.

## Used libraries
* [OpenAI Scala Client](https://github.com/cequence-io/openai-scala-client) - openai-compatible API client
* [Telegramium](https://github.com/apimorphism/telegramium) - pure functional Telegram Bot API for Scala
* [Skunk](https://typelevel.org/skunk/) - PostgreSQL API
* [Dumbo](https://github.com/rolang/dumbo/) - DB migration tool
* [PureConfig](https://github.com/pureconfig/pureconfig) - for loading configuration files
* [Log4s](https://github.com/Log4s/log4s) - SLF4J wrapper for Scala
* [SBT Native Packager](https://github.com/sbt/sbt-native-packager) - to build a Docker image

## Usage

You'll need:
* A key to openai or openai-compatible chat completion API (like [this](https://github.com/PawanOsman/ChatGPT) one), or run such API locally
* a telegram bot token (obtainable from @BotFather (https://t.me/BotFather))
* group privacy of telegram bot should be disabled, so that it could read and respond to group messages 

Easiest way to run this bot app with docker, you'll need to:
1. make `.env` configuration file
2. build bot image with `sbt Docker / publishLocal`
3. run bot and db images with `docker-compose up`

# TODO
* `/digest` command to get a digest of the group chat messages
* tests
