package main.walksy.lib.api.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.config.serialization.ConfigSerializer;
import main.walksy.lib.core.config.serialization.JsonFiles;
import main.walksy.lib.core.config.serialization.LegacyConfigMigrator;
import main.walksy.lib.core.network.ServerOptOut;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

public final class Config {
    private final Path path;
    private final List<Category> categories;
    private final Runnable saveListener;
    private final boolean shared;
    private final Path previousPath;
    private final Path seedPath;
    private final boolean updateChecks;
    private final ServerOptOut serverOptOut;
    private JsonObject notifiedState;

    public Config(final Path path, final List<Category> categories, final Runnable saveListener, final boolean shared, final Path previousPath, final Path seedPath, final boolean updateChecks, final ServerOptOut serverOptOut) {
        this.path = path;
        this.categories = List.copyOf(categories);
        this.saveListener = saveListener;
        this.shared = shared;
        this.previousPath = previousPath;
        this.seedPath = seedPath;
        this.updateChecks = updateChecks;
        this.serverOptOut = serverOptOut;
    }

    public boolean checksForUpdates() {
        return this.updateChecks;
    }

    public ServerOptOut serverOptOut() {
        return this.serverOptOut;
    }

    public boolean isDisabledByServer() {
        return this.serverOptOut.isModDisabled();
    }

    public boolean isDisabledByServer(final String feature) {
        return this.serverOptOut.isFeatureDisabled(feature);
    }

    public String serverDisableReason() {
        return this.serverOptOut.modReason();
    }

    public String serverDisableReason(final String feature) {
        return this.serverOptOut.featureReason(feature);
    }

    public static ConfigBuilder builder() {
        return new ConfigBuilder();
    }

    public Path path() {
        return this.path;
    }

    public List<Category> categories() {
        return this.categories;
    }

    public Stream<Option<?>> options() {
        return this.categories.stream().flatMap(Category::allOptions);
    }

    public void load() {
        this.loadFromDisk();
        this.notifiedState = this.serialize();
    }

    private void loadFromDisk() {
        this.importPrevious();
        this.importSeed();
        if (!Files.exists(this.path)) {
            this.save();
            return;
        }
        final JsonElement root;
        try {
            root = JsonParser.parseString(Files.readString(this.path));
        } catch (final JsonParseException e) {
            WalksyLib.getLogger().err("Failed to parse config from " + this.path + ": " + e.getMessage());
            this.backupTo(".corrupt.bak");
            return;
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to read config from " + this.path + ": " + e.getMessage());
            return;
        }
        if (LegacyConfigMigrator.isLegacy(root)) {
            WalksyLib.getLogger().err("Migrating legacy config format: " + this.path);
            this.backupTo(".legacy.bak");
            ConfigSerializer.apply(this, LegacyConfigMigrator.migrate(root.getAsJsonArray()));
            this.save();
        } else if (root.isJsonObject()) {
            ConfigSerializer.apply(this, root.getAsJsonObject());
        } else {
            WalksyLib.getLogger().err("Unrecognized config format in " + this.path + ", keeping defaults");
            this.backupTo(".corrupt.bak");
        }
    }

    public void saveAndNotify() {
        final JsonObject serialized = this.serialize();
        if (serialized == null) {
            return;
        }
        this.write(serialized);
        if (serialized.equals(this.notifiedState)) {
            return;
        }
        this.notifiedState = serialized;
        if (this.saveListener != null) {
            this.saveListener.run();
        }
    }

    public void save() {
        final JsonObject serialized = this.serialize();
        if (serialized != null) {
            this.write(serialized);
        }
    }

    private JsonObject serialize() {
        try {
            return ConfigSerializer.serialize(this);
        } catch (final RuntimeException e) {
            WalksyLib.getLogger().err("Failed to serialize config for " + this.path + ": " + e.getMessage());
            return null;
        }
    }

    private void write(final JsonObject serialized) {
        JsonFiles.write(this.path, this.shared ? mergeIntoFile(serialized.deepCopy()) : serialized, "config " + this.path.getFileName());
    }

    private JsonObject mergeIntoFile(final JsonObject serialized) {
        final JsonObject existing = JsonFiles.read(this.path);
        if (existing == null) {
            return serialized;
        }
        final JsonElement existingCategories = existing.get("categories");
        if (existingCategories == null || !existingCategories.isJsonObject()) {
            existing.add("categories", serialized.get("categories"));
        } else {
            final JsonObject target = existingCategories.getAsJsonObject();
            final JsonElement mine = serialized.get("categories");
            if (mine != null && mine.isJsonObject()) {
                for (final var entry : mine.getAsJsonObject().entrySet()) {
                    target.add(entry.getKey(), entry.getValue());
                }
            }
        }
        existing.add("format", serialized.get("format"));
        return existing;
    }

    private void importSeed() {
        if (this.seedPath == null || this.seedPath.equals(this.path) || !Files.exists(this.seedPath)) {
            return;
        }
        if (Files.exists(this.path)) {
            final JsonObject existing = JsonFiles.read(this.path);
            if (existing == null || this.hasOwnCategories(existing)) {
                return;
            }
        }
        final JsonObject seed = JsonFiles.read(this.seedPath);
        if (seed != null) {
            ConfigSerializer.apply(this, seed);
            this.save();
        }
    }

    private boolean hasOwnCategories(final JsonObject existing) {
        final JsonElement saved = existing.get("categories");
        if (saved == null || !saved.isJsonObject()) {
            return false;
        }
        for (final Category category : this.categories) {
            if (saved.getAsJsonObject().has(category.name())) {
                return true;
            }
        }
        return false;
    }

    private void importPrevious() {
        if (this.previousPath == null || !Files.exists(this.previousPath)) {
            return;
        }
        final JsonObject root = JsonFiles.read(this.previousPath);
        if (root != null) {
            WalksyLib.getLogger().err("Moving config " + this.previousPath.getFileName() + " into " + this.path.getFileName());
            ConfigSerializer.apply(this, root);
            this.save();
        }
        try {
            Files.move(this.previousPath, this.previousPath.resolveSibling(this.previousPath.getFileName() + ".moved.bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to retire " + this.previousPath + ": " + e.getMessage());
        }
    }

    private void backupTo(final String suffix) {
        final Path backup = this.path.resolveSibling(this.path.getFileName() + suffix);
        try {
            Files.copy(this.path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to back up config to " + backup + ": " + e.getMessage());
        }
    }
}
