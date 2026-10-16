package main.walksy.lib.api.config;

import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.core.network.ServerOptOut;
import main.walksy.lib.core.network.ServerOptOutResponse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConfigBuilder {
    private final List<Category> categories = new ArrayList<>();
    private final Map<String, ServerOptOutResponse> serverFeatures = new LinkedHashMap<>();
    private Path path;
    private Runnable onSave;
    private boolean shared;
    private Path previousPath;
    private Path seedPath;
    private boolean updateChecks = true;
    private ServerOptOutResponse serverResponse;

    public ConfigBuilder() {
    }

    public ConfigBuilder path(final Path path) {
        this.path = path;
        return this;
    }

    public ConfigBuilder category(final Category category) {
        this.categories.add(category);
        return this;
    }

    public ConfigBuilder sharedFile() {
        this.shared = true;
        return this;
    }

    public ConfigBuilder movedFrom(final Path previousPath) {
        this.previousPath = previousPath;
        return this;
    }

    public ConfigBuilder seedFrom(final Path seedPath) {
        this.seedPath = seedPath;
        return this;
    }

    public ConfigBuilder onSave(final Runnable onSave) {
        this.onSave = onSave;
        return this;
    }

    public ConfigBuilder disableUpdateChecks() {
        this.updateChecks = false;
        return this;
    }

    @Deprecated
    public ConfigBuilder checkForUpdatesViaModrinth(final String modrinthPageUrl) {
        return this;
    }

    public ConfigBuilder allowOptOutFromServer(final ServerOptOutResponse response) {
        this.serverResponse = Objects.requireNonNull(response, "response");
        return this;
    }

    public ConfigBuilder allowOptOutFromServer(final String feature, final ServerOptOutResponse response) {
        ServerOptOut.checkFeatureId(feature);
        Objects.requireNonNull(response, "response");
        if (this.serverFeatures.putIfAbsent(feature, response) != null) {
            throw new IllegalArgumentException("Server feature \"" + feature + "\" is declared twice");
        }
        return this;
    }

    public Config build() {
        if (this.path == null) {
            throw new IllegalStateException("Config requires a .path()");
        }
        return new Config(this.path, this.categories, this.onSave, this.shared, this.previousPath, this.seedPath, this.updateChecks, new ServerOptOut(this.serverResponse, this.serverFeatures));
    }
}
