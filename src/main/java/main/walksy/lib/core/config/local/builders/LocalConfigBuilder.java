package main.walksy.lib.core.config.local.builders;

import main.walksy.lib.core.config.impl.ModConfig;
import main.walksy.lib.core.config.local.Category;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalConfigBuilder {

    private final List<Category> categories = new ArrayList<>();
    private Path path;
    private Runnable onSave;

    public LocalConfigBuilder path(final Path path) {
        this.path = path;
        return this;
    }

    public LocalConfigBuilder category(final Category categoryBuilder) {
        this.categories.add(categoryBuilder);
        return this;
    }

    public LocalConfigBuilder onSave(final Runnable onSave) {
        this.onSave = onSave;
        return this;
    }

    public ModConfig build() {
        if (this.path == null) {
            throw new IllegalStateException("Missing required .path()");
        }

        return new ModConfig(this.path, this.categories, this.onSave);
    }
}
