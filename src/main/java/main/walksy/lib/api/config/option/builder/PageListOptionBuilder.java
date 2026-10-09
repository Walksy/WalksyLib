package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.option.EntryNameRule;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.value.ListEntries;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PageListOptionBuilder extends OptionBuilder<List<String>, PageListOptionBuilder> {
    private Function<String, List<Option<?>>> pageFactory;
    private String newEntryName = "Entry";
    private Consumer<String> onPageRemoved;
    private BiConsumer<String, String> onPageRenamed;
    private boolean namesOnly;
    private boolean readOnlyNames;
    private EntryNameRule nameRule = EntryNameRule.NONE;
    private ListEntries entries;
    private Function<String, List<OptionGroup>> pageGroupFactory;

    public PageListOptionBuilder(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        super(name, getter, defaultValue, setter);
    }

    public PageListOptionBuilder options(final Function<String, List<Option<?>>> factory) {
        this.pageFactory = Objects.requireNonNull(factory, "page factory");
        return this;
    }

    public PageListOptionBuilder groups(final Function<String, List<OptionGroup>> factory) {
        this.pageGroupFactory = Objects.requireNonNull(factory, "page group factory");
        this.pageFactory = id -> List.of();
        return this;
    }

    public PageListOptionBuilder newEntryName(final String stem) {
        this.newEntryName = stem == null || stem.isBlank() ? "Entry" : stem.trim();
        return this;
    }

    public PageListOptionBuilder onRemoved(final Consumer<String> onRemoved) {
        this.onPageRemoved = onRemoved;
        return this;
    }

    public PageListOptionBuilder onRenamed(final BiConsumer<String, String> onRenamed) {
        this.onPageRenamed = onRenamed;
        return this;
    }

    public PageListOptionBuilder nameRule(final EntryNameRule rule) {
        this.nameRule = rule == null ? EntryNameRule.NONE : this.nameRule.and(rule);
        return this;
    }

    public PageListOptionBuilder noSpaces() {
        return this.nameRule(EntryNameRule.NO_SPACES);
    }

    public PageListOptionBuilder namesOnly() {
        this.namesOnly = true;
        return this.options(name -> List.of());
    }

    public PageListOptionBuilder entries(final ListEntries entries) {
        this.entries = entries;
        return this;
    }

    public PageListOptionBuilder readOnlyNames() {
        this.readOnlyNames = true;
        return this;
    }

    @Override
    public boolean isNamesOnly() {
        return this.namesOnly;
    }

    @Override
    public boolean hasReadOnlyNames() {
        return this.readOnlyNames;
    }

    @Override
    public Function<String, List<OptionGroup>> pageGroupFactory() {
        return this.pageGroupFactory;
    }

    @Override
    public Function<String, List<Option<?>>> pageFactory() {
        return this.pageFactory;
    }

    @Override
    public String newEntryName() {
        return this.newEntryName;
    }

    @Override
    public Consumer<String> onPageRemoved() {
        return this.onPageRemoved;
    }

    @Override
    public BiConsumer<String, String> onPageRenamed() {
        return this.onPageRenamed;
    }

    @Override
    public ListEntries listEntries() {
        return this.entries;
    }

    @Override
    public EntryNameRule entryNameRule() {
        return this.nameRule;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<String>> valueType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
}
