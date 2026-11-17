package io.github.stanikoc.botforge.trait;

import org.jspecify.annotations.NonNull;

/**
 * A trait for any object containing a name as a string.
 */
public interface Nameable {
    @NonNull String name();
}
