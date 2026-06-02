package main.walksy.lib.core.config.local;

import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.*;
import main.walksy.lib.core.utils.IdentifierWrapper;
import main.walksy.lib.core.utils.SearchUtils;
import main.walksy.lib.core.utils.log.InternalLog;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Option<T> {
    private final String name;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Class<T> type;
    private final T min;
    private final T max;
    private final T increment;
    private OptionDescription description;
    private final T defaultValue;
    private final Supplier<Boolean> availability;
    private final String availabilityHelper;
    private T prevValue;
    private final Runnable onChange;
    public T screenInstanceValue = null;
    private final BooleanOption.Warning warning;

    private String searchQ = "";

    public Option(final String name, final OptionDescription description, final Supplier<T> getter, final Consumer<T> setter, final Supplier<Boolean> availability, final String availabilityHelper, final Class<T> type, final T defaultValue, final BooleanOption.Warning warning, final Runnable onChange) {
        this(name, description, getter, setter, availability, availabilityHelper, type, null, null, null, defaultValue, warning, onChange);
    }

    public Option(final String name, final OptionDescription description, final Supplier<T> getter, final Consumer<T> setter, final Supplier<Boolean> availability, final String availabilityHelper, final Class<T> type, final T defaultValue, final Runnable onChange) {
        this(name, description, getter, setter, availability, availabilityHelper, type, null, null, null, defaultValue, null, onChange);
    }

    public Option(final String name, final OptionDescription description, final Supplier<T> getter, final Consumer<T> setter,
                  final Supplier<Boolean> availability, final String availabilityHelper, final Class<T> type, final T min, final T max, final T increment, final T defaultValue,
                  final BooleanOption.Warning warning, final Runnable onChange) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.availability = availability;
        this.availabilityHelper = availabilityHelper;
        this.type = type;
        this.min = min;
        this.max = max;
        this.increment = increment;
        if (getter.get() instanceof PixelGridAnimation) {
            this.defaultValue = (T) ((PixelGridAnimation) defaultValue).copy();
        } else if (getter.get() instanceof WalksyLibColor) {
            this.defaultValue = (T) ((WalksyLibColor) defaultValue).copy();
        } else {
            this.defaultValue = defaultValue;
        }
        this.prevValue = null;
        this.onChange = onChange;
        this.warning = warning;
    }


    public String getName() {
        return this.name;
    }

    private Supplier<T> getGetter() {
        return this.getter;
    }
    private Consumer<T> getSetter() {
        return this.setter;
    }

    public Class<T> getType() {
        return this.type;
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

    public OptionDescription getDescription() {
        return this.description;
    }

    public String getAvailabilityHelper() {
        return this.availabilityHelper;
    }

    public T getValue() {
        return this.getter.get();
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public boolean screenInstanceCheck() {
        return Objects.equals(this.screenInstanceValue, this.getValue());
    }

    public void setScreenInstance() {
        if (this.getValue() instanceof PixelGridAnimation animation) {
            this.screenInstanceValue = (T) animation.copy();
        } else if (this.getValue() instanceof WalksyLibColor color) {
            this.screenInstanceValue = (T) color.copy();
        } else {
            this.screenInstanceValue = this.getter.get();
        }
    }

    public void undo() {
        if (this.setter == null) return;
        this.setter.accept(this.screenInstanceValue);
    }

    @SuppressWarnings("unchecked")
    public void setValue(final Object value) {
        if (this.type.isInstance(value)) {
            if (this.setter != null) {
                if (!this.getter.get().equals(value)) {
                    this.runChange();
                }
                this.setter.accept((T) value);
            }
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
        }
    }

    public boolean canReset() {
        return this.getValue() != this.defaultValue;
    }

    public boolean isAvailable() {
        return this.availability.get();
    }

    public void setPrev(final String config) {
        if (Objects.equals(this.getValue(), this.screenInstanceValue)) {
            return;
        }
        this.prevValue = this.screenInstanceValue;
        final Object oldVal = this.prevValue;
        final Object newVal = this.getValue();

        if (oldVal instanceof WalksyLibColor oldColor && newVal instanceof WalksyLibColor newColor) {
            if (oldColor.isRainbow() != newColor.isRainbow()) {
                this.logField(config, this.getName() + "'s Rainbow", oldColor.isRainbow(), newColor.isRainbow());
            }
            if (oldColor.getRainbowSpeed() != newColor.getRainbowSpeed()) {
                this.logField(config, this.getName() + "'s Rainbow Speed", oldColor.getRainbowSpeed(), newColor.getRainbowSpeed());
            }
            if (oldColor.isPulse() != newColor.isPulse()) {
                this.logField(config, this.getName() + "'s Pulse", oldColor.isPulse(), newColor.isPulse());
            }
            if (oldColor.getPulseSpeed() != newColor.getPulseSpeed()) {
                this.logField(config, this.getName() + "'s Pulse Speed", oldColor.getPulseSpeed(), newColor.getPulseSpeed());
            }
            if (Float.compare(oldColor.getSaturation(), newColor.getSaturation()) != 0) {
                this.logField(config, this.getName() + "'s Saturation", oldColor.getSaturation(), newColor.getSaturation());
            }
            if (Float.compare(oldColor.getBrightness(), newColor.getBrightness()) != 0) {
                this.logField(config, this.getName() + "'s Brightness", oldColor.getBrightness(), newColor.getBrightness());
            }
        } else if (oldVal instanceof PixelGridAnimation oldAnim && newVal instanceof PixelGridAnimation newAnim) {
            if (oldAnim.getAnimationSpeed() != newAnim.getAnimationSpeed()) {
                this.logField(config, this.getName() + "'s Speed", oldAnim.getAnimationSpeed(), newAnim.getAnimationSpeed());
            }
            if (Double.compare(oldAnim.getOffsetX(), newAnim.getOffsetX()) != 0) {
                this.logField(config, this.getName() + "'s X Pos", oldAnim.getOffsetX(), newAnim.getOffsetX());
            }
            if (Double.compare(oldAnim.getOffsetY(), newAnim.getOffsetY()) != 0) {
                this.logField(config, this.getName() + "'s Y Pos", oldAnim.getOffsetY(), newAnim.getOffsetY());
            }
        } else {
            this.logField(config, this.getName(), oldVal, newVal);
        }
    }

    private <V> void logField(final String configName, final String name, final V oldVal, final V newVal) {
        InternalLog.ToolTip toolTip = null;
        if (this.warning != null) {
            toolTip = new InternalLog.ToolTip(Tooltip.create(Component.literal("Option has warning: " + this.warning.message())), Color.RED.getRGB());
        }
        final String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        WalksyLibLogger.log(InternalLog.of("[" + time + "]: " + "[" + configName + "] " + "-> " + "[" + name + "], " + "[" + oldVal + "] to " + "[" + newVal + "]", toolTip));
    }


    public void reset() {
        if (this.getType() == Runnable.class) return;
        if (this.getValue() instanceof PixelGridAnimation) {
            this.setter.accept((T) ((PixelGridAnimation) this.defaultValue).copy());
        } else if (this.getValue() instanceof WalksyLibColor c) {
            this.setter.accept((T) ((WalksyLibColor) this.defaultValue).copy());
        } else {
            this.setter.accept(this.defaultValue);
        }
    }

    public Option<T> description(final OptionDescription description) {
        this.description = description;
        return this;
    }

    public boolean hasChanged() {
        final T value = this.getValue();
        final T defaultValue = this.getDefaultValue();

        if (value == null || defaultValue == null) {
            return value != defaultValue;
        }

        return !Objects.equals(value, defaultValue);
    }

    public T getPrevValue() {
        return this.prevValue;
    }

    public boolean searched() {
        if (this.searchQ.isEmpty()) return true;

        final String[] queryWords = this.searchQ.toLowerCase().trim().split("\\s+");
        final String[] nameWords = this.getName().toLowerCase().trim().split("\\s+");

        outer:
        for (final String qWord : queryWords) {
            for (final String numWord : nameWords) {
                if (numWord.contains(qWord) || numWord.startsWith(qWord)) {
                    continue outer;
                }
                if (SearchUtils.levenshteinDistance(numWord, qWord) <= 2) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    public void updateSearchQ(final String searchQ) {
        this.searchQ = searchQ;
    }

    public void runChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    @SuppressWarnings("unchecked")
    public OptionWidget createWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height) {
        if (this.type == Boolean.class) {
            return new BooleanWidget(parent, screen, x, y, width, height, (Option<Boolean>) this, this.warning);
        } else if (this.type == Integer.class) {
            return new NumericalWidget<Integer>(parent, screen, x, y, width, height, (Option<Integer>) this);
        } else if (this.type == Double.class) {
            return new NumericalWidget<Double>(parent, screen, x, y, width, height, (Option<Double>) this);
        } else if (this.type == Float.class) {
            return new NumericalWidget<Float>(parent, screen, x, y, width, height, (Option<Float>) this);
        } else if (this.type == Color.class || this.type == WalksyLibColor.class) {
            return new ColorWidget(parent, screen, x, y, width, height, (Option<WalksyLibColor>) this);
        } else if (this.type == PixelGridAnimation.class) {
            return new PixelGridAnimationWidget(parent, screen, x, y, width, height, (Option<PixelGridAnimation>) this);
        } else if (this.type == List.class) {
            return new StringListOptionWidget(parent, screen, x, y, width, height, (Option<List<String>>) this);
        } else if (this.type == Runnable.class) {
            return new ButtonOptionWidget(parent, screen, x, y, width, height, (Option<Runnable>) this);
        } else if (this.type == IdentifierWrapper.class) {
            return new SpriteOptionWidget(parent, screen, x, y, width, height, (Option<IdentifierWrapper>) this);
        } else if (this.type == String.class) {
            return new StringOptionWidget(parent, screen, x, y, width, height, (Option<String>) this);
        } else if (Enum.class.isAssignableFrom(this.type)) {
            final Option<? extends Enum<?>> enumOption = (Option<? extends Enum<?>>) this;
            return new EnumOptionWidget(parent, screen, x, y, width, height, enumOption);
        } else {
            throw new UnsupportedOperationException("Unsupported option type: " + this.type);
        }
    }
}
