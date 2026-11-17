package io.github.stanikoc.botforge.registry;

import io.github.stanikoc.botforge.trait.Nameable;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class RegistryImpl<E extends Nameable> implements Registry<E> {
    private final String [] names;
    private final E[] values;
    private final int[] table;

    @SafeVarargs
    public RegistryImpl(E... nameables) {
        this.values = nameables.clone();
        this.names = Arrays.stream(this.values)
                .map(Nameable::name)
                .toArray(String[]::new);
        this.table = buildTable(this.names);
    }

    @Override
    public Optional<E> getByName(String name) {
        int i = indexOf(this.table, this.names, name);
        return i == -1 ? Optional.empty() : Optional.of(this.values[i]);
    }

    @Override
    public int size() {
        return this.values.length;
    }

    @Override
    public @NonNull Iterator<E> iterator() {
        return new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return this.cursor != values.length;
            }

            @Override
            public E next() {
                try {
                    int i = this.cursor;
                    E next = values[i];
                    this.cursor = i + 1;
                    return next;
                } catch (IndexOutOfBoundsException var3) {
                    throw new NoSuchElementException();
                }
            }
        };
    }


    private static int[] buildTable(String[] keys) {
        if (keys.length == 0) {
            return new int[0];
        }

        int capacity = Integer.highestOneBit(Math.max(2, keys.length * 2 - 1)) << 1;
        int[] table = new int[capacity];
        Arrays.fill(table, -1);

        int mask = capacity - 1;
        for (int k = 0; k < keys.length; k++) {
            int slot = hashIgnoreCase(keys[k]) & mask;
            int i;
            while ((i = table[slot]) != -1) {
                if (keys[k].equalsIgnoreCase(keys[i])) {
                    throw new IllegalArgumentException("Duplicate name: " + keys[k]);
                }

                slot = (slot + 1) & mask;
            }

            table[slot] = k;
        }

        return table;
    }

    private static int indexOf(int[] table, String[] keys, String key) {
        if (table.length == 0) {
            return -1;
        }

        int mask = table.length - 1;
        int slot = hashIgnoreCase(key) & mask;
        int i;
        while ((i = table[slot]) != -1) {
            if (key.equalsIgnoreCase(keys[i])) {
                return i;
            }

            slot = (slot + 1) & mask;
        }

        return -1;
    }

    private static int hashIgnoreCase(String key) {
        int h = 0;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + Character.toLowerCase(key.charAt(i));
        }

        return h ^ (h >>> 16);
    }
}
