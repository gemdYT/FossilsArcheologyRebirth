package com.github.teamfossilsarcheology.fossil.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Loads the existing flat fossil.json format without a configuration library. */
public final class JsonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Map<String, Field> fields = new TreeMap<>();
    private JsonObject document = new JsonObject();

    public JsonConfig(Path path, Class<?> schema) {
        this.path = path.toAbsolutePath();
        for (Field field : schema.getFields()) {
            if (!field.isAnnotationPresent(ConfigEntry.class)) continue;
            if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                    || (field.getType() != boolean.class && field.getType() != int.class
                    && field.getType() != double.class)) {
                throw new IllegalArgumentException("Unsupported config field: " + field);
            }
            fields.put(field.getName(), field);
        }
    }

    public Map<String, Field> fields() {
        return Map.copyOf(fields);
    }

    public void load() throws IOException {
        if (!Files.exists(path)) return;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("Expected a JSON object");
            JsonObject loaded = parsed.getAsJsonObject();
            Map<Field, Object> values = new LinkedHashMap<>();
            for (var entry : fields.entrySet()) {
                if (!loaded.has(entry.getKey())) continue;
                values.put(entry.getValue(), readValue(entry.getValue(), loaded.get(entry.getKey())));
            }
            // Validate the entire file before changing any runtime setting.
            for (var value : values.entrySet()) value.getKey().set(null, value.getValue());
            document = loaded;
        } catch (RuntimeException | IllegalAccessException exception) {
            throw new IOException("Cannot read config " + path, exception);
        }
    }

    private static Object readValue(Field field, JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("Invalid " + field.getName());
        JsonPrimitive value = element.getAsJsonPrimitive();
        if (field.getType() == boolean.class) {
            if (!value.isBoolean()) throw new IllegalArgumentException("Expected boolean: " + field.getName());
            return value.getAsBoolean();
        }
        if (!value.isNumber()) throw new IllegalArgumentException("Expected number: " + field.getName());
        ConfigEntry bounds = field.getAnnotation(ConfigEntry.class);
        if (field.getType() == int.class) {
            BigDecimal number = value.getAsBigDecimal();
            if (number.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Expected integer: " + field.getName());
            }
            BigDecimal minimum = BigDecimal.valueOf(Math.max(Integer.MIN_VALUE, bounds.min()));
            BigDecimal maximum = BigDecimal.valueOf(Math.min(Integer.MAX_VALUE, bounds.max()));
            return number.max(minimum).min(maximum).intValueExact();
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Non-finite " + field.getName());
        return Math.clamp(number, bounds.min(), bounds.max());
    }

    public void save() throws IOException {
        JsonObject updated = document.deepCopy();
        try {
            for (var entry : fields.entrySet()) {
                updated.add(entry.getKey(), GSON.toJsonTree(entry.getValue().get(null)));
            }
        } catch (IllegalAccessException exception) {
            throw new IOException("Cannot serialize config " + path, exception);
        }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(updated) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            document = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
