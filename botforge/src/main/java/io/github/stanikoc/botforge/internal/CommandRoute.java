package io.github.stanikoc.botforge.internal;

import io.github.stanikoc.botforge.action.CommandHandler;
import io.github.stanikoc.botforge.trait.HasDescription;
import io.github.stanikoc.botforge.trait.Nameable;

public record CommandRoute(String name, String description, CommandHandler handler)
        implements Nameable, HasDescription {}
