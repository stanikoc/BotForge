package io.github.stanikoc.botforge.keyboard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InlineKeyboardTest {
    private static final InlineButton A = InlineButton.callback("A", "ns", "a");
    private static final InlineButton B = InlineButton.callback("B", "ns", "b");
    private static final InlineButton C = InlineButton.url("C", "https://example.com");

    @Test
    void keepsRowsOfDifferentLengths() {
        InlineKeyboard keyboard = InlineKeyboard.builder().row(A, B).row(C).row(A, B, C).build();

        assertEquals(3, keyboard.rowCount());
        assertEquals(2, keyboard.rowSize(0));
        assertEquals(1, keyboard.rowSize(1));
        assertEquals(3, keyboard.rowSize(2));
        assertEquals(B, keyboard.button(0, 1));
        assertEquals(C, keyboard.button(1, 0));
        assertEquals(C, keyboard.button(2, 2));
    }

    @Test
    void growsPastInitialCapacity() {
        var builder = InlineKeyboard.builder();
        for (int i = 0; i < 20; i++) {
            builder.row(A, B, C);
        }

        InlineKeyboard keyboard = builder.build();
        assertEquals(20, keyboard.rowCount());
        assertEquals(C, keyboard.button(19, 2));
    }

    @Test
    void growsPastDoubleCapacityOnWideSingleRow() {
        InlineButton[] row = new InlineButton[17];
        Arrays.fill(row, A);
        row[16] = C;

        InlineKeyboard keyboard = InlineKeyboard.of(row);
        assertEquals(1, keyboard.rowCount());
        assertEquals(17, keyboard.rowSize(0));
        assertEquals(C, keyboard.button(0, 16));
    }

    @Test
    void supportsEmptyBuilder() {
        InlineKeyboard empty = InlineKeyboard.builder().build();

        assertEquals(0, empty.rowCount());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.rowSize(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.button(0, 0));
    }

    @Test
    void rejectsOutOfRangeIndices() {
        InlineKeyboard keyboard = InlineKeyboard.builder().row(A, B).row(C).build();

        assertThrows(IndexOutOfBoundsException.class, () -> keyboard.rowSize(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> keyboard.rowSize(2));
        assertThrows(IndexOutOfBoundsException.class, () -> keyboard.button(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> keyboard.button(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> keyboard.button(0, -1));
    }

    @Test
    void builtKeyboardIsUnaffectedByLaterRows() {
        var builder = InlineKeyboard.builder().row(A);
        InlineKeyboard first = builder.build();
        builder.row(B);

        assertEquals(1, first.rowCount());
        assertEquals(2, builder.build().rowCount());
    }

    @Test
    void comparesByContentAndFormatsToString() {
        assertEquals(InlineKeyboard.builder().row(A, B).build(), InlineKeyboard.of(A, B));
        assertEquals(InlineKeyboard.of(A, B).hashCode(), InlineKeyboard.of(A, B).hashCode());
        assertNotEquals(InlineKeyboard.builder().row(A).row(B).build(), InlineKeyboard.of(A, B));
        assertNotEquals(InlineKeyboard.of(A), null);
        assertNotEquals(InlineKeyboard.of(A), "not a keyboard");
        assertEquals(
                "InlineKeyboard[[" + A + ", " + B + "], [" + C + "]]",
                InlineKeyboard.builder().row(A, B).row(C).build().toString());
    }

    @Test
    void rejectsEmptyRowsAndNullButtons() {
        assertThrows(IllegalArgumentException.class, () -> InlineKeyboard.builder().row());
        assertThrows(NullPointerException.class, () -> InlineKeyboard.builder().row(A, null));
    }
}
