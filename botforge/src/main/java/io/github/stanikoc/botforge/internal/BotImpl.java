package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.Bot;
import io.github.stanikoc.botforge.TelegramException;
import io.github.stanikoc.botforge.action.Action;
import io.github.stanikoc.botforge.internal.json.JsonObject;
import io.github.stanikoc.botforge.registry.Registry;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

public final class BotImpl implements Bot {
    private static final System.Logger LOG = System.getLogger("io.github.stanikoc.botforge");
    private static final int POLL_TIMEOUT_SECONDS = 30;
    private static final int MAX_BACKOFF_SECONDS = 30;
    private static final List<String> ALLOWED_UPDATES = List.of("message", "callback_query");

    private final TelegramApi api;
    private final Dispatcher dispatcher;
    private final Registry<CommandRoute> commands;
    private final boolean publishCommands;

    private volatile boolean running;
    private @Nullable Thread pollThread;

    public BotImpl(TelegramApi api, Dispatcher dispatcher, Registry<CommandRoute> commands, boolean publishCommands) {
        this.api = api;
        this.dispatcher = dispatcher;
        this.commands = commands;
        this.publishCommands = publishCommands;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (pollThread != null) {
                throw new IllegalStateException("Bot is already running");
            }

            pollThread = Thread.currentThread();
            running = true;
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            String username = JsonObject.of(api.call("getMe", Map.of())).requireString("username");
            if (publishCommands) {
                publishCommands();
            }

            long offset = poll(executor, username);
            synchronized (this) {
                pollThread = null;
            }

            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
            confirm(offset);
        } finally {
            synchronized (this) {
                pollThread = null;
                running = false;
            }
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }

    private long poll(Executor executor, String username) {
        long offset = 0;
        long backoff = 1;
        while (running) {
            List<?> updates;
            try {
                Object result = api.call(
                        "getUpdates",
                        Map.of("offset", offset, "timeout", POLL_TIMEOUT_SECONDS, "allowed_updates", ALLOWED_UPDATES),
                        Duration.ofSeconds(POLL_TIMEOUT_SECONDS + 10));
                updates = result instanceof List<?> list ? list : List.of();
                backoff = 1;
            } catch (TelegramException e) {
                if (!running) {
                    break;
                }

                LOG.log(WARNING, "Polling failed, retrying in " + backoff + "s: " + e.getMessage());
                try {
                    Thread.sleep(Duration.ofSeconds(backoff));
                } catch (InterruptedException interrupted) {
                    break;
                }

                backoff = Math.min(backoff * 2, MAX_BACKOFF_SECONDS);
                continue;
            }

            for (Object raw : updates) {
                JsonObject update = JsonObject.of(raw);
                offset = update.requireLong("update_id") + 1;
                Action action = parse(update, username);
                if (action != null) {
                    executor.execute(() -> handle(action));
                }
            }
        }

        return offset;
    }

    private static @Nullable Action parse(JsonObject update, String username) {
        try {
            return UpdateParser.parse(update, username);
        } catch (RuntimeException e) {
            LOG.log(WARNING, "Skipping malformed update", e);
            return null;
        }
    }

    private void handle(Action action) {
        try {
            dispatcher.dispatch(action);
        } catch (Exception e) {
            LOG.log(ERROR, "Handler failed for " + action.getClass().getSimpleName()
                    + " in chat " + action.chatId(), e);
        }
    }

    private void confirm(long offset) {
        if (offset == 0) {
            return;
        }

        try {
            api.call("getUpdates", Map.of("offset", offset, "timeout", 0, "limit", 1));
        } catch (TelegramException e) {
            LOG.log(WARNING, "Could not confirm processed updates: " + e.getMessage());
        }
    }

    private void publishCommands() {
        List<Map<String, String>> list = commands.stream()
                .map(c -> Map.of("command", c.name(), "description", c.description()))
                .toList();
        if (list.isEmpty()) {
            return;
        }

        try {
            api.call("setMyCommands", Map.of("commands", list));
        } catch (TelegramException e) {
            LOG.log(WARNING, "Could not publish the command menu: " + e.getMessage());
        }
    }
}
