package io.github.stanikoc.botforge.action;

import org.jspecify.annotations.Nullable;

/**
 * The Telegram user who performed an action.
 */
public record User(long id, String firstName, @Nullable String lastName, @Nullable String username, boolean isBot) {}
