package com.tianma.xsmscode.feature.config;

import org.junit.Test;
import java.util.*;
import com.google.gson.*;
import static org.junit.Assert.*;

public class ConfigDocumentTest {
    @Test public void allPreferenceTypesRoundTripWithoutLosingLongPrecision() {
        Map<String,Object> input = new LinkedHashMap<>();
        input.put("switch", true); input.put("delay", "1.5"); input.put("integer", 3);
        input.put("long", 9007199254740993L); input.put("float", 1.25f);
        input.put("set", new HashSet<>(Arrays.asList("a", "b")));
        assertEquals(input, ConfigDocument.decodePreferences(ConfigDocument.encodePreferences(input)));
    }
    @Test public void backupHasOnlyConfigurationSections() {
        JsonObject doc = ConfigDocument.empty(); ConfigDocument.validate(doc);
        assertEquals(new HashSet<>(Arrays.asList("format", "version", "preferences", "rules", "blockedApps")), doc.keySet());
    }
    @Test(expected=IllegalArgumentException.class) public void unknownVersionRejected() {
        JsonObject doc = ConfigDocument.empty(); doc.addProperty("version", 2); ConfigDocument.validate(doc);
    }
    @Test(expected=IllegalArgumentException.class) public void traversalPreferenceGroupRejected() {
        JsonObject doc = ConfigDocument.empty(); doc.getAsJsonObject("preferences").add("../data", new JsonObject()); ConfigDocument.validate(doc);
    }
    @Test(expected=IllegalArgumentException.class) public void truncatedDocumentRejected() {
        ConfigDocument.validate(JsonParser.parseString("{}").getAsJsonObject());
    }
}
