package main.walksy.lib.core.gui.widget.options.control;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.util.Mth;

public final class ToggleControl {
    public static final float TRACK_W = 30f;
    public static final float TRACK_H = 13f;
    public static final float KNOB_REST_INSET = 1.2f;
    public static final float KNOB_PRESS_OUTSET = 3.5f;
    public static final float KNOB_WIDTH_RATIO = 1.5f;
    private static final float DRAG_START_THRESHOLD = 2f;
    private static final float TRACK_COLOR_SPEED = 14f;
    private final BooleanSupplier value;
    private final float uiScale;
    private final long fadeKeyBase;
    private final Consumer<Boolean> onCommit;
    private boolean trackOn;
    private float trackColorMix;
    private float availFade = 1f;
    private final GlassStyle trackOffStyle;
    private final GlassStyle trackOnStyle;
    private final LiquidKnob knob;
    private boolean pressed;
    private boolean dragStarted;
    private float pressX;
    private float dragT;
    private float trackX0;
    private float trackY0;
    private float trackX1;
    private float trackH;
    private static final Map<Object, float[]> CARRIED = new WeakHashMap<>();
    private final Object animationKey;

    public ToggleControl(final BooleanSupplier value, final float uiScale, final long fadeKeyBase, final Consumer<Boolean> onCommit, final Integer layerBase) {
        this(value, uiScale, fadeKeyBase, onCommit, layerBase, null);
    }

    public ToggleControl(final BooleanSupplier value, final float uiScale, final long fadeKeyBase, final Consumer<Boolean> onCommit, final Integer layerBase, final Object animationKey) {
        this.animationKey = animationKey;
        this.value = value;
        this.uiScale = uiScale;
        this.fadeKeyBase = fadeKeyBase;
        this.onCommit = onCommit;
        final int knobLayer = layerBase != null ? layerBase + 1 : ControlLayers.KNOB;
        this.knob = new LiquidKnob(value.getAsBoolean() ? 1f : 0f, KNOB_WIDTH_RATIO, uiScale, fadeKeyBase + 1, knobLayer, true);
        this.trackOffStyle = LiquidKnob.grayTrackStyle();
        if (layerBase != null) {
            this.trackOffStyle.layer(layerBase);
        }
        this.trackOnStyle = this.trackOffStyle.copy().tint(LiquidKnob.ACCENT_COLOR(), 1.25f);
        this.trackOn = value.getAsBoolean();
        this.trackColorMix = this.trackOn ? 1f : 0f;
        final float[] carried = animationKey == null ? null : CARRIED.get(animationKey);
        if (carried != null) {
            this.knob.seedSpring(carried[0], carried[1]);
            this.trackColorMix = carried[2];
        }
    }

    private void carry() {
        if (this.animationKey != null) {
            CARRIED.put(this.animationKey, new float[] { this.knob.springValue(), this.knob.springVelocity(), this.trackColorMix});
        }
    }

    private float knobRestHeight() {
        return (TRACK_H - KNOB_REST_INSET * 2f) * this.uiScale;
    }

    private float knobTravelMin() {
        return this.trackX0 + KNOB_REST_INSET * this.uiScale + this.knobRestHeight() * KNOB_WIDTH_RATIO / 2f;
    }

    private float knobTravelMax() {
        return this.trackX1 - KNOB_REST_INSET * this.uiScale - this.knobRestHeight() * KNOB_WIDTH_RATIO / 2f;
    }

    public boolean isOverControl(final double mouseX, final double mouseY) {
        final float slop = 2f * this.uiScale;
        return mouseX >= this.trackX0 - slop && mouseX <= this.trackX1 + slop && mouseY >= this.trackY0 - slop && mouseY <= this.trackY0 + this.trackH + slop;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    public void extract(final WLGraphics2D graphics, final float dt, final float alpha, final float x1, final float rowY, final float rowHeight) {
        final boolean value = this.value.getAsBoolean();
        final boolean dragging = this.pressed && this.dragStarted;
        final float knobTarget = dragging ? this.dragT : (value ? 1f : 0f);
        final float trackW = TRACK_W * this.uiScale;
        final float trackH = TRACK_H * this.uiScale;
        final float x0 = x1 - trackW;
        final float y0 = rowY + (rowHeight - trackH) / 2f;
        this.trackX0 = x0;
        this.trackY0 = y0;
        this.trackX1 = x1;
        this.trackH = trackH;
        this.knob.extract(graphics, dt, knobTarget, this.pressed, dragging, this.knobTravelMin(), this.knobTravelMax(), y0 + trackH / 2f, this.knobRestHeight(), (TRACK_H + KNOB_PRESS_OUTSET * 2f) * this.uiScale, alpha);
        final float pos = this.knob.position01();
        if (pos >= 0.98f) {
            this.trackOn = true;
        } else if (pos <= 0.02f) {
            this.trackOn = false;
        }
        this.trackColorMix = MathUtils.approach(this.trackColorMix, this.trackOn ? 1f : 0f, TRACK_COLOR_SPEED, dt);
        this.carry();
        graphics.glass()
            .dimensions(x0, y0, trackW, trackH)
            .cornerRadius(trackH / 2f)
            .style(this.trackMaterial().scaled(this.uiScale).opacity(alpha))
            .extract();
    }

    private GlassStyle trackMaterial() {
        final GlassStyle live = this.trackStyle();
        if (this.availFade >= 0.999f) {
            return live;
        }
        final GlassStyle off = GlassStyle.inactive(live).tint(Theme.get().trackFillSolid(), Theme.tintAlpha(Theme.SURFACE_OPACITY));
        final GlassStyle blend = this.availFade <= 0.001f ? off : GlassStyle.lerp(off, live, this.availFade);
        blend.layer(live.getLayer());
        if (live.hasFadeKey()) {
            blend.fadeKey(live.getFadeKey());
        }
        return blend;
    }

    public void setAvailability(final float amount) {
        this.availFade = Math.max(0f, Math.min(1f, amount));
        this.knob.setAvailability(this.availFade);
    }

    private GlassStyle trackStyle() {
        final float offA = this.trackOffStyle.getTintAlpha();
        final float onA = this.trackOnStyle.getTintAlpha();
        final float mixA = offA + (onA - offA) * this.trackColorMix;
        return this.trackOffStyle.copy()
            .tint(blendTint(this.trackOffStyle.getTintColor(), offA, this.trackOnStyle.getTintColor(), onA, this.trackColorMix, mixA), mixA)
            .fadeKey(this.fadeKeyBase);
    }

    public void settle() {
        this.pressed = false;
        this.dragStarted = false;
        this.knob.settle();
        this.carry();
    }

    public LiquidRect anchorRect() {
        return new LiquidRect(this.trackX0, this.trackY0, this.trackX1 - this.trackX0, this.trackH, this.trackH / 2f);
    }

    public GlassStyle originStyle(final int panelLayer) {
        return this.trackStyle().scaled(this.uiScale).layer(panelLayer);
    }

    public boolean mouseClicked(final MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.isOverControl(event.x(), event.y())) {
            this.pressed = true;
            this.dragStarted = false;
            this.pressX = (float) event.x();
            return true;
        }
        return false;
    }

    public boolean mouseDragged(final MouseButtonEvent event) {
        if (!this.pressed) {
            return false;
        }
        if (!this.dragStarted && Math.abs(event.x() - this.pressX) > DRAG_START_THRESHOLD * this.uiScale) {
            this.dragStarted = true;
        }
        if (this.dragStarted) {
            final float min = this.knobTravelMin();
            final float max = this.knobTravelMax();
            this.dragT = Mth.clamp((float) ((event.x() - min) / (max - min)), 0f, 1f);
        }
        return true;
    }

    public boolean mouseReleased(final MouseButtonEvent event) {
        if (!this.pressed) {
            return false;
        }
        this.pressed = false;
        final boolean value = this.value.getAsBoolean();
        if (this.dragStarted) {
            final boolean next = this.dragT > 0.5f;
            if (next != value) {
                this.onCommit.accept(next);
            }
        } else if (this.isOverControl(event.x(), event.y())) {
            this.onCommit.accept(!value);
        }
        this.dragStarted = false;
        return true;
    }

    public void cancel() {
        this.pressed = false;
        this.dragStarted = false;
    }

    private static int blendTint(final int offColor, final float offA, final int onColor, final float onA, final float t, final float mixA) {
        if (mixA <= 1.0e-4f) {
            return offColor;
        }
        final int r = premul((offColor >> 16) & 0xFF, offA, (onColor >> 16) & 0xFF, onA, t, mixA);
        final int g = premul((offColor >> 8) & 0xFF, offA, (onColor >> 8) & 0xFF, onA, t, mixA);
        final int b = premul(offColor & 0xFF, offA, onColor & 0xFF, onA, t, mixA);
        return (r << 16) | (g << 8) | b;
    }

    private static int premul(final int offCh, final float offA, final int onCh, final float onA, final float t, final float mixA) {
        final float v = (offCh * offA * (1f - t) + onCh * onA * t) / mixA;
        return (int) Mth.clamp(Math.round(v), 0f, 255f);
    }
}
