package main.walksy.lib.core.config.local.builders;

import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalConfigBuilder {

    private final String configName;
    private final List<Category> categories = new ArrayList<>();
    private Path path;
    private Runnable onSave;

    public LocalConfigBuilder(String configName) {
        this.configName = configName;
    }

    public LocalConfigBuilder path(Path path) {
        this.path = path;
        return this;
    }

    public LocalConfigBuilder category(Category categoryBuilder) {
        categories.add(categoryBuilder);
        return this;
    }

    public LocalConfigBuilder onSave(Runnable onSave) {
        this.onSave = onSave;
        return this;
    }

    public LocalConfig build() {
        if (path == null) {
            throw new IllegalStateException("""
                [WalksyLib] LocalConfigBuilder error: Missing required .path()!
                You must call .path(Path) before build().
                Example:
                    new LocalConfigBuilder("example")
                        .path(FabricLoader.getInstance().getConfigDir().resolve("example.json"))
                        .build();
                """);
        }

        return new LocalConfig(configName, path, categories, onSave);
    }
}

