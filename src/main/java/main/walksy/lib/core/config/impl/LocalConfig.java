package main.walksy.lib.core.config.impl;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.Config;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;

import java.nio.file.Path;
import java.util.List;

public class LocalConfig implements Config {

    private final String name;
    private final Path path;
    private final List<Category> categories;

    public LocalConfig(String name, Path path, List<Category> categories) {
        this.name = name;
        this.path = path;
        this.categories = categories;
    }

    public String getName() {
        return name;
    }

    @Override
    public Path path() {
        return this.path;
    }

    public List<Category> getCategories() {
        return categories;
    }

    @Override
    public void load() {

    }

    @Override
    public void save() {
        WalksyLib.getInstance().getConfigManager().save(this);
    }

    public static LocalConfigBuilder createBuilder(String name) {
        return new LocalConfigBuilder(name);
    }
}
