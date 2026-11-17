package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.ButtonHandler;
import io.github.stanikoc.botforge.trait.Nameable;

public record ButtonRoute(String name, ButtonHandler handler) implements Nameable {}
