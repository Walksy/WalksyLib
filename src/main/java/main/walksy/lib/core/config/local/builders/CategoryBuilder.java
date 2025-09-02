package main.walksy.lib.core.config.local.builders;

import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;

import java.util.ArrayList;
import java.util.List;

public class CategoryBuilder {

    private final String name;
    private final List<OptionGroup> optionGroups = new ArrayList<>();
    private final List<Option<?>> options = new ArrayList<>();

    public CategoryBuilder(String name) {
        this.name = name;
    }

    public CategoryBuilder group(OptionGroup group)
    {
        optionGroups.add(group);
        return this;
    }

    public CategoryBuilder option(Option<?> option) {
        options.add(option);
        return this;
    }

    public Category build() {
        return new Category(name, optionGroups, options);
    }
}
