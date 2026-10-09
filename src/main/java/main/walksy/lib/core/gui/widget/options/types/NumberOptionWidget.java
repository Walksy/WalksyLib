package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.SliderControl;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class NumberOptionWidget extends OptionRowWidget<Number> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x40B3E0L);
    private final SliderControl slider;

    public NumberOptionWidget(final Option<Number> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.slider = new SliderControl(this::value, this.min(), this.max(), this.increment(), FADE_KEYS.addAndGet(5) - 4, v -> this.option.setValue(this.snap(v)), true, v -> this.formatValue(), SliderControl.Metrics.row(), this.uiScale, null);
    }

    private double min() {
        return this.option.getMin().doubleValue();
    }

    private double max() {
        return this.option.getMax().doubleValue();
    }

    private double increment() {
        return this.option.getIncrement().doubleValue();
    }

    private double value() {
        return this.option.getValue().doubleValue();
    }

    private Number snap(final double raw) {
        final double min = this.min();
        final double max = this.max();
        final double inc = this.increment();
        double snapped = raw;
        if (inc > 0) {
            snapped = min + Math.round((raw - min) / inc) * inc;
        }
        snapped = Math.max(min, Math.min(max, snapped));
        final Number current = this.option.getValue();
        if (current instanceof Integer) {
            return (int) Math.round(snapped);
        }
        if (current instanceof Float) {
            return (float) snapped;
        }
        return snapped;
    }

    private String formatValue() {
        final Number v = this.option.getValue();
        if (v instanceof Integer) {
            return String.valueOf(v.intValue());
        }
        final double inc = this.increment();
        final int decimals = inc > 0 && inc < 1 ? Math.max(1, (int) Math.ceil(-Math.log10(inc))) : 2;
        return String.format(Locale.ROOT, "%." + decimals + "f", v.doubleValue());
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.slider.setAvailability(this.availability());
        this.slider.extractInRow(graphics, dt, this.controlAreaX(), this.getX() + this.getWidth() - PADDING * this.uiScale, this.getY(), this.rowLabelHeight(), this.bandAlpha(), mouseX, mouseY, dt);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive()) {
            return false;
        }
        if (this.slider.isOverField(event.x(), event.y())) {
            this.slider.setFieldFocused(true);
            this.slider.fieldClicked(event, doubleClick);
            return true;
        }
        this.slider.setFieldFocused(false);
        if (this.slider.isOverTrack(event.x(), event.y())) {
            this.slider.startDrag(event.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.slider.isFieldFocused()) {
            return this.slider.fieldDragged(event, dragX, dragY);
        }
        if (this.slider.isDragging() && this.isInteractive()) {
            this.slider.dragTo(event.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        final boolean was = this.slider.isDragging();
        this.slider.release();
        return was;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.slider.isFieldFocused() && this.slider.fieldKeyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.slider.isFieldFocused() && this.slider.fieldCharTyped(event);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.slider.setFieldFocused(false);
        }
    }
}
