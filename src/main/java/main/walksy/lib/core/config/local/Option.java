package main.walksy.lib.core.config.local;

import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.*;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
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

    //Screen
    private T screenInstanceValue = null;

    //Boolean Option
    private BooleanOption.Warning warning;

    //Color Option
    private float hue = 0f;
    private float saturation;
    private float brightness;
    private int alpha;
    private boolean rainbow = false;
    private int rainbowSpeed = 5;
    private int pulseSpeed = 5;
    private  PulseValue pulseValue;
    public LoadedAdditions loadedAdditions = null;


    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type, T defaultValue, BooleanOption.Warning warning) {
        this(name, description, getter, setter, type, null, null, null, defaultValue, warning);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type, T defaultValue) {
        this(name, description, getter, setter, type, null, null, null, defaultValue, null);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter,
                  Class<T> type, T min, T max, T increment, T defaultValue, BooleanOption.Warning warning) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
        this.min = min;
        this.max = max;
        this.increment = increment;
        if (getter.get() instanceof PixelGridAnimation) {
            this.defaultValue = (T) ((PixelGridAnimation)defaultValue).copy();
        } else {
            this.defaultValue = defaultValue;
        }
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

    public boolean isRainbow() {
        return rainbow;
    }

    public void setHue(float hue) {
        this.hue = Math.max(0f, Math.min(1f, hue));
    }

    public float getHue() {
        return this.hue;
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.max(0f, Math.min(1f, saturation));
    }

    public float getSaturation() {
        return this.saturation;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(0f, Math.min(1f, brightness));
    }

    public float getBrightness() {
        return this.brightness;
    }

    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    public int getAlpha() {
        return this.alpha;
    }

    public int getRainbowSpeed()
    {
        return this.rainbowSpeed;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public void setRainbowSpeed(int rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }

    public int getPulseSpeed() {
        return pulseSpeed;
    }

    public void setPulseSpeed(int pulseSpeed) {
        this.pulseSpeed = pulseSpeed;
    }

    public PulseValue getPulseValue()
    {
        return pulseValue;
    }

    public void setPulseValue(@Nullable PulseValue pulseValue)
    {
        this.pulseValue = pulseValue;
    }

    public boolean screenInstanceCheck()
    {
        return Objects.equals(this.screenInstanceValue, this.getValue());
    }

    public void setScreenInstance()
    {
        if (this.getValue() instanceof PixelGridAnimation animation) {
            this.screenInstanceValue = (T) animation.copy();
        } else {
            this.screenInstanceValue = this.getter.get();
        }
    }

    public void undo()
    {
        this.setter.accept(this.screenInstanceValue);
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if (type.isInstance(value)) {
            setter.accept((T) value);
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
        }
    }

    public boolean canReset()
    {
        return this.getValue() != this.defaultValue;
    }

    public void reset()
    {
        if (this.getValue() instanceof PixelGridAnimation) {
            this.setter.accept((T) ((PixelGridAnimation)this.defaultValue).copy());
        } else {
            this.setter.accept(this.defaultValue);
        }
        this.resetHSB();
        this.resetAdditions();
    }

    public void resetHSB()
    {
        if (this.type != Color.class) return;
        Color color = (Color) this.getValue();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    private void resetAdditions()
    {
        this.pulseValue = null;
        this.rainbow = false;
        this.rainbowSpeed = 5;
        this.pulseSpeed = 5;
    }

    public Option<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    public void tick() {
        this.handleRainbow();
        this.handlePulse();
    }

    private void handleRainbow()
    {
        if (!rainbow || type != Color.class) return;

        float speed = (float) this.rainbowSpeed / 1000;
        hue += speed;
        if (hue > 1f) hue = 0f;

        Color newColor = Color.getHSBColor(hue, saturation, brightness);
        Color neww = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), getAlpha());
        setValue((T) neww);
    }

    private float pulseTime = 0;

    private void handlePulse() {
        if (this.pulseValue == null || type != Color.class) return;

        pulseTime += (float) this.pulseSpeed / 1000f;
        brightness = (float) ((Math.sin(pulseTime * 2 * Math.PI) + 1) / 2);
        Color newColor = Color.getHSBColor(hue, saturation, brightness);
        Color neww = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), getAlpha());

        setValue((T) neww);
    }

    public boolean hasChanged() {
        T value = getValue();
        T defaultValue = getDefaultValue();

        if (value == null || defaultValue == null) {
            return value != defaultValue;
        }

        if (type == Color.class) {
            Color valColor = (Color) value;
            Color defColor = (Color) defaultValue;

            return valColor.getRed() != defColor.getRed() ||
                    valColor.getGreen() != defColor.getGreen() ||
                    valColor.getBlue() != defColor.getBlue() ||
                    valColor.getAlpha() != defColor.getAlpha();
        }

        return !Objects.equals(value, defaultValue);
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
        } else if (type == Color.class) {
            return new ColorWidget(parent, screen, x, y, width, height, (Option<Color>) this);
        } else if (type == PixelGridAnimation.class) {
            return new PixelGridAnimationWidget(parent, screen, x, y, width, height, (Option<PixelGridAnimation>) this);
        } else {
            throw new UnsupportedOperationException("Unsupported option type: " + type);
        }
    }

    public enum PulseValue
    {
        SINE,
        SQUARE
    }

    public record LoadedAdditions(boolean rainbow, int rainbowSpeed, int pulseSpeed, PulseValue pulseValue)
    {
        public void reload(Option<?> option)
        {
            option.setRainbow(rainbow);
            option.setRainbowSpeed(rainbowSpeed);
            option.setPulseSpeed(pulseSpeed);
            option.setPulseValue(pulseValue);
        }
    }
}
