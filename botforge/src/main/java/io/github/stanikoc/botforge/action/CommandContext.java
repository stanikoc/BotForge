package io.github.stanikoc.botforge.action;

import java.util.List;

public interface CommandContext extends Context<Action.Command> {
    default String name() {
        return action().name();
    }

    default String argument() {
        return action().argument();
    }

    default List<String> args() {
        return action().args();
    }
}
