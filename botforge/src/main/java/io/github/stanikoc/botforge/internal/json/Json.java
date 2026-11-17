package io.github.stanikoc.botforge.internal.json;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static @Nullable Object parse(String text) {
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected trailing data");
        }

        return value;
    }

    public static JsonObject parseObject(String text) {
        return JsonObject.of(parse(text));
    }

    public static String write(@Nullable Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, @Nullable Object value) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> writeString(sb, s);
            case Boolean b -> sb.append(b);
            case Double d -> sb.append(finite(d));
            case Float f -> sb.append(finite(f));
            case Number n -> sb.append(n);
            case Map<?, ?> map -> {
                sb.append('{');
                boolean first = true;
                for (var entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new JsonException("Object keys must be strings");
                    }

                    if (!first) {
                        sb.append(',');
                    }

                    first = false;
                    writeString(sb, key);
                    sb.append(':');
                    write(sb, entry.getValue());
                }

                sb.append('}');
            }
            case Iterable<?> iterable -> {
                sb.append('[');
                boolean first = true;
                for (Object element : iterable) {
                    if (!first) {
                        sb.append(',');
                    }

                    first = false;
                    write(sb, element);
                }

                sb.append(']');
            }
            case Object[] array -> write(sb, Arrays.asList(array));
            default -> throw new JsonException("Cannot write " + value.getClass().getName());
        }
    }

    private static double finite(double d) {
        if (!Double.isFinite(d)) {
            throw new JsonException("Cannot write " + d);
        }

        return d;
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u00");
                        sb.append(Character.forDigit(c >> 4, 16));
                        sb.append(Character.forDigit(c & 0xF, 16));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }

        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        @Nullable Object readValue() {
            char c = peek();
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> {
                    if (c == '-' || isDigit(c)) {
                        yield readNumber();
                    }

                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        private Map<String, @Nullable Object> readObject() {
            pos++;
            Map<String, @Nullable Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }

            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw error("Expected a string key");
                }

                String key = readString();
                skipWhitespace();
                if (next() != ':') {
                    throw error("Expected ':'");
                }

                skipWhitespace();
                map.put(key, readValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }

                if (c != ',') {
                    throw error("Expected ',' or '}'");
                }
            }
        }

        private List<@Nullable Object> readArray() {
            pos++;
            List<@Nullable Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }

            while (true) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }

                if (c != ',') {
                    throw error("Expected ',' or ']'");
                }
            }
        }

        private String readString() {
            pos++;
            StringBuilder sb = null;
            int start = pos;
            while (true) {
                if (pos >= s.length()) {
                    throw error("Unterminated string");
                }

                char c = s.charAt(pos);
                if (c == '"') {
                    String tail = s.substring(start, pos);
                    pos++;
                    return sb == null ? tail : sb.append(tail).toString();
                }

                if (c == '\\') {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }

                    sb.append(s, start, pos);
                    pos++;
                    sb.append(readEscape());
                    start = pos;
                } else if (c < 0x20) {
                    throw error("Unescaped control character in string");
                } else {
                    pos++;
                }
            }
        }

        private char readEscape() {
            char c = next();
            return switch (c) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> {
                    if (pos + 4 > s.length()) {
                        throw error("Truncated unicode escape");
                    }

                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        int digit = Character.digit(s.charAt(pos++), 16);
                        if (digit < 0) {
                            throw error("Invalid unicode escape");
                        }

                        value = value * 16 + digit;
                    }

                    yield (char) value;
                }
                default -> throw error("Invalid escape '\\" + c + "'");
            };
        }

        private Object readNumber() {
            int start = pos;
            if (peekIs('-')) {
                pos++;
            }

            if (peekIs('0')) {
                pos++;
            } else {
                readDigits();
            }

            boolean integral = true;
            if (peekIs('.')) {
                integral = false;
                pos++;
                readDigits();
            }

            if (peekIs('e') || peekIs('E')) {
                integral = false;
                pos++;
                if (peekIs('+') || peekIs('-')) {
                    pos++;
                }

                readDigits();
            }

            String number = s.substring(start, pos);
            if (integral) {
                try {
                    return Long.parseLong(number);
                } catch (NumberFormatException _) {
                }
            }

            return Double.parseDouble(number);
        }

        private void readDigits() {
            int start = pos;
            while (pos < s.length() && isDigit(s.charAt(pos))) {
                pos++;
            }

            if (pos == start) {
                throw error("Expected a digit");
            }
        }

        private @Nullable Object readLiteral(String word, @Nullable Object value) {
            if (!s.startsWith(word, pos)) {
                throw error("Invalid literal");
            }

            pos += word.length();
            return value;
        }

        void skipWhitespace() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    return;
                }

                pos++;
            }
        }

        private char peek() {
            if (pos >= s.length()) {
                throw error("Unexpected end of input");
            }

            return s.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private boolean peekIs(char c) {
            return pos < s.length() && s.charAt(pos) == c;
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        JsonException error(String message) {
            return new JsonException(message + " at position " + pos);
        }
    }
}
