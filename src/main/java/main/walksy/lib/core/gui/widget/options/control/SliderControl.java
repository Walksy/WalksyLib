package main.walksy.lib.core.gui.widget.options.control;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.widget.AbstractTextEditWidget;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import net.minecraft.util.Mth;

public final class SliderControl {
    private final DoubleSupplier value;
    private final double min;
    private final double max;
    private final double increment;
    private final DoubleConsumer onChange;
    private final boolean hasField;
    private final DoubleFunction<String> formatter;
    private final Metrics metrics;
    private final float uiScale;
    private static final float DISABLED_FIELD_DIM = 0.4f;
    private float availFade = 1f;
    private final GlassStyle trackStyle;
    private final GlassStyle fillStyle;
    private final GlassStyle fieldStyle;
    private final LiquidKnob knob;
    private final ValueField field;
    private boolean dragging;
    private float dragT;
    private float x0;
    private float x1;
    private float y;
    private float h;

    public record Metrics(float knobWidthRatio, float knobRestWidthRatio, float knobRestH, float knobPressH, float trackH, float valueReserve, float valueGap, float fieldHeightRatio, float labelSize, float fieldTextSize, float fieldTextInset, int textColor, int maxFieldLength, boolean allowDecimalAndSign, float fieldCapHeight) {

        public static final float DEFAULT_CAP_HEIGHT = 0.7f;

        public Metrics(final float knobWidthRatio, final float knobRestWidthRatio, final float knobRestH, final float knobPressH, final float trackH, final float valueReserve, final float valueGap, final float fieldHeightRatio, final float labelSize, final float fieldTextSize, final float fieldTextInset, final int textColor, final int maxFieldLength, final boolean allowDecimalAndSign) {
            this(knobWidthRatio, knobRestWidthRatio, knobRestH, knobPressH, trackH, valueReserve, valueGap, fieldHeightRatio, labelSize, fieldTextSize, fieldTextInset, textColor, maxFieldLength, allowDecimalAndSign, DEFAULT_CAP_HEIGHT);
        }

        public Metrics withFieldCapHeight(final float capHeight) {
            return new Metrics(this.knobWidthRatio, this.knobRestWidthRatio, this.knobRestH, this.knobPressH, this.trackH, this.valueReserve, this.valueGap, this.fieldHeightRatio, this.labelSize, this.fieldTextSize, this.fieldTextInset, this.textColor, this.maxFieldLength, this.allowDecimalAndSign, capHeight);
        }

        public static Metrics row() {
            return new Metrics(1.55f, 1.7f, 12f, 19f, 4f, 34f, 4f, 0.72f, 0f, 11f, 4f, OptionRowWidget.LABEL_COLOR(), 16, true);
        }

        public static Metrics popover() {
            return new Metrics(1.55f, 1.7f, 10f, 17f, 4f, 28f, 4f, 0.56f, 9f, 9f, 3f, Theme.get().label(), 3, false);
        }
    }

    public SliderControl(DoubleSupplier value, double min, double max, double increment, long fadeKeyBase, DoubleConsumer onChange, boolean hasField, DoubleFunction<String> formatter, Metrics metrics, float uiScale, Integer layerBase) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.onChange = onChange;
        this.hasField = hasField;
        this.formatter = formatter;
        this.metrics = metrics;
        this.uiScale = uiScale;
        this.trackStyle = LiquidKnob.grayTrackStyle().fadeKey(fadeKeyBase);
        this.fillStyle = LiquidKnob.accentFillStyle().fadeKey(fadeKeyBase + 1);
        this.fieldStyle = SearchBar.defaultStyle().fadeKey(fadeKeyBase + 3);
        if (layerBase != null) {
            this.trackStyle.layer(layerBase);
            this.fillStyle.layer(layerBase + 1);
            this.fieldStyle.layer(layerBase + 3);
        }
        final int knobLayer = layerBase != null ? layerBase + 2 : ControlLayers.KNOB;
        this.knob = new LiquidKnob((float) this.value01(value.getAsDouble()), metrics.knobWidthRatio(), uiScale, fadeKeyBase + 2, knobLayer).restWidthRatio(metrics.knobRestWidthRatio());
        this.field = hasField ? new ValueField() : null;
    }

    private double value01(final double v) {
        return this.max > this.min ? Mth.clamp((v - this.min) / (this.max - this.min), 0f, 1f) : 0f;
    }

    private double snap(final double raw) {
        final double clamped = Math.max(this.min, Math.min(this.max, raw));
        if (this.increment <= 0) {
            return clamped;
        }
        final long steps = Math.round((clamped - this.min) / this.increment);
        return Math.max(this.min, Math.min(this.max, this.min + steps * this.increment));
    }

    private double fromT(final float t) {
        return this.snap(this.min + Mth.clamp(t, 0f, 1f) * (this.max - this.min));
    }

    private float travelMin() {
        return this.x0 + this.metrics.knobRestH() * this.metrics.knobWidthRatio() * this.uiScale / 2f;
    }

    private float travelMax() {
        return this.x1 - this.metrics.knobRestH() * this.metrics.knobWidthRatio() * this.uiScale / 2f;
    }

    private float tAt(final double mouseX) {
        final float min = this.travelMin();
        final float max = this.travelMax();
        return Mth.clamp((float) ((mouseX - min) / (max - min)), 0f, 1f);
    }

    public boolean isOverTrack(final double mouseX, final double mouseY) {
        return mouseX >= this.x0 && mouseX <= this.x1 && mouseY >= this.y && mouseY <= this.y + this.h;
    }

    public boolean isOverField(final double mouseX, final double mouseY) {
        return this.hasField && mouseX >= this.field.getX() && mouseX < this.field.getX() + this.field.getWidth() && mouseY >= this.field.getY() && mouseY < this.field.getY() + this.field.getHeight();
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void startDrag(final double mouseX) {
        this.dragging = true;
        this.dragT = this.tAt(mouseX);
        this.onChange.accept(this.fromT(this.dragT));
    }

    public void dragTo(final double mouseX) {
        this.dragT = this.tAt(mouseX);
        this.onChange.accept(this.fromT(this.dragT));
    }

    public void release() {
        this.dragging = false;
    }

    public boolean isFieldFocused() {
        return this.hasField && this.field.isFocused();
    }

    public void setFieldFocused(final boolean focused) {
        if (this.hasField) {
            this.field.setFocused(focused);
        }
    }

    public void fieldClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.hasField) {
            this.field.onClick(event, doubleClick);
        }
    }

    public boolean fieldDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return this.hasField && this.field.mouseDragged(event, dragX, dragY);
    }

    public boolean fieldKeyPressed(final KeyEvent event) {
        return this.hasField && this.field.keyPressed(event);
    }

    public boolean fieldCharTyped(final CharacterEvent event) {
        return this.hasField && this.field.charTyped(event);
    }

    private void extractTrackFillKnob(final WLGraphics2D graphics, final float dt, final float x0, final float trackY, final float trackX1, final float trackH, final float alpha) {
        graphics.glass()
            .dimensions(x0, trackY, trackX1 - x0, trackH)
            .cornerRadius(trackH / 2f)
            .style(this.trackMaterial().scaled(this.uiScale).opacity(alpha))
            .extract();
        final float knobTarget = this.dragging ? this.dragT : (float) this.value01(this.value.getAsDouble());
        this.knob.extract(graphics, dt, knobTarget, this.dragging, this.dragging, this.travelMin(), this.travelMax(), trackY + trackH / 2f, this.metrics.knobRestH() * this.uiScale, this.metrics.knobPressH() * this.uiScale, alpha);
        final float fillW = this.travelMin() + (this.travelMax() - this.travelMin()) * Mth.clamp(this.knob.position01(), 0f, 1f) - x0;
        graphics.glass()
            .dimensions(x0, trackY, Math.max(0f, fillW), trackH)
            .cornerRadius(trackH / 2f)
            .style(this.fillMaterial().scaled(this.uiScale).opacity(alpha))
            .extract();
    }

    public void setAvailability(final float amount) {
        this.availFade = Math.max(0f, Math.min(1f, amount));
        this.knob.setAvailability(this.availFade);
    }

    private GlassStyle trackMaterial() {
        if (this.availFade >= 0.999f) {
            return this.trackStyle;
        }
        final GlassStyle off = GlassStyle.inactive(this.trackStyle).tint(Theme.get().trackFillSolid(), Theme.tintAlpha(Theme.SURFACE_OPACITY));
        final GlassStyle blend = this.availFade <= 0.001f ? off : GlassStyle.lerp(off, this.trackStyle, this.availFade);
        blend.layer(this.trackStyle.getLayer());
        if (this.trackStyle.hasFadeKey()) {
            blend.fadeKey(this.trackStyle.getFadeKey());
        }
        return blend;
    }

    private GlassStyle fillMaterial() {
        if (this.availFade >= 0.999f) {
            return this.fillStyle;
        }
        final GlassStyle off = GlassStyle.inactive(this.fillStyle).tint(Theme.get().trackFillSolid(), Theme.tintAlpha(Theme.SURFACE_OPACITY));
        final GlassStyle blend = this.availFade <= 0.001f ? off : GlassStyle.lerp(off, this.fillStyle, this.availFade);
        blend.layer(this.fillStyle.getLayer());
        if (this.fillStyle.hasFadeKey()) {
            blend.fadeKey(this.fillStyle.getFadeKey());
        }
        return blend;
    }

    public void extractInRow(final WLGraphics2D graphics, final float dt, final float x0, final float x1, final float rowY, final float rowHeight, final float alpha, final int mouseX, final int mouseY, final float delta) {
        final float trackX1 = x1 - this.metrics.valueReserve() * this.uiScale;
        final float trackH = this.metrics.trackH() * this.uiScale;
        final float trackY = rowY + (rowHeight - trackH) / 2f;
        this.x0 = x0;
        this.x1 = trackX1;
        this.y = rowY;
        this.h = rowHeight;
        this.extractTrackFillKnob(graphics, dt, x0, trackY, trackX1, trackH, alpha);
        final float fieldX0 = trackX1 + this.metrics.valueGap() * this.uiScale;
        final float fieldX1 = x1;
        final float fieldH = rowHeight * this.metrics.fieldHeightRatio();
        final float fieldY0 = rowY + (rowHeight - fieldH) / 2f;
        this.extractField(graphics, mouseX, mouseY, delta, fieldX0, fieldY0, fieldX1 - fieldX0, fieldH, alpha);
    }

    private void extractField(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta, final float fieldX0, final float fieldY0, final float fieldW, final float fieldH, final float alpha) {
        if (!this.hasField) {
            return;
        }
        this.field.setX(Math.round(fieldX0));
        this.field.setY(Math.round(fieldY0));
        this.field.setWidth(Math.round(fieldW));
        this.field.setHeight(Math.round(fieldH));
        if (!this.field.isFocused()) {
            this.field.setValue(this.formatter.apply(this.value.getAsDouble()));
        }
        this.field.setAlpha(alpha * (DISABLED_FIELD_DIM + (1f - DISABLED_FIELD_DIM) * this.availFade));
        graphics.widget(this.field, mouseX, mouseY, delta);
    }

    private final class ValueField extends AbstractTextEditWidget {
        ValueField() {
            super(0, 0, 1, 1);
            this.setMaxLength(SliderControl.this.metrics.maxFieldLength());
        }

        @Override
        protected void insertText(final String raw) {
            final StringBuilder filtered = new StringBuilder(raw.length());
            final boolean allowDecimalAndSign = SliderControl.this.metrics.allowDecimalAndSign();
            for (int i = 0; i < raw.length(); i++) {
                final char c = raw.charAt(i);
                if (Character.isDigit(c) || (allowDecimalAndSign && (c == '.' || c == '-'))) {
                    filtered.append(c);
                }
            }
            super.insertText(filtered.toString());
        }

        @Override
        protected void onTextChanged() {
            final String text = this.getValue().replaceAll("[^0-9.+-]", "");
            if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
                return;
            }
            double parsed;
            try {
                parsed = Double.parseDouble(text);
            } catch (final NumberFormatException ignored) {
                return;
            }
            SliderControl.this.onChange.accept(SliderControl.this.snap(parsed));
        }

        @Override
        protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
            final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
            if (alpha <= 0.001f) {
                return;
            }
            final float dt = WLRenderSystem.frameDeltaSeconds();
            this.extractFieldGlass(graphics, SliderControl.this.fieldStyle, alpha, SliderControl.this.uiScale, dt);
            final float inset = SliderControl.this.metrics.fieldTextInset() * SliderControl.this.uiScale;
            final float textStartX = this.getX() + inset;
            final float innerWidth = Math.max(4f, this.getWidth() - inset * 2f);
            final float size = Math.min(SliderControl.this.metrics.fieldTextSize() * SliderControl.this.uiScale, this.getHeight() * 0.55f);
            final String visible = this.layoutText(textStartX, innerWidth, size);
            final float capHeight = size * SliderControl.this.metrics.fieldCapHeight();
            final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
            final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            if (!visible.isEmpty()) {
                Fonts.SFPRO_DISPLAY.extract(graphics, visible, textStartX, textTop, size, ColorUtils.withAlpha(SliderControl.this.metrics.textColor(), alpha));
            }
            this.extractCaretAndSelection(graphics, "", textStartX, size, alpha, SliderControl.this.uiScale);
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (this.handleCommonKeyPress(event)) {
                return true;
            }
            if (!this.isFocused() || !this.active) {
                return false;
            }
            switch (event.key()) {
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_ESCAPE -> {
                    this.setFocused(false);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
}
}
