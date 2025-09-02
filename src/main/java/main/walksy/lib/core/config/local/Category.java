package main.walksy.lib.core.config.local;

import main.walksy.lib.core.config.local.builders.CategoryBuilder;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;

import java.util.List;
import java.util.Objects;

public record Category(String name, List<OptionGroup> optionGroups, List<Option<?>> options) {

    public static CategoryBuilder createBuilder(String name) {
        return new CategoryBuilder(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(this.name(), category.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
