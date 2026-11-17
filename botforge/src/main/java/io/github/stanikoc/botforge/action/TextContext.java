package io.github.stanikoc.botforge.action;

public interface TextContext extends Context<Action.Text> {
    default String text() {
        return action().text();
    }
}
