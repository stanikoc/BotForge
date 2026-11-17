package io.github.stanikoc.botforge.keyboard;

import java.util.Arrays;
import java.util.Objects;

final class InlineKeyboardImpl implements InlineKeyboard {
    private final InlineButton[] buttons;
    private final int[] rowStarts;

    private InlineKeyboardImpl(InlineButton[] buttons, int[] rowStarts) {
        this.buttons = buttons;
        this.rowStarts = rowStarts;
    }

    @Override
    public int rowCount() {
        return rowStarts.length - 1;
    }

    @Override
    public int rowSize(int row) {
        Objects.checkIndex(row, rowCount());
        return rowStarts[row + 1] - rowStarts[row];
    }

    @Override
    public InlineButton button(int row, int column) {
        Objects.checkIndex(column, rowSize(row));
        return buttons[rowStarts[row] + column];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InlineKeyboardImpl other
                && Arrays.equals(rowStarts, other.rowStarts)
                && Arrays.equals(buttons, other.buttons);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(rowStarts) + Arrays.hashCode(buttons);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InlineKeyboard[");
        for (int row = 0; row < rowCount(); row++) {
            if (row > 0) {
                sb.append(", ");
            }

            sb.append(Arrays.toString(Arrays.copyOfRange(buttons, rowStarts[row], rowStarts[row + 1])));
        }

        return sb.append(']').toString();
    }

    static final class ArrayBuilder implements Builder {
        private InlineButton[] buttons = new InlineButton[8];
        private int[] rowStarts = new int[4];
        private int buttonCount;
        private int rowCount;

        @Override
        public Builder row(InlineButton... row) {
            if (row.length == 0) {
                throw new IllegalArgumentException("A row needs at least one button");
            }

            for (InlineButton button : row) {
                Objects.requireNonNull(button, "button");
            }

            if (buttonCount + row.length > buttons.length) {
                buttons = Arrays.copyOf(buttons, Math.max(buttons.length * 2, buttonCount + row.length));
            }

            if (rowCount + 2 > rowStarts.length) {
                rowStarts = Arrays.copyOf(rowStarts, rowStarts.length * 2);
            }

            System.arraycopy(row, 0, buttons, buttonCount, row.length);
            buttonCount += row.length;
            rowStarts[++rowCount] = buttonCount;
            return this;
        }

        @Override
        public InlineKeyboard build() {
            return new InlineKeyboardImpl(
                    Arrays.copyOf(buttons, buttonCount),
                    Arrays.copyOf(rowStarts, rowCount + 1));
        }
    }
}
