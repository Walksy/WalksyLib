package main.walksy.lib.core.config.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import main.walksy.lib.core.config.Config;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.config.serialization.SerializableCategory;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.utils.log.WalksyLibLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record ModConfig(Path getPath, List<Category> categories, Runnable saveCallback) implements Config {

    @Override
    public void onLoad() {
        final Path path = this.getPath();
        if (!Files.exists(path)) {
            this.onSave();
            return;
        }

        List<SerializableCategory> loadedCategories;
        try {
            final String json = Files.readString(path);
            final Type type = new TypeToken<List<SerializableCategory>>() {}.getType();
            loadedCategories = WalksyLibConfigManager.GSON.fromJson(json, type);
        } catch (IOException | JsonParseException e) {
            WalksyLibLogger.err("Failed to read or parse config from " + path + ": " + e.getMessage());
            return;
        }

        for (final Category existingCategory : this.categories()) {
            loadedCategories.stream()
                    .filter(serialized -> serialized.name.equals(existingCategory.name()))
                    .findFirst()
                    .ifPresent(serialized -> WalksyLibConfigManager.applyCategoryValues(existingCategory, serialized));
        }
    }


    @Override
    public void onSave() {
        final Path path = this.getPath();
        final List<SerializableCategory> serializedCategories = new ArrayList<>();

        for (final Category category : this.categories()) {
            serializedCategories.add(WalksyLibConfigManager.serializeCategory(category));
        }

        try {
            Files.createDirectories(path.getParent());
            final String json = WalksyLibConfigManager.GSON.toJson(serializedCategories);
            Files.writeString(path, json);
        } catch (IOException e) {
            WalksyLibLogger.err("Failed to save config to " + path + ": " + e.getMessage());
        }
    }

    public void runSave() {
        if (this.saveCallback != null) {
            this.saveCallback.run();
        }
    }

    public static LocalConfigBuilder createBuilder() {
        return new LocalConfigBuilder();
    }

    @Deprecated
    public static LocalConfigBuilder createBuilder(final String ignored) {
        return new LocalConfigBuilder();
    }
}
