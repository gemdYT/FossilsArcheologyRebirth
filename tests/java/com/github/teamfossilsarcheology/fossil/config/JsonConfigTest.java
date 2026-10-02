package com.github.teamfossilsarcheology.fossil.config;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone behavioral checks; no game bootstrap or additional test library required. */
public final class JsonConfigTest {
    public static final class Settings {
        @ConfigEntry public static boolean enabled = true;
        @ConfigEntry(min = 1, max = 100) public static int ticks = 10;
        @ConfigEntry(min = 0, max = 10) public static double hardness = 5;
        public static String untracked = "not a setting";
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("fossil-config-test-");
        Path file = directory.resolve("fossil.json");
        try {
            JsonConfig config = new JsonConfig(file, Settings.class);
            config.load();
            check(Settings.ticks == 10, "missing config preserves defaults");
            config.save();
            check(Files.exists(file), "missing config is created");
            check(!Files.readString(file).contains("untracked"), "only annotated fields are saved");
            Files.writeString(file, "{\"enabled\":false,\"ticks\":999999999999999999999,\"hardness\":-2,\"addon\":{\"key\":42}}");
            config.load();
            check(!Settings.enabled && Settings.ticks == 100 && Settings.hardness == 0,
                    "boolean values are loaded and numbers respect bounds");
            config.save();
            var saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            check(saved.getAsJsonObject("addon").get("key").getAsInt() == 42, "unknown fields survive saving");
            check(saved.get("ticks").getAsInt() == 100, "clamped values are persisted");
            Files.writeString(file, "{\"ticks\":7}");
            config.load();
            check(Settings.ticks == 7 && !Settings.enabled, "partial configs preserve unspecified values");
            for (String invalid : new String[]{"{broken", "[]", "{\"enabled\":\"false\"}",
                    "{\"enabled\":true,\"ticks\":1.5}", "{\"hardness\":1e999}", "{\"ticks\":null}"}) {
                Files.writeString(file, invalid);
                boolean rejected = false;
                try { config.load(); } catch (IOException expected) { rejected = true; }
                check(rejected, "invalid config is rejected: " + invalid);
                check(Settings.ticks == 7 && !Settings.enabled && Settings.hardness == 0,
                        "invalid config cannot partially change settings");
                check(Files.readString(file).equals(invalid), "invalid file is preserved");
            }
            Files.writeString(file, "{\"ticks\":-99,\"hardness\":99}");
            config.load();
            check(Settings.ticks == 1 && Settings.hardness == 10, "opposite bounds are enforced");
            System.out.println("JsonConfig: all persistence, bounds, and corruption checks passed");
        } finally {
            Files.deleteIfExists(file);
            try (var children = Files.list(directory)) {
                check(children.findAny().isEmpty(), "atomic saves leave no temporary files");
            }
            Files.deleteIfExists(directory);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
