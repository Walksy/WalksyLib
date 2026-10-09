package main.walksy.lib.api.config.option;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.builder.OptionBuilder;
import main.walksy.lib.api.config.value.CopyableValue;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.core.hud.HudSpace;
import main.walksy.lib.core.utils.SearchUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Option<T> {
    private final String name;
    private final String saveKey;
    private final String hudName;
    private final HudElement hudElement;
    private HudSpace hudSpace;
    private final Class<T> type;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T defaultValue;
    private final OptionDescription description;
    private final Supplier<Boolean> availability;
    private final AvailabilityMode availabilityMode;
    private final Supplier<Boolean> visibility;
    private final String availabilityHelper;
    private final Runnable onChange;
    private final Warning warning;
    private final T min;
    private final T max;
    private final T increment;
    private final boolean alphaEditable;
    private final boolean chromaEditable;
    private final float cropAspect;
    private final ListEntries listEntries;
    private final boolean reorderable;
    private final boolean allowDuplicates;
    private final boolean container;
    private final Map<String, List<Option<?>>> pages;
    private final Function<String, List<Option<?>>> pageFactory;
    private final String newEntryName;
    private final boolean namesOnly;
    private final boolean readOnlyNames;
    private final Consumer<String> onPageRemoved;
    private final BiConsumer<String, String> onPageRenamed;
    private final EntryNameRule entryNameRule;
    private final Map<String, List<Option<?>>> dynamicPages = new LinkedHashMap<>();
    private final Map<String, List<OptionGroup>> dynamicGroups = new LinkedHashMap<>();
    public T screenInstanceValue;
    private T prevValue;
    private String searchQuery = "";
    private String sourceName;
    private boolean sourceIsLibrary;
    private String activePageId;

    public Option(final OptionBuilder<T, ?> builder) {
        this.name = builder.name();
        this.saveKey = builder.saveKey();
        this.hudName = builder.hudName();
        this.hudElement = builder.hudElement();
        this.hudSpace = builder.hudSpace();
        this.type = builder.valueType();
        this.getter = builder.getter();
        this.setter = builder.setter();
        this.defaultValue = copyOf(builder.defaultValue());
        this.description = builder.description();
        this.availability = builder.availability();
        this.availabilityMode = builder.availabilityMode();
        this.visibility = builder.visibility();
        this.availabilityHelper = builder.availabilityHelper();
        this.onChange = builder.onChange();
        this.warning = builder.warning();
        this.min = builder.min();
        this.max = builder.max();
        this.increment = builder.increment();
        this.alphaEditable = builder.alphaEditable();
        this.chromaEditable = builder.chromaEditable();
        this.cropAspect = builder.cropAspect();
        this.listEntries = builder.listEntries();
        this.reorderable = builder.isReorderable();
        this.allowDuplicates = builder.allowsDuplicates();
        this.container = builder.isContainer();
        final Map<String, List<Option<?>>> pages = builder.pages();
        this.pages = pages == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(pages));
        final Function<String, List<OptionGroup>> groupFactory = builder.pageGroupFactory();
        this.pageFactory = groupFactory == null ? builder.pageFactory() : id -> this.buildGroupedPage(id, groupFactory);
        this.newEntryName = builder.newEntryName();
        this.namesOnly = builder.isNamesOnly();
        this.readOnlyNames = builder.hasReadOnlyNames();
        this.onPageRemoved = builder.onPageRemoved();
        this.onPageRenamed = builder.onPageRenamed();
        final EntryNameRule rule = builder.entryNameRule();
        this.entryNameRule = rule == null ? EntryNameRule.NONE : rule;
    }

    public String getName() {
        return this.name;
    }

    public String getSaveKey() {
        return this.saveKey == null ? this.name : this.saveKey;
    }

    public HudElement getHudElement() {
        return this.hudElement;
    }

    public String getHudName() {
        return this.hudName == null ? this.name : this.hudName;
    }

    public HudSpace getHudSpace() {
        return this.hudSpace;
    }

    public void bindHudSpace(final HudSpace space) {
        this.hudSpace = space;
    }

    public Class<T> getType() {
        return this.type;
    }

    public T getValue() {
        return this.getter.get();
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public OptionDescription getDescription() {
        return this.description;
    }

    public Warning getWarning() {
        return this.warning;
    }

    public T getMin() {
        return this.min;
    }

    public T getMax() {
        return this.max;
    }

    public T getIncrement() {
        return this.increment;
    }

    public boolean isAlphaEditable() {
        return this.alphaEditable;
    }

    public boolean isChromaEditable() {
        return this.chromaEditable;
    }

    public float getCropAspect() {
        return this.cropAspect;
    }

    public ListEntries getListEntries() {
        return this.listEntries;
    }

    public boolean isReorderable() {
        return this.reorderable;
    }

    public boolean allowsDuplicates() {
        return this.allowDuplicates;
    }

    public boolean isContainer() {
        return this.container;
    }

    public boolean isPageSelector() {
        return this.pages != null;
    }

    public boolean isPageList() {
        return this.pageFactory != null;
    }

    public boolean hasPages() {
        return this.pages != null || this.pageFactory != null;
    }

    public Map<String, List<Option<?>>> getPages() {
        if (this.pageFactory != null) {
            return this.syncPages();
        }
        return this.pages;
    }

    private Map<String, List<Option<?>>> syncPages() {
        final Object value = this.getValue();
        final List<?> ids = value instanceof List<?> list ? list : List.of();
        final Map<String, List<Option<?>>> synced = new LinkedHashMap<>();
        for (final Object raw : ids) {
            if (!(raw instanceof String id) || id.isEmpty() || synced.containsKey(id)) {
                continue;
            }
            List<Option<?>> page = this.dynamicPages.get(id);
            if (page == null) {
                final List<Option<?>> built = this.pageFactory.apply(id);
                page = built == null ? List.of() : List.copyOf(built);
                for (final Option<?> nested : page) {
                    nested.stampSource(this.sourceName, this.sourceIsLibrary);
                }
            }
            synced.put(id, page);
        }
        this.dynamicPages.clear();
        this.dynamicPages.putAll(synced);
        this.dynamicGroups.keySet().retainAll(synced.keySet());
        return Collections.unmodifiableMap(new LinkedHashMap<>(synced));
    }

    private List<Option<?>> buildGroupedPage(final String id, final Function<String, List<OptionGroup>> groupFactory) {
        final List<OptionGroup> groups = groupFactory.apply(id);
        final List<OptionGroup> kept = groups == null ? List.of() : List.copyOf(groups);
        this.dynamicGroups.put(id, kept);
        final List<Option<?>> options = new ArrayList<>();
        for (final OptionGroup group : kept) {
            options.addAll(group.getOptions());
        }
        return options;
    }

    public List<OptionGroup> getActivePageGroups() {
        this.getPages();
        final List<OptionGroup> groups = this.activePageId == null ? null : this.dynamicGroups.get(this.activePageId);
        return groups == null ? List.of() : groups;
    }

    public List<Option<?>> getActivePageOptions() {
        final Map<String, List<Option<?>>> pages = this.getPages();
        if (pages == null) {
            return List.of();
        }
        final String id = this.pageFactory != null ? this.activePageId : (String) this.getValue();
        final List<Option<?>> options = id == null ? null : pages.get(id);
        return options == null ? List.of() : options;
    }

    public String pageLabel(final String id) {
        if (id == null || this.listEntries == null) {
            return id;
        }
        return this.listEntries.label(id);
    }

    public String getActivePageId() {
        return this.activePageId;
    }

    public void setActivePageId(final String id) {
        this.activePageId = id;
    }

    public String getNewEntryName() {
        return this.newEntryName;
    }

    public EntryNameRule getEntryNameRule() {
        return this.entryNameRule;
    }

    public String cleanEntryName(final String candidate) {
        if (candidate == null) {
            return "";
        }
        final String cleaned = this.entryNameRule.clean(candidate);
        return cleaned == null ? "" : cleaned;
    }

    public boolean isNamesOnly() {
        return this.namesOnly;
    }

    public boolean hasReadOnlyNames() {
        return this.readOnlyNames;
    }

    public boolean isTeamList() {
        return this.readOnlyNames && this.pageFactory != null;
    }

    public void renamePage(final String from, final String to) {
        if (this.pageFactory == null || from == null || to == null || from.equals(to)) {
            return;
        }
        this.dynamicPages.remove(from);
        this.dynamicGroups.remove(from);
        if (from.equals(this.activePageId)) {
            this.activePageId = to;
        }
        if (this.onPageRenamed != null) {
            this.onPageRenamed.accept(from, to);
        }
    }

    public void removePage(final String id) {
        if (this.pageFactory == null || id == null) {
            return;
        }
        this.dynamicPages.remove(id);
        this.dynamicGroups.remove(id);
        if (id.equals(this.activePageId)) {
            this.activePageId = null;
        }
        if (this.onPageRemoved != null) {
            this.onPageRemoved.accept(id);
        }
    }

    public Stream<Option<?>> allNested() {
        final Map<String, List<Option<?>>> pages = this.getPages();
        if (pages == null) {
            return Stream.of(this);
        }
        return Stream.concat(Stream.of(this), pages.values().stream().flatMap(List::stream).flatMap(Option::allNested));
    }

    public boolean isAvailable() {
        return this.availability.get();
    }

    public AvailabilityMode getAvailabilityMode() {
        return this.availabilityMode;
    }

    public boolean isVisible() {
        return this.visibility == null || Boolean.TRUE.equals(this.visibility.get());
    }

    public boolean isShown() {
        if (!this.isVisible()) {
            return false;
        }
        return this.availabilityMode != AvailabilityMode.HIDE || this.isAvailable();
    }

    public boolean isHiddenByAvailability() {
        return this.isVisible() && this.availabilityMode == AvailabilityMode.HIDE && !this.isAvailable();
    }

    public boolean canHide() {
        return this.visibility != null || this.availabilityMode == AvailabilityMode.HIDE;
    }

    public String getAvailabilityHelper() {
        return this.availabilityHelper;
    }

    @SuppressWarnings("unchecked")
    public void setValue(final Object value) {
        if (!this.type.isInstance(value)) {
            throw new IllegalArgumentException("Invalid value type for option '" + this.name + "': expected " + this.type.getName() + " but got " + (value == null ? "null" : value.getClass().getName()));
        }
        if (this.setter == null) {
            return;
        }
        final T previous = copyOf(this.getValue());
        if (!Objects.equals(this.getValue(), value)) {
            this.runChange();
        }
        this.setter.accept((T) value);
        WalksyLib.getInstance().getOptionChangeLogger().logChange(this, previous, this.getValue());
    }

    public void reset() {
        if (this.setter == null) {
            return;
        }
        final T previous = copyOf(this.getValue());
        this.setter.accept(copyOf(this.defaultValue));
        WalksyLib.getInstance().getOptionChangeLogger().logChange(this, previous, this.getValue());
    }

    public boolean canReset() {
        return this.setter != null;
    }

    public boolean hasChanged() {
        return !Objects.equals(this.getValue(), this.defaultValue);
    }

    public void runChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    public void setScreenInstance() {
        this.screenInstanceValue = copyOf(this.getValue());
    }

    public boolean screenInstanceCheck() {
        return Objects.equals(this.screenInstanceValue, this.getValue());
    }

    public void undo() {
        if (this.setter == null) {
            return;
        }
        this.setter.accept(this.screenInstanceValue);
    }

    public void stampSource(final String name, final boolean library) {
        this.sourceName = name;
        this.sourceIsLibrary = library;
        for (final List<Option<?>> page : this.dynamicPages.values()) {
            for (final Option<?> nested : page) {
                nested.stampSource(name, library);
            }
        }
    }

    public String sourceName() {
        return this.sourceName == null ? "Options" : this.sourceName;
    }

    public boolean sourceIsLibrary() {
        return this.sourceIsLibrary;
    }

    public T getPrevValue() {
        return this.prevValue;
    }

    public void updateSearchQ(final String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public boolean searched() {
        if (this.searchQuery.isEmpty()) {
            return true;
        }
        final String[] queryWords = this.searchQuery.toLowerCase().trim().split("\\s+");
        final String[] nameWords = this.name.toLowerCase().trim().split("\\s+");
        outer:
        for (final String queryWord : queryWords) {
            for (final String nameWord : nameWords) {
                if (nameWord.contains(queryWord) || nameWord.startsWith(queryWord)) {
                    continue outer;
                }
                if (SearchUtils.levenshteinDistance(nameWord, queryWord) <= 2) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <V> V copyOf(final V value) {
        if (value instanceof CopyableValue<?> copyable) {
            return (V) copyable.copy();
        }
        return value;
    }
}
