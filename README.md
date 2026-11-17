# BotForge

A lightweight, zero-bloat Telegram Bot API framework for Java 25 using virtual threads and the JDK HTTP client.

---

## Installation

**Gradle (Kotlin DSL)**
```kotlin
implementation("io.github.stanikoc:botforge:1.0.0")
```

**Gradle (Groovy)**
```groovy
implementation 'io.github.stanikoc:botforge:1.0.0'
```

**Maven**
```xml
<dependency>
    <groupId>io.github.stanikoc</groupId>
    <artifactId>botforge</artifactId>
    <version>1.0.0</version>
</dependency>
```

*Requires **Java 25+**. If your project uses JPMS (`module-info.java`), add `requires io.github.stanikoc.botforge;`.*

---

## Quick Start

Create a bot token with [@BotFather](https://t.me/BotFather), configure handlers with `Bot.builder(token)`, and call `bot.run()`:

```java
import io.github.stanikoc.botforge.Bot;

public class Main {
    static void main(String[] args) {
        Bot bot = Bot.builder(System.getenv("BOT_TOKEN"))
                .command("start", "Say hello", ctx ->
                        ctx.reply("Hi " + ctx.user().firstName() + "!"))
                .command("echo", "Repeat text", ctx ->
                        ctx.reply(ctx.argument().isEmpty() ? "Usage: /echo <text>" : ctx.argument()))
                .fallbackText(ctx -> ctx.reply("You wrote: " + ctx.text()))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::stop));
        bot.run();
    }
}
```

See [ExampleBot.java](botforge-example/src/main/java/io/github/stanikoc/botforge/example/ExampleBot.java) for a full runnable example (`BOT_TOKEN=<token> ./gradlew :botforge-example:run`).

---

## Basic Usage

### Commands
Register `/name` commands with `.command(name, description, handler)`:
- `name` must be 1–32 characters of `a-z`, `0-9`, and `_` (without `/`).
- By default, registered commands are published to Telegram's command menu via `setMyCommands` on startup (disable with `.publishCommands(false)`).
- Inside the handler, `CommandContext` provides `ctx.argument()` (raw trimmed string) and `ctx.args()` (whitespace-split `List<String>`).

```java
.command("greet", "Greet someone", ctx -> {
    String target = ctx.args().isEmpty() ? ctx.user().firstName() : ctx.args().getFirst();
    ctx.reply("Hello, " + target + "!");
})
```

### Inline Keyboards & Buttons
Build inline keyboards with `InlineKeyboard.of(...)` (single row) or `InlineKeyboard.builder().row(...).build()` (multiple rows), and handle button presses by `namespace`:

```java
var keyboard = InlineKeyboard.builder()
        .row(
                InlineButton.callback("👍", "vote", "up"),
                InlineButton.callback("👎", "vote", "down"))
        .row(InlineButton.url("Docs", "https://github.com/stanikoc/BotForge"))
        .build();

Bot bot = Bot.builder(token)
        .command("vote", "Open poll", ctx -> ctx.reply("Choose an option:", keyboard))
        .button("vote", ctx -> {
            ctx.answer("Voted: " + ctx.payload()); // or ctx.alert("...")
            ctx.edit("Thanks for voting " + ctx.payload() + "!");
        })
        .build();
```

- Callback buttons carry `namespace:payload` (max 64 UTF-8 bytes total).
- If your handler doesn't call `ctx.answer()`, `ctx.answer(text)`, or `ctx.alert(text)`, BotForge automatically answers the callback query silently after the handler returns.

### Text Routing
Add plain-text routes with `.text(predicate, handler)` and an optional `.fallbackText(handler)`. Conditions are checked in registration order and only the first match runs:

```java
.text(text -> text.equalsIgnoreCase("ping"), ctx -> ctx.reply("pong"))
.fallbackText(ctx -> ctx.reply("Try /start"))
```

### Replying, Sending & Errors
- Every context (`CommandContext`, `ButtonContext`, `TextContext`) gives access to `ctx.chatId()`, `ctx.user()`, `ctx.reply(...)`, and `ctx.send(chatId, ...)`.
- Telegram API failures throw `TelegramException` (with `errorCode()`). Exceptions thrown inside handlers are logged without stopping the polling loop.
- `bot.run()` blocks the calling thread and runs each update on its own virtual thread; call `bot.stop()` from any thread (such as a shutdown hook) to stop gracefully.
