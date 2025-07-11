package main.walksy.lib.core.config;

import main.walksy.lib.core.config.impl.Category;
import main.walksy.lib.core.config.impl.builders.ConfigClassBuilder;

import java.nio.file.Path;
import java.util.List;

public class ConfigClass {

    private final String name;
    private final Path path;
    private final List<Category> categories;
    private final Runnable save;

    public ConfigClass(String name, Path path, List<Category> categories, Runnable save) {
        this.name = name;
        this.path = path;
        this.categories = categories;
        this.save = save;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void save() {
        if (save != null) {
            save.run();
        }
    }

    public static ConfigClassBuilder createBuilder(String name) {
        return new ConfigClassBuilder(name);
    }
}
