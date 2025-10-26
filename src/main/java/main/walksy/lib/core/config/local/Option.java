package main.walksy.lib.core.config.local;

import main.walksy.lib.core.config.impl.LocalConfig;
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
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

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
    private T prevValue;
    private final Runnable onChange;

    //Screen
    public T screenInstanceValue = null;

    //Boolean Option
    private final BooleanOption.Warning warning;

    private String searchQ = "";

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Supplier<Boolean> availability, Class<T> type, T defaultValue, BooleanOption.Warning warning, Runnable onChange) {
        this(name, description, getter, setter, availability, type, null, null, null, defaultValue, warning, onChange);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Supplier<Boolean> availability, Class<T> type, T defaultValue, Runnable onChange) {
        this(name, description, getter, setter, availability, type, null, null, null, defaultValue, null, onChange);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter,
                  Supplier<Boolean> availability, Class<T> type, T min, T max, T increment, T defaultValue,
                  BooleanOption.Warning warning, Runnable onChange) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.availability = availability;
        this.type = type;
        this.min = min;
        this.max = max;
        this.increment = increment;
        if (getter.get() instanceof PixelGridAnimation) {
            this.defaultValue = (T) ((PixelGridAnimation)defaultValue).copy();
        } else if (getter.get() instanceof WalksyLibColor) {
            this.defaultValue = (T) ((WalksyLibColor)defaultValue).copy();
        } else {
            this.defaultValue = defaultValue;
        }
        this.prevValue = null;
        this.onChange = onChange;
        this.warning = warning;
    }


    public String getName() { return name; }
    private Supplier<T> getGetter() { return getter; }
    private Consumer<T> getSetter() { return setter; }
    public Class<T> getType() { return type; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public T getIncrement() { return increment; }

    public OptionDescription getDescription() { return description; }

    public T getValue()
    {
        return getter.get();
    }

    public T getDefaultValue()
    {
        return defaultValue;
    }

    public boolean screenInstanceCheck()
    {
        return Objects.equals(this.screenInstanceValue, this.getValue());
    }

    public void setScreenInstance()
    {
        if (this.getValue() instanceof PixelGridAnimation animation) {
            this.screenInstanceValue = (T) animation.copy();
        } else if (this.getValue() instanceof WalksyLibColor color) {
            this.screenInstanceValue = (T) color.copy();
        } else {
            this.screenInstanceValue = this.getter.get();
        }
    }



    public void undo()
    {
        if (this.setter == null) return;
        this.setter.accept(this.screenInstanceValue);
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if (type.isInstance(value)) {
            if (setter != null) {
                if (!this.getter.get().equals(value)) {
                    this.runChange();
                }
                setter.accept((T) value);
            }
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
        }
    }

    public boolean canReset() {
        return this.getValue() != this.defaultValue;
    }

    public boolean isAvailable()
    {
        return this.availability.get();
    }

    public void setPrev(LocalConfig config) {
        if (Objects.equals(getValue(), screenInstanceValue)) return;

        this.prevValue = screenInstanceValue;

        Object oldVal = prevValue;
        Object newVal = getValue();

        if (oldVal instanceof WalksyLibColor oldColor && newVal instanceof WalksyLibColor newColor) {
            if (oldColor.isRainbow() != newColor.isRainbow()) {
                logField(config, this.getName() + "'s Rainbow", oldColor.isRainbow(), newColor.isRainbow());
            }
            if (oldColor.getRainbowSpeed() != newColor.getRainbowSpeed()) {
                logField(config, this.getName() + "'s Rainbow Speed", oldColor.getRainbowSpeed(), newColor.getRainbowSpeed());
            }
            if (oldColor.isPulse() != newColor.isPulse()) {
                logField(config, this.getName() + "'s Pulse", oldColor.isPulse(), newColor.isPulse());
            }
            if (oldColor.getPulseSpeed() != newColor.getPulseSpeed()) {
                logField(config, this.getName() + "'s Pulse Speed", oldColor.getPulseSpeed(), newColor.getPulseSpeed());
            }
            if (Float.compare(oldColor.getSaturation(), newColor.getSaturation()) != 0) {
                logField(config, this.getName() + "'s Saturation", oldColor.getSaturation(), newColor.getSaturation());
            }
            if (Float.compare(oldColor.getBrightness(), newColor.getBrightness()) != 0) {
                logField(config, this.getName() + "'s Brightness", oldColor.getBrightness(), newColor.getBrightness());
            }
        } else if (oldVal instanceof PixelGridAnimation oldAnim && newVal instanceof PixelGridAnimation newAnim) {
            if (oldAnim.getAnimationSpeed() != newAnim.getAnimationSpeed()) {
                logField(config, this.getName() + "'s Speed", oldAnim.getAnimationSpeed(), newAnim.getAnimationSpeed());
            }
            if (Float.compare(oldAnim.getOffsetX(), newAnim.getOffsetX()) != 0) {
                logField(config, this.getName() + "'s X Pos", oldAnim.getOffsetX(), newAnim.getOffsetX());
            }
            if (Float.compare(oldAnim.getOffsetY(), newAnim.getOffsetY()) != 0) {
                logField(config, this.getName() + "'s Y Pos", oldAnim.getOffsetY(), newAnim.getOffsetY());
            }
        } else {
            logField(config, getName(), oldVal, newVal);
        }
    }


    private String formatValue(Object value) {
        if (value == null) return "Not Set";

        String str;
        if (value instanceof Float f)
            str = String.format("%.3f", f);
        else if (value instanceof Double d)
            str = String.format("%.3f", d);
        else
            str = value.toString();

        return str;
    }

    private <V> void logField(LocalConfig config, String name, V oldVal, V newVal) {
        String configName = config.name();
        InternalLog.ToolTip toolTip = null;
        if (this.warning != null)
        {
            toolTip = new InternalLog.ToolTip(Tooltip.of(Text.of("Option has warning: " + this.warning.message)), Color.RED.getRGB());
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        WalksyLibLogger.log(InternalLog.of("[" + time + "]: " + "[" + configName + "] " + "-> " + "[" + name + "], " + "[" + oldVal + "] to " + "[" + newVal + "]", toolTip));
    }


    public void reset()
    {
        if (this.getType() == Runnable.class) return;
        if (this.getValue() instanceof PixelGridAnimation) {
            this.setter.accept((T) ((PixelGridAnimation) this.defaultValue).copy());
        } else if (this.getValue() instanceof WalksyLibColor c) {
            this.setter.accept((T) ((WalksyLibColor) this.defaultValue).copy());
        } else {
            this.setter.accept(this.defaultValue);
        }
    }

    public Option<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    public boolean hasChanged() {
        T value = getValue();
        T defaultValue = getDefaultValue();

        if (value == null || defaultValue == null) {
            return value != defaultValue;
        }

        return !Objects.equals(value, defaultValue);
    }

    public T getPrevValue()
    {
        return this.prevValue;
    }

    public boolean searched() {
        if (searchQ.isEmpty()) return true;

        String[] queryWords = searchQ.toLowerCase().trim().split("\\s+");
        String[] nameWords = this.getName().toLowerCase().trim().split("\\s+");

        outer:
        for (String qWord : queryWords) {
            for (String numWord : nameWords) {
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

    public void updateSearchQ(String searchQ)
    {
        this.searchQ = searchQ;
    }

    public void runChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    @SuppressWarnings("unchecked")
    public OptionWidget createWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height) {
        if (type == Boolean.class) {
            return new BooleanWidget(parent, screen, x, y, width, height, (Option<Boolean>) this, this.warning);
        } else if (type == Integer.class) {
            return new NumericalWidget<Integer>(parent, screen, x, y, width, height, (Option<Integer>) this);
        } else if (type == Double.class) {
            return new NumericalWidget<Double>(parent, screen, x, y, width, height, (Option<Double>) this);
        } else if (type == Float.class) {
            return new NumericalWidget<Float>(parent, screen, x, y, width, height, (Option<Float>) this);
        } else if (type == java.awt.Color.class || type == WalksyLibColor.class) {
            return new ColorWidget(parent, screen, x, y, width, height, (Option<WalksyLibColor>) this);
        } else if (type == PixelGridAnimation.class) {
            return new PixelGridAnimationWidget(parent, screen, x, y, width, height, (Option<PixelGridAnimation>) this);
        } else if (type == List.class) {
            return new StringListOptionWidget(parent, screen, x, y, width, height, (Option<List<String>>) this);
        } else if (type == Runnable.class) {
            return new ButtonOptionWidget(parent, screen, x, y, width, height, (Option<Runnable>) this);
        } else if (type == IdentifierWrapper.class) {
            return new SpriteOptionWidget(parent, screen, x, y, width, height, (Option<IdentifierWrapper>) this);
        } else if (type == String.class) {
            return new StringOptionWidget(parent, screen, x, y, width, height, (Option<String>) this);
        }  else if (Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Option<? extends Enum<?>> enumOption = (Option<? extends Enum<?>>) this;

            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) type;

            return new EnumOptionWidget(parent, screen, x, y, width, height, enumOption);
        }  else {
            throw new UnsupportedOperationException("Unsupported option type: " + type);
        }
    }
}
