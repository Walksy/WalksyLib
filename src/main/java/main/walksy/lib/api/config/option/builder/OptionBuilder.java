package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.option.AvailabilityMode;
import main.walksy.lib.api.config.option.EntryNameRule;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Warning;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.api.hud.HudElements;
import main.walksy.lib.core.hud.HudSpace;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class OptionBuilder<T, SELF extends OptionBuilder<T, SELF>> {
    private final String name;
    private String saveKey;
    private String hudName;
    private HudElement hudElement;
    private HudSpace hudSpaceHandle;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T defaultValue;
    private OptionDescription description;
    private Supplier<Boolean> availability = () -> true;
    private AvailabilityMode availabilityMode = AvailabilityMode.GRAY_OUT;
    private Supplier<Boolean> visibility;
    private String availabilityHelper = "";
    private Runnable onChange;

    protected OptionBuilder(final String name, final Supplier<T> getter, final T defaultValue, final Consumer<T> setter) {
        this.name = Objects.requireNonNull(name, "option name");
        this.getter = Objects.requireNonNull(getter, "option getter");
        this.defaultValue = defaultValue;
        this.setter = setter;
    }

    public SELF description(final OptionDescription description) {
        this.description = description;
        return this.self();
    }

    public SELF description(final String text) {
        return this.description(text == null ? null : OptionDescription.ofText(() -> text));
    }

    public SELF onChange(final Runnable onChange) {
        this.onChange = onChange;
        return this.self();
    }

    public SELF availability(final Supplier<Boolean> condition, final String availabilityHelper) {
        return this.availability(condition, availabilityHelper, AvailabilityMode.GRAY_OUT);
    }

    public SELF availability(final Supplier<Boolean> condition, final AvailabilityMode mode) {
        return this.availability(condition, "", mode);
    }

    public SELF availability(final Supplier<Boolean> condition, final String availabilityHelper, final AvailabilityMode mode) {
        this.availability = condition;
        this.availabilityHelper = availabilityHelper;
        this.availabilityMode = mode == null ? AvailabilityMode.GRAY_OUT : mode;
        return this.self();
    }

    public SELF availabilityMode(final AvailabilityMode mode) {
        this.availabilityMode = mode == null ? AvailabilityMode.GRAY_OUT : mode;
        return this.self();
    }

    public SELF visibility(final Supplier<Boolean> condition) {
        this.visibility = condition;
        return this.self();
    }

    public Supplier<Boolean> visibility() {
        return this.visibility;
    }

    public final Option<T> build() {
        return new Option<>(this);
    }

    public abstract Class<T> valueType();

    public Warning warning() {
        return null;
    }

    public T min() {
        return null;
    }

    public T max() {
        return null;
    }

    public T increment() {
        return null;
    }

    public boolean alphaEditable() {
        return true;
    }

    public boolean chromaEditable() {
        return true;
    }

    public float cropAspect() {
        return 0f;
    }

    public ListEntries listEntries() {
        return null;
    }

    public boolean isReorderable() {
        return false;
    }

    public boolean allowsDuplicates() {
        return false;
    }

    public boolean isContainer() {
        return false;
    }

    public Map<String, List<Option<?>>> pages() {
        return null;
    }

    public Function<String, List<OptionGroup>> pageGroupFactory() {
        return null;
    }

    public Function<String, List<Option<?>>> pageFactory() {
        return null;
    }

    public String newEntryName() {
        return null;
    }

    public boolean isNamesOnly() {
        return false;
    }

    public boolean hasReadOnlyNames() {
        return false;
    }

    public Consumer<String> onPageRemoved() {
        return null;
    }

    public BiConsumer<String, String> onPageRenamed() {
        return null;
    }

    public EntryNameRule entryNameRule() {
        return EntryNameRule.NONE;
    }

    @SuppressWarnings("unchecked")
    public final SELF saveKey(final String saveKey) {
        this.saveKey = saveKey;
        return (SELF) this;
    }

    public final String saveKey() {
        return this.saveKey;
    }

    @SuppressWarnings("unchecked")
    public final SELF hudSpace(final String name) {
        final HudElement element = HudElements.forValue(this.valueType(), (Supplier<Object>) this.getter());
        if (element == null) {
            throw new IllegalArgumentException("No built-in HUD renderer for " + this.valueType().getSimpleName() + " — pass your own HudElement to hudSpace(name, element)");
        }
        return this.hudSpace(name, element);
    }

    public final SELF hudSpace(final String name, final BooleanSupplier visibleWhen) {
        this.hudSpace(name);
        this.hudElement = HudElements.when(visibleWhen, this.hudElement);
        return this.self();
    }

    @SuppressWarnings("unchecked")
    public final SELF hudSpace(final String name, final HudElement element) {
        this.hudName = name;
        this.hudElement = element;
        return (SELF) this;
    }

    public final String hudName() {
        return this.hudName;
    }

    public final HudElement hudElement() {
        return this.hudElement;
    }

    @SuppressWarnings("unchecked")
    public final SELF hudSpace(final HudSpace space) {
        this.hudSpaceHandle = space;
        this.hudName = space.name();
        this.hudElement = space.element();
        return (SELF) this;
    }

    public final HudSpace hudSpace() {
        return this.hudSpaceHandle;
    }

    public final String name() {
        return this.name;
    }

    public final Supplier<T> getter() {
        return this.getter;
    }

    public final Consumer<T> setter() {
        return this.setter;
    }

    public final T defaultValue() {
        return this.defaultValue;
    }

    public final OptionDescription description() {
        return this.description;
    }

    public final Supplier<Boolean> availability() {
        return this.availability;
    }

    public final AvailabilityMode availabilityMode() {
        return this.availabilityMode;
    }

    public final String availabilityHelper() {
        return this.availabilityHelper;
    }

    public final Runnable onChange() {
        return this.onChange;
    }

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }
}
