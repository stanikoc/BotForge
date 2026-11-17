package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.TextHandler;

import java.util.function.Predicate;

public record TextRoute(Predicate<String> condition, TextHandler handler) {}
