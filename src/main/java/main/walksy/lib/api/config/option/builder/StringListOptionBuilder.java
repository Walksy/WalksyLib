package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.option.EntryNameRule;
import main.walksy.lib.api.config.value.ListEntries;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StringListOptionBuilder extends OptionBuilder<List<String>, StringListOptionBuilder> {
    private ListEntries listEntries;
    private boolean reorderable;
    private boolean allowDuplicates;
    private EntryNameRule nameRule = EntryNameRule.NONE;

    public StringListOptionBuilder(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        super(name, getter, defaultValue, setter);
    }

    public StringListOptionBuilder entries(final ListEntries entries) {
        this.listEntries = entries;
        return this;
    }

    public StringListOptionBuilder reorderable() {
        this.reorderable = true;
        return this;
    }

    public StringListOptionBuilder allowDuplicates() {
        this.allowDuplicates = true;
        return this;
    }

    public StringListOptionBuilder nameRule(final EntryNameRule rule) {
        this.nameRule = rule == null ? EntryNameRule.NONE : this.nameRule.and(rule);
        return this;
    }

    public StringListOptionBuilder noSpaces() {
        return this.nameRule(EntryNameRule.NO_SPACES);
    }

    @Override
    public EntryNameRule entryNameRule() {
        return this.nameRule;
    }

    @Override
    public ListEntries listEntries() {
        return this.listEntries;
    }

    @Override
    public boolean isReorderable() {
        return this.reorderable;
    }

    @Override
    public boolean allowsDuplicates() {
        return this.allowDuplicates;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<String>> valueType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
}
