package main.walksy.lib.core.config.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.Config;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.config.serialization.SerializableCategory;
import main.walksy.lib.core.manager.WalksyLibConfigManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record LocalConfig(String name, Path path, List<Category> categories) implements Config {

    @Override
    public void load() {
        Path path = this.path();
        if (!Files.exists(path)) {
            this.save();
            return;
        }

        List<SerializableCategory> loadedCategories;
        try {
            String json = Files.readString(path);
            Type type = new TypeToken<List<SerializableCategory>>() {
            }.getType();
            loadedCategories = WalksyLib.GSON.fromJson(json, type);
        } catch (IOException | JsonParseException e) {
            System.err.println("Failed to read or parse config from " + path + ": " + e.getMessage());
            return;
        }

        for (Category existingCategory : this.categories()) {
            loadedCategories.stream()
                    .filter(serialized -> serialized.name.equals(existingCategory.name()))
                    .findFirst()
                    .ifPresent(serialized -> WalksyLibConfigManager.applyCategoryValues(existingCategory, serialized));
        }
    }

    @Override
    public void save() {
        Path path = this.path();
        List<SerializableCategory> serializedCategories = new ArrayList<>();

        for (Category category : this.categories()) {
            serializedCategories.add(WalksyLibConfigManager.serializeCategory(category));
        }

        try {
            Files.createDirectories(path.getParent());
            String json = WalksyLib.GSON.toJson(serializedCategories);
            Files.writeString(path, json);
        } catch (IOException e) {
            System.err.println("Failed to save config to " + path + ": " + e.getMessage());
        }
    }

    public static LocalConfigBuilder createBuilder(String name) {
        return new LocalConfigBuilder(name);
    }
}
