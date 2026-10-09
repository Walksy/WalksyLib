package main.walksy.lib.core.config.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class LegacyConfigMigrator {
    private LegacyConfigMigrator() {
    }

    public static boolean isLegacy(final JsonElement root) {
        return root.isJsonArray();
    }

    public static JsonObject migrate(final JsonArray legacyCategories) {
        final JsonObject root = new JsonObject();
        root.addProperty("format", ConfigSerializer.FORMAT_VERSION);
        final JsonObject categories = new JsonObject();
        for (final JsonElement element : legacyCategories) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject legacyCategory = element.getAsJsonObject();
            final String name = stringOrNull(legacyCategory.get("name"));
            if (name == null) {
                continue;
            }
            categories.add(name, migrateCategory(legacyCategory));
        }
        root.add("categories", categories);
        return root;
    }

    private static JsonObject migrateCategory(final JsonObject legacyCategory) {
        final JsonObject category = new JsonObject();
        final JsonObject options = migrateOptions(legacyCategory.get("options"));
        if (!options.isEmpty()) {
            category.add("options", options);
        }
        final JsonObject groups = new JsonObject();
        if (legacyCategory.get("groups") instanceof JsonArray legacyGroups) {
            for (final JsonElement element : legacyGroups) {
                if (!element.isJsonObject()) {
                    continue;
                }
                final JsonObject legacyGroup = element.getAsJsonObject();
                final String name = stringOrNull(legacyGroup.get("name"));
                if (name == null) {
                    continue;
                }
                final JsonObject group = new JsonObject();
                if (legacyGroup.get("expanded") != null && legacyGroup.get("expanded").isJsonPrimitive()) {
                    group.addProperty("expanded", legacyGroup.get("expanded").getAsBoolean());
                }
                group.add("options", migrateOptions(legacyGroup.get("options")));
                groups.add(name, group);
            }
        }
        category.add("groups", groups);
        return category;
    }

    private static JsonObject migrateOptions(final JsonElement legacyOptions) {
        final JsonObject options = new JsonObject();
        if (!(legacyOptions instanceof JsonArray array)) {
            return options;
        }
        for (final JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject legacyOption = element.getAsJsonObject();
            final String name = stringOrNull(legacyOption.get("name"));
            final JsonElement value = legacyOption.get("value");
            if (name == null || value == null || value.isJsonNull()) {
                continue;
            }
            options.add(name, value);
        }
        return options;
    }

    private static String stringOrNull(final JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        return element.getAsString();
    }
}
