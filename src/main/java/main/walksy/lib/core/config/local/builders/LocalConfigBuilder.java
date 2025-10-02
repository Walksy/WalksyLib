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

    public LocalConfig build() {
        return new LocalConfig(configName, path, categories);
    }
}

