package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.registry.RegistryImpl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotImplTest {
    @FunctionalInterface
    private interface ApiBehavior {
        @Nullable Object handle(String method, Map<String, ?> params);
    }

    private static final class FakeApi implements TelegramApi {
        final List<String> methods = new CopyOnWriteArrayList<>();
        final List<Map<String, ?>> params = new CopyOnWriteArrayList<>();
        volatile ApiBehavior behavior = (method, p) -> Map.of();

        @Override
        public @Nullable Object call(@NonNull String method, @NonNull Map<String, ?> params, @NonNull Duration timeout) {
            methods.add(method);
            this.params.add(params);
            return behavior.handle(method, params);
        }
    }

    private final FakeApi api = new FakeApi();

    private BotImpl bot(
            List<CommandRoute> commands,
            List<ButtonRoute> buttons,
            List<TextRoute> texts,
            boolean publishCommands) {
        var commandRegistry = new RegistryImpl<>(commands.toArray(CommandRoute[]::new));
        var buttonRegistry = new RegistryImpl<>(buttons.toArray(ButtonRoute[]::new));
        var dispatcher = new Dispatcher(
                api, commandRegistry, buttonRegistry, texts.toArray(TextRoute[]::new), null);
        return new BotImpl(api, dispatcher, commandRegistry, publishCommands);
    }

    @Test
    void publishesCommandsDispatchesUpdatesAndConfirmsOffsetOnStop() {
        List<String> handled = new CopyOnWriteArrayList<>();
        var bot = bot(
                List.of(new CommandRoute("start", "Say hi", ctx -> handled.add("cmd:" + ctx.argument()))),
                List.of(),
                List.of(new TextRoute(t -> true, ctx -> handled.add("text:" + ctx.text()))),
                true);

        var polls = new int[1];
        api.behavior = (method, params) -> switch (method) {
            case "getMe" -> Map.of("username", "ForgeBot");
            case "setMyCommands" -> Boolean.TRUE;
            case "getUpdates" -> {
                if (!Duration.ZERO.equals(Duration.ofSeconds(((Number) params.get("timeout")).longValue()))) {
                    if (polls[0]++ == 0) {
                        yield List.of(
                                Map.of(
                                        "update_id", 10L,
                                        "message", Map.of(
                                                "message_id", 1L,
                                                "chat", Map.of("id", 5L),
                                                "from", Map.of("id", 7L, "first_name", "Alice"),
                                                "text", "/start now",
                                                "entities", List.of(Map.of("type", "bot_command", "offset", 0L, "length", 6L)))),
                                // malformed update missing message_id should be skipped without stopping
                                Map.of(
                                        "update_id", 11L,
                                        "message", Map.of(
                                                "chat", Map.of("id", 5L),
                                                "from", Map.of("id", 7L, "first_name", "Alice"),
                                                "text", "broken")),
                                Map.of(
                                        "update_id", 12L,
                                        "message", Map.of(
                                                "message_id", 2L,
                                                "chat", Map.of("id", 5L),
                                                "from", Map.of("id", 7L, "first_name", "Alice"),
                                                "text", "hello")));
                    }
                    bot.stop();
                }
                yield List.of();
            }
            default -> Map.of();
        };

        bot.run();

        assertEquals(List.of("cmd:now", "text:hello"), handled.stream().sorted().toList());
        assertEquals(
                List.of("getMe", "setMyCommands", "getUpdates", "getUpdates", "getUpdates"),
                api.methods);
        assertEquals(
                Map.of("commands", List.of(Map.of("command", "start", "description", "Say hi"))),
                api.params.get(1));
        assertEquals(0L, api.params.get(2).get("offset"));
        assertEquals(13L, api.params.get(3).get("offset"));
        assertEquals(Map.of("offset", 13L, "timeout", 0, "limit", 1), api.params.get(4));
    }

    @Test
    void skipsSetMyCommandsWhenDisabledOrEmptyAndDoesNotConfirmZeroOffset() {
        var botDisabled = bot(
                List.of(new CommandRoute("start", "Start", ctx -> {})),
                List.of(),
                List.of(),
                false);
        api.behavior = (method, params) -> {
            if ("getMe".equals(method)) {
                return Map.of("username", "ForgeBot");
            }
            botDisabled.stop();
            return null;
        };

        botDisabled.run();
        assertEquals(List.of("getMe", "getUpdates"), api.methods);

        api.methods.clear();
        var botEmpty = bot(List.of(), List.of(), List.of(), true);
        api.behavior = (method, params) -> {
            if ("getMe".equals(method)) {
                return Map.of("username", "ForgeBot");
            }
            botEmpty.stop();
            return List.of();
        };

        botEmpty.run();
        assertEquals(List.of("getMe", "getUpdates"), api.methods);
    }

    @Test
    void continuesWhenPublishCommandsOrHandlerFails() {
        List<String> seen = new CopyOnWriteArrayList<>();
        var bot = bot(
                List.of(
                        new CommandRoute("fail", "Fails", ctx -> {
                            seen.add("fail");
                            throw new IllegalStateException("handler error");
                        }),
                        new CommandRoute("ok", "Works", ctx -> seen.add("ok"))),
                List.of(),
                List.of(),
                true);

        var polls = new int[1];
        api.behavior = (method, params) -> switch (method) {
            case "getMe" -> Map.of("username", "ForgeBot");
            case "setMyCommands" -> throw new TelegramException(500, "cannot publish");
            case "getUpdates" -> {
                if (((Number) params.get("timeout")).intValue() > 0 && polls[0]++ == 0) {
                    yield List.of(
                            commandUpdate(1L, "/fail"),
                            commandUpdate(2L, "/ok"));
                }
                bot.stop();
                yield List.of();
            }
            default -> Map.of();
        };

        bot.run();

        assertTrue(seen.contains("fail"));
        assertTrue(seen.contains("ok"));
    }

    @Test
    void retriesAfterPollingErrorAndStopsCleanly() {
        var bot = bot(List.of(), List.of(), List.of(), false);
        var polls = new int[1];
        api.behavior = (method, params) -> switch (method) {
            case "getMe" -> Map.of("username", "ForgeBot");
            case "getUpdates" -> {
                if (polls[0]++ == 0) {
                    throw new TelegramException(502, "bad gateway");
                }
                bot.stop();
                yield List.of();
            }
            default -> Map.of();
        };

        bot.run();

        assertEquals(List.of("getMe", "getUpdates", "getUpdates"), api.methods);
    }

    @Test
    void stopsImmediatelyWhenPollingFailsAfterStopOrInterruptedSleep() {
        var bot = bot(List.of(), List.of(), List.of(), false);
        api.behavior = (method, params) -> {
            if ("getMe".equals(method)) {
                return Map.of("username", "ForgeBot");
            }
            bot.stop();
            throw new TelegramException(502, "bad gateway");
        };

        bot.run();
        assertEquals(List.of("getMe", "getUpdates"), api.methods);
    }

    @Test
    void recoversAfterGetMeFailureAndRejectsConcurrentRun() throws Exception {
        var bot = bot(List.of(), List.of(), List.of(), false);
        api.behavior = (method, params) -> {
            throw new TelegramException(401, "Unauthorized");
        };

        assertThrows(TelegramException.class, bot::run);

        var enteredPoll = new CountDownLatch(1);
        var releasePoll = new CountDownLatch(1);
        api.behavior = (method, params) -> {
            if ("getMe".equals(method)) {
                return Map.of("username", "ForgeBot");
            }
            enteredPoll.countDown();
            try {
                releasePoll.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        };

        Thread runner = Thread.ofVirtual().start(bot::run);
        assertTrue(enteredPoll.await(5, TimeUnit.SECONDS));

        assertThrows(IllegalStateException.class, bot::run);

        bot.stop();
        releasePoll.countDown();
        runner.join(5000);
        assertFalse(runner.isAlive());
    }

    @Test
    void stopBeforeRunIsNoOp() {
        var bot = bot(List.of(), List.of(), List.of(), false);
        bot.stop();
    }

    private static Map<String, Object> commandUpdate(long updateId, String text) {
        return Map.of(
                "update_id", updateId,
                "message", Map.of(
                        "message_id", updateId,
                        "chat", Map.of("id", 5L),
                        "from", Map.of("id", 7L, "first_name", "Alice"),
                        "text", text,
                        "entities", List.of(Map.of(
                                "type", "bot_command",
                                "offset", 0L,
                                "length", (long) text.length()))));
    }
}
