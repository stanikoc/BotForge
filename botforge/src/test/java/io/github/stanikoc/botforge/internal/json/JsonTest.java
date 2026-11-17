package io.github.stanikoc.botforge.internal.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {
    @Test
    void parsesPrimitivesWithSurroundingWhitespace() {
        assertNull(Json.parse("  null \n\t\r"));
        assertEquals(Boolean.TRUE, Json.parse(" true "));
        assertEquals(Boolean.FALSE, Json.parse("\tfalse"));
        assertEquals(0L, Json.parse("0"));
        assertEquals(-12.5, Json.parse("-12.5"));
        assertEquals(2.5e-1, Json.parse("2.5E-1"));
        assertEquals(100.0, Json.parse("1e+2"));
        assertEquals("plain", Json.parse("\"plain\""));
    }

    @Test
    void parsesNestedStructures() {
        JsonObject root = Json.parseObject("""
                {"ok": true, "result": [{"update_id": 9007199254740991, "message": {"text": "hi", "x": null}}],
                 "neg": -42, "pi": 3.25, "exp": 1e3, "empty": {}, "none": []}
                """);

        assertTrue(root.bool("ok"));
        JsonObject update = JsonObject.of(root.array("result").getFirst());
        assertEquals(9007199254740991L, update.requireLong("update_id"));
        assertEquals("hi", update.requireObject("message").requireString("text"));
        assertNull(update.requireObject("message").map().get("x"));
        assertEquals(-42L, root.map().get("neg"));
        assertEquals(3.25, root.map().get("pi"));
        assertEquals(1000.0, root.map().get("exp"));
        assertEquals(Map.of(), root.map().get("empty"));
        assertEquals(List.of(), root.map().get("none"));
    }

    @Test
    void fallsBackToDoubleWhenIntegerOverflowsLong() {
        Object parsed = Json.parse("99999999999999999999");
        assertInstanceOf(Double.class, parsed);
    }

    @Test
    void decodesEscapesIncludingSurrogatePairs() {
        assertEquals(
                "a\"b\\c/d\b\f\n\r\té😀",
                Json.parse("\"a\\\"b\\\\c\\/d\\b\\f\\n\\r\\t\\u00e9\\ud83d\\ude00\""));
    }

    @Test
    void writesAndReadsBack() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("chat_id", -1001234567890L);
        value.put("text", "line1\nline2\r\t\b\f \"quoted\" \u0001 😀");
        value.put("flags", List.of(true, false));
        value.put("nothing", null);

        String json = Json.write(value);
        assertEquals(
                "{\"chat_id\":-1001234567890,\"text\":\"line1\\nline2\\r\\t\\b\\f \\\"quoted\\\" \\u0001 😀\",\"flags\":[true,false],\"nothing\":null}",
                json);
        assertEquals(value, Json.parse(json));
    }

    @Test
    void writesArraysAndFloatsAndNullElements() {
        assertEquals("[1,null]", Json.write(Arrays.asList(1, null)));
        assertEquals("[\"a\",2,1.5]", Json.write(new Object[]{"a", 2, 1.5f}));
    }

    @Test
    void parseObjectRejectsNonObjectJson() {
        assertThrows(JsonException.class, () -> Json.parseObject("[1, 2]"));
        assertThrows(JsonException.class, () -> Json.parseObject("null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "{", "[", "[1,]", "{\"a\":1,}", "{\"a\" 1}", "{1:2}",
            "01", "-", "1.", "1e", "1e+", "\"abc", "\"\\x\"", "\"\\u12\"", "\"\\u12zz\"",
            "tru", "fals", "nul", "{} {}", "\"\t\"", "x"
    })
    void rejectsInvalidInput(String input) {
        assertThrows(JsonException.class, () -> Json.parse(input));
    }

    @Test
    void rejectsUnwritableValues() {
        assertThrows(JsonException.class, () -> Json.write(Double.NaN));
        assertThrows(JsonException.class, () -> Json.write(Double.POSITIVE_INFINITY));
        assertThrows(JsonException.class, () -> Json.write(Float.NEGATIVE_INFINITY));
        assertThrows(JsonException.class, () -> Json.write(new Object()));
        assertThrows(JsonException.class, () -> Json.write(Map.of(1, "a")));
    }
}
