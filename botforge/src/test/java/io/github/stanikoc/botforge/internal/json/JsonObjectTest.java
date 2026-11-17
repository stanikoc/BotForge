package io.github.stanikoc.botforge.internal.json;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonObjectTest {
    @Test
    void wrapsMapAndRejectsNonMaps() {
        Map<String, Object> raw = Map.of("k", "v");
        assertEquals(raw, JsonObject.of(raw).map());

        assertThrows(JsonException.class, () -> JsonObject.of(null));
        assertThrows(JsonException.class, () -> JsonObject.of("text"));
        assertThrows(JsonException.class, () -> JsonObject.of(List.of(1)));
    }

    @Test
    void readsAndRequiresNestedObjects() {
        var obj = new JsonObject(Map.of("child", Map.of("id", 1L), "wrong", "nope"));

        assertEquals(new JsonObject(Map.of("id", 1L)), obj.object("child"));
        assertNull(obj.object("missing"));
        assertNull(obj.object("wrong"));

        assertEquals(new JsonObject(Map.of("id", 1L)), obj.requireObject("child"));
        assertThrows(JsonException.class, () -> obj.requireObject("missing"));
        assertThrows(JsonException.class, () -> obj.requireObject("wrong"));
    }

    @Test
    void readsAndRequiresStrings() {
        var obj = new JsonObject(Map.of("name", "Alice", "wrong", 123));

        assertEquals("Alice", obj.string("name"));
        assertNull(obj.string("missing"));
        assertNull(obj.string("wrong"));

        assertEquals("Alice", obj.requireString("name"));
        assertThrows(JsonException.class, () -> obj.requireString("missing"));
        assertThrows(JsonException.class, () -> obj.requireString("wrong"));
    }

    @Test
    void readsArraysWithEmptyFallback() {
        var obj = new JsonObject(Map.of("items", List.of("a", "b"), "wrong", "nope"));

        assertEquals(List.of("a", "b"), obj.array("items"));
        assertEquals(List.of(), obj.array("missing"));
        assertEquals(List.of(), obj.array("wrong"));
    }

    @Test
    void readsBooleansSafely() {
        var obj = new JsonObject(Map.of("yes", true, "no", false, "wrong", "true"));

        assertTrue(obj.bool("yes"));
        assertFalse(obj.bool("no"));
        assertFalse(obj.bool("missing"));
        assertFalse(obj.bool("wrong"));
    }

    @Test
    void readsAndRequiresNumbers() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 42L);
        map.put("flt", 7.9);
        map.put("wrong", "42");
        map.put("nullVal", null);
        var obj = new JsonObject(map);

        assertEquals(42L, obj.longOr("id", -1));
        assertEquals(7L, obj.longOr("flt", -1));
        assertEquals(-1L, obj.longOr("missing", -1));
        assertEquals(-1L, obj.longOr("wrong", -1));
        assertEquals(-1L, obj.longOr("nullVal", -1));

        assertEquals(42L, obj.requireLong("id"));
        assertThrows(JsonException.class, () -> obj.requireLong("missing"));
        assertThrows(JsonException.class, () -> obj.requireLong("wrong"));
        assertThrows(JsonException.class, () -> obj.requireLong("nullVal"));
    }
}
