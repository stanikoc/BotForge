package io.github.stanikoc.botforge.trait;

import org.jspecify.annotations.NonNull;

/**
 * A trait for any object containing a description as a string.
 */
public interface HasDescription {
    @NonNull String description();
}
