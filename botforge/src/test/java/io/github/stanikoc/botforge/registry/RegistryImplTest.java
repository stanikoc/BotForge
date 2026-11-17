package io.github.stanikoc.botforge.registry;

import io.github.stanikoc.botforge.trait.Nameable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryImplTest {
    @Test
    void getByNameIsCaseInsensitive() {
        var registry = of("a", "B");

        assertTrue(registry.getByName("a").isPresent());
        assertTrue(registry.getByName("A").isPresent());
        assertTrue(registry.getByName("b").isPresent());
        assertFalse(registry.getByName("c").isPresent());
    }

    @Test
    void reportsSizeAndEmptyLookup() {
        var registry = of("1", "2", "3");
        assertEquals(3, registry.size());

        var emptyRegistry = of();
        assertEquals(0, emptyRegistry.size());
        assertFalse(emptyRegistry.getByName("1").isPresent());
    }

    @Test
    void iteratesAndStreamsInInsertionOrder() {
        var registry = of("alpha", "beta", "gamma");

        List<String> iterated = new ArrayList<>();
        for (Nameable item : registry) {
            iterated.add(item.name());
        }
        assertEquals(List.of("alpha", "beta", "gamma"), iterated);
        assertEquals(List.of("alpha", "beta", "gamma"), registry.stream().map(Nameable::name).toList());

        var iterator = registry.iterator();
        iterator.next();
        iterator.next();
        iterator.next();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void rejectsDuplicateNamesCaseInsensitively() {
        assertThrows(IllegalArgumentException.class, () -> of("start", "start"));
        assertThrows(IllegalArgumentException.class, () -> of("Vote", "vote"));
    }

    @Test
    void resolvesHashCollisionsAndManyEntries() {
        // "a?" and "b " share the same case-insensitive hash (31 * 'a' + '?' == 31 * 'b' + ' ')
        var registry = of("a?", "b ", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9", "k10");

        assertEquals("a?", registry.getByName("A?").orElseThrow().name());
        assertEquals("b ", registry.getByName("B ").orElseThrow().name());
        assertEquals("k10", registry.getByName("K10").orElseThrow().name());
        // "c\u0001" shares the same hash as "a?" and "b " without being present
        assertTrue(registry.getByName("c\u0001").isEmpty());
    }

    @Test
    void clonesInputArrayOnConstruction() {
        Nameable[] source = {new NameableImpl("first")};
        var registry = new RegistryImpl<>(source);
        source[0] = new NameableImpl("replaced");

        assertTrue(registry.getByName("first").isPresent());
        assertTrue(registry.getByName("replaced").isEmpty());
    }

    private static Registry<Nameable> of(String... names) {
        return new RegistryImpl<>(Arrays.stream(names).map(NameableImpl::new).toArray(Nameable[]::new));
    }

    private record NameableImpl(@NonNull String name) implements Nameable {}
}
