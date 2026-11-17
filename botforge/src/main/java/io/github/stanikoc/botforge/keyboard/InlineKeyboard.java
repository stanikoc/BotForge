package io.github.stanikoc.botforge.keyboard;

/**
 * An immutable grid of buttons attached to a message. Rows may have different lengths.
 */
public interface InlineKeyboard {
    int rowCount();

    /**
     * The number of buttons in {@code row}.
     *
     * @throws IndexOutOfBoundsException if {@code row} is out of range
     */
    int rowSize(int row);

    /**
     * The button at {@code column} in {@code row}.
     *
     * @throws IndexOutOfBoundsException if either index is out of range
     */
    InlineButton button(int row, int column);

    /** A keyboard with a single row. */
    static InlineKeyboard of(InlineButton... row) {
        return builder().row(row).build();
    }

    static Builder builder() {
        return new InlineKeyboardImpl.ArrayBuilder();
    }

    interface Builder {
        /**
         * Appends a row.
         *
         * @throws IllegalArgumentException if {@code buttons} is empty
         */
        Builder row(InlineButton... buttons);

        /** Creates the keyboard. The builder can keep being used afterwards without affecting it. */
        InlineKeyboard build();
    }
}
