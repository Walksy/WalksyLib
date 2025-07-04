package walksy.lib.core.config.impl;

import walksy.lib.core.config.impl.builders.CategoryBuilder;
import walksy.lib.core.config.impl.options.groups.OptionGroup;

import java.util.List;

public record Category(String name, List<OptionGroup> optionGroups, List<Option<?>> options) {

    public static CategoryBuilder createBuilder(String name) {
        return new CategoryBuilder(name);
    }
}
