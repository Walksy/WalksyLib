package walksy.lib.core.config.impl.builders;

import walksy.lib.core.config.ConfigClass;
import walksy.lib.core.config.impl.Category;
import walksy.lib.core.config.impl.builders.CategoryBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigClassBuilder {

    private final String configName;
    private final List<Category> categories = new ArrayList<>();
    private Path path;
    private Runnable save;

    public ConfigClassBuilder(String configName) {
        this.configName = configName;
        //TODO Set default save func
    }

    public ConfigClassBuilder path(Path path) {
        this.path = path;
        return this;
    }

    public ConfigClassBuilder category(Category categoryBuilder) {
        categories.add(categoryBuilder);
        return this;
    }

    public ConfigClassBuilder save(Runnable save)
    {
        this.save = save;
        return this;
    }

    public ConfigClass build() {
        return new ConfigClass(configName, path, categories, save);
    }
}

