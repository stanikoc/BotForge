package io.github.stanikoc.botforge.registry;

import io.github.stanikoc.botforge.trait.Nameable;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Registry<E extends Nameable> extends Iterable<E> {
    Optional<E> getByName(String name);

    int size();

    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
