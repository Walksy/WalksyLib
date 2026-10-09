package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.option.Option;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class OptionPageBuilder extends OptionBuilder<String, OptionPageBuilder> {
    private final Map<String, List<Option<?>>> pages = new LinkedHashMap<>();

    public OptionPageBuilder(final String name, final Supplier<String> getter, final String defaultValue, final Consumer<String> setter) {
        super(name, getter, defaultValue, setter);
    }

    public OptionPageBuilder page(final String id, final Option<?>... options) {
        this.pages.put(id, List.of(options));
        return this;
    }

    @Override
    public Map<String, List<Option<?>>> pages() {
        return this.pages;
    }

    @Override
    public Class<String> valueType() {
        return String.class;
    }
}
