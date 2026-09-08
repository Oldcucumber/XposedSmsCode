package com.tianma.xsmscode.feature.config;

import com.google.gson.*;
import java.util.*;

/** Versioned, typed configuration format. Contains no SMS records or log data. */
public final class ConfigDocument {
    public static final String FORMAT = "XposedSmsCode-config";
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    public static JsonObject empty() {
        JsonObject d = new JsonObject(); d.addProperty("format", FORMAT); d.addProperty("version", 1);
        d.add("preferences", new JsonObject()); d.add("rules", new JsonArray()); d.add("blockedApps", new JsonArray());
        return d;
    }
    public static JsonObject encodePreferences(Map<String, ?> values) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object v = entry.getValue(); JsonObject typed = new JsonObject(); String type;
            if (v instanceof String) type = "string";
            else if (v instanceof Boolean) type = "boolean";
            else if (v instanceof Integer) type = "int";
            else if (v instanceof Long) type = "long";
            else if (v instanceof Float) type = "float";
            else if (v instanceof Set) type = "stringSet";
            else throw new IllegalArgumentException("Unsupported preference type");
            typed.addProperty("type", type); typed.add("value", new Gson().toJsonTree(v));
            result.add(entry.getKey(), typed);
        }
        return result;
    }
    public static Map<String, Object> decodePreferences(JsonObject values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : values.entrySet()) {
            JsonObject t = e.getValue().getAsJsonObject(); JsonElement v = t.get("value"); Object value;
            switch (t.get("type").getAsString()) {
                case "string": if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(); value = v.getAsString(); break;
                case "boolean": if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException(); value = v.getAsBoolean(); break;
                case "int": value = v.getAsBigDecimal().intValueExact(); break;
                case "long": value = v.getAsBigDecimal().longValueExact(); break;
                case "float": float f = v.getAsFloat(); if (!Float.isFinite(f)) throw new IllegalArgumentException(); value = f; break;
                case "stringSet": Set<String> set = new HashSet<>(); for (JsonElement item : v.getAsJsonArray()) {
                    if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(); set.add(item.getAsString());
                } value = set; break;
                default: throw new IllegalArgumentException("Unknown preference type");
            }
            result.put(e.getKey(), value);
        }
        return result;
    }
    public static void validate(JsonObject d) {
        try {
            if (!FORMAT.equals(d.get("format").getAsString()) || d.get("version").getAsBigDecimal().intValueExact() != 1) throw new IllegalArgumentException();
            for (Map.Entry<String, JsonElement> p : d.getAsJsonObject("preferences").entrySet()) {
                if (!p.getKey().matches("[A-Za-z0-9_.-]+") || p.getKey().startsWith("apiupdate_")) throw new IllegalArgumentException("Unsafe preference group");
                decodePreferences(p.getValue().getAsJsonObject());
            }
            Set<String> rules = new HashSet<>();
            for (JsonElement e : d.getAsJsonArray("rules")) {
                JsonObject r = e.getAsJsonObject();
                requireString(r, "company"); requireString(r, "keyword"); requireString(r, "regex");
                if (!rules.add(r.get("company") + "\n" + r.get("keyword") + "\n" + r.get("regex"))) throw new IllegalArgumentException("Duplicate rule");
            }
            Set<String> packages = new HashSet<>();
            for (JsonElement e : d.getAsJsonArray("blockedApps")) {
                JsonObject a = e.getAsJsonObject(); requireString(a, "packageName"); requireString(a, "label");
                if (!a.get("blocked").getAsJsonPrimitive().isBoolean() || !packages.add(a.get("packageName").getAsString())) throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid or unsupported configuration backup", e); }
    }
    private static void requireString(JsonObject obj, String name) {
        if (!obj.has(name) || !obj.get(name).isJsonPrimitive() || !obj.get(name).getAsJsonPrimitive().isString()) throw new IllegalArgumentException(name);
    }
}
