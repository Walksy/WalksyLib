package main.walksy.lib.core.gui.widget.options.types;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.AbstractTextEditWidget;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.ColorPickerBody;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob2D;
import main.walksy.lib.core.gui.widget.options.control.ToggleControl;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.util.Mth;

public class ColorOptionWidget extends OptionRowWidget<WLColor> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0xC01000L);
    private static final int PICKER_TRACK_LAYER = PopoverHost.CONTENT_LAYER;
    private static final int PICKER_FILL_LAYER = PopoverHost.CONTENT_LAYER + 1;
    private static final int PICKER_KNOB_LAYER = PopoverHost.CONTENT_LAYER + 2;
    private static final float HOVER_SPEED = 14f;
    private static final float HOVER_POP = 1.5f;
    private static final float SWATCH_TINT_BOOST = 1.25f;
    private static final float PATTERN_SPEED = 10f;
    private static final float PICKER_WIDTH = 200f;
    private static final float PICKER_PAD = 12f;
    private static final float PICKER_GAP = 8f;
    private static final float BOX_ASPECT = 0.55f;
    private static final float HUE_H = 10f;
    private static final float ALPHA_CHECKER_CELL = 5f;
    private static final float SLIDER_ROW_H = 14f;
    private static final float CHROMA_ROW_H = 16f;
    private static final float HEX_ROW_H = 18f;
    private static final float SLIDER_TRACK_H = 4f;
    private static final float SLIDER_KNOB_REST_H = 10f;
    private static final float SLIDER_KNOB_PRESS_H = 16f;
    private static final float SLIDER_KNOB_RATIO = 1.4f;
    private static final float SWITCH_W = ToggleControl.TRACK_W;
    private static final float SWITCH_H = ToggleControl.TRACK_H;
    private static final float SWITCH_KNOB_RATIO = ToggleControl.KNOB_WIDTH_RATIO;
    private static final float SWITCH_KNOB_INSET = ToggleControl.KNOB_REST_INSET;
    private static final float SWITCH_KNOB_OUTSET = ToggleControl.KNOB_PRESS_OUTSET;
    private static final float CHROMA_DRAG_START_THRESHOLD = 2f;
    private static final float SPEED_FIELD_RESERVE = 26f;
    private static final float SPEED_FIELD_GAP = 4f;
    private static final float HUE_KNOB_REST_DIAMETER = 9f;
    private static final float HUE_KNOB_PRESS_DIAMETER = 18f;
    private static final float SWATCH_GAP = 3f;
    private static final float SWATCH_ROW_LABEL_H = 10f;
    private static final float SWATCH_LABEL_GAP = 3f;
    private final GlassStyle swatchStyle;
    private final GlassStyle swatchBackingStyle;
    private final GlassStyle trackStyle;
    private final GlassStyle fillStyle;
    private final GlassStyle speedFieldStyle;
    private final GlassStyle switchOffStyle;
    private final GlassStyle switchOnStyle;
    private final long switchTrackFadeKey;
    private final LiquidKnob speedKnob;
    private final LiquidKnob chromaKnob;
    private final LiquidKnob2D alphaKnob;
    private final ColorPickerBody colorPickerBody;
    private final SpeedField speedField;
    private final AbstractTextEditWidget[] textFields;
    private float hoverAnim;
    private boolean pickerActive;
    private float patternReveal = 1f;
    private float backingX;
    private float backingY;
    private float backingW;
    private float backingH;

    private static int CHECKER_LIGHT() {
        return Theme.get().checkerLight();
    }

    private static int CHECKER_DARK() {
        return Theme.get().checkerDark();
    }

    private static int CHECKER_MEAN() {
        return Theme.get().checkerMean();
    }

    public ColorOptionWidget(final Option<WLColor> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.swatchStyle = WLButton.panelLens(WLButton.defaultStyle())
            .blurRadius(LiquidGlassConstants.BLUR_POPOVER)
            .fadeKey(FADE_KEYS.incrementAndGet())
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
        this.swatchBackingStyle = this.swatchStyle.copy()
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .refractionReach(0f)
            .brightEdge(0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), 0f)
            .innerShadow(0f)
            .rimLight(Theme.get().rimLightColor(), 0f);
        this.trackStyle = LiquidKnob.grayTrackStyle().layer(PICKER_TRACK_LAYER);
        this.fillStyle = LiquidKnob.accentFillStyle().layer(PICKER_FILL_LAYER);
        this.speedFieldStyle = SearchBar.defaultStyle().layer(PopoverHost.CONTENT_LAYER).fadeKey(FADE_KEYS.incrementAndGet());
        this.switchOffStyle = LiquidKnob.grayTrackStyle().layer(PICKER_TRACK_LAYER);
        this.switchOnStyle = this.switchOffStyle.copy().tint(LiquidKnob.ACCENT_COLOR(), 1.25f);
        this.switchTrackFadeKey = FADE_KEYS.incrementAndGet();
        final WLColor color = option.getValue();
        this.speedKnob = new LiquidKnob(speed01(color.getRainbowSpeed()), SLIDER_KNOB_RATIO, this.uiScale, FADE_KEYS.addAndGet(4), PICKER_KNOB_LAYER);
        this.chromaKnob = new LiquidKnob(color.isRainbow() ? 1f : 0f, SWITCH_KNOB_RATIO, this.uiScale, FADE_KEYS.addAndGet(4), PICKER_KNOB_LAYER);
        this.alphaKnob = new LiquidKnob2D(color.getAlpha() / 255f, 0.5f, this.uiScale, FADE_KEYS.addAndGet(4), PICKER_KNOB_LAYER, HUE_KNOB_REST_DIAMETER, HUE_KNOB_PRESS_DIAMETER).withoutInnerShadow();
        final long colorPickerFadeKeyBase = FADE_KEYS.addAndGet(ColorPickerBody.FADE_KEY_COUNT) - ColorPickerBody.FADE_KEY_COUNT + 1;
        this.colorPickerBody = new ColorPickerBody(this.option::getValue, t -> this.mutate(c -> {
            c.setHue(t);
            c.syncValueFromHsb();
        }), (sat, val) -> this.mutate(c -> {
            c.setSaturation(sat);
            c.setBrightness(val);
            c.syncValueFromHsb();
        }), rgb -> {
            final WLColor current = this.option.getValue();
            final WLColor fresh = new WLColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, current.getAlpha());
            fresh.setAdditions(current.getAdditions());
            this.option.setValue(fresh);
        }, this::applySwatchColor, this.uiScale, colorPickerFadeKeyBase, ColorPickerBody.Metrics.rowPicker(), new ColorPickerBody.Layers(PICKER_KNOB_LAYER, PopoverHost.CONTENT_LAYER, PopoverHost.CONTENT_LAYER, PopoverHost.PANEL_LAYER));
        this.speedField = new SpeedField();
        this.textFields = new AbstractTextEditWidget[] {this.speedField};
    }

    private static float speed01(final int speed) {
        return Math.max(0f, Math.min(1f, speed / 100f));
    }

    private static int checkerColor(final int col, final int row, final int dark, final int light) {
        return ((col + row) & 1) == 0 ? dark : light;
    }

    private void mutate(final Consumer<WLColor> mutator) {
        final WLColor updated = this.option.getValue().copy();
        mutator.accept(updated);
        this.option.setValue(updated);
    }

    private void applySwatchColor(final int argb) {
        final WLColor current = this.option.getValue();
        final WLColor fresh = new WLColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, current.getAlpha());
        fresh.setAdditions(current.getAdditions());
        this.option.setValue(fresh);
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final WLColor color = this.option.getValue();
        this.backingH = this.rowLabelHeight() * 0.65f;
        this.backingW = this.backingH * 1.9f;
        this.backingX = this.getX() + this.getWidth() - PADDING * this.uiScale - this.backingW;
        this.backingY = this.getY() + (this.rowLabelHeight() - this.backingH) / 2f;
        final boolean hovered = available && this.isOverSwatch(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(this.isOverSwatch(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY), available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        if (this.pickerActive) {
            this.hoverAnim = 0f;
            return;
        }
        final float opacity = this.bandAlpha();
        final float saturation = this.availability();
        final float pop = this.hoverAnim * HOVER_POP * this.uiScale;
        final float bx0 = this.backingX - pop;
        final float by0 = this.backingY - pop;
        final float bx1 = this.backingX + this.backingW + pop;
        final float by1 = this.backingY + this.backingH + pop;
        graphics.glass()
            .dimensions(bx0, by0, bx1 - bx0, by1 - by0)
            .cornerRadius((this.backingH + pop * 2f) / 2f)
            .style(this.swatchBackingStyle.scaled(this.uiScale).opacity(opacity))
            .extract();
        this.patternReveal = MathUtils.approach(this.patternReveal, 1f, PATTERN_SPEED, dt);
        final int rgb = ColorUtils.lerpColor(Theme.get().trackFillSolid() | 0xFF000000, color.getRGB() | 0xFF000000, saturation);
        this.extractCheckerboardCapsule(graphics, bx0, by0, bx1, by1, opacity, rgb, color.getAlpha() / 255f, this.patternReveal);
    }

    private void extractCheckerboardCapsule(final WLGraphics2D graphics, final float x0, final float y0, final float x1, final float y1, final float alpha, final int targetRgb, final float colorAlpha, final float pattern) {
        final float r = (y1 - y0) / 2f;
        if (r <= 0f || x1 - x0 <= 0f) {
            return;
        }
        final int flat = ColorUtils.lerpColor(CHECKER_MEAN(), targetRgb, colorAlpha);
        final int light = ColorUtils.withAlpha(ColorUtils.lerpColor(flat, ColorUtils.lerpColor(CHECKER_LIGHT(), targetRgb, colorAlpha), pattern), alpha);
        final int dark = ColorUtils.withAlpha(ColorUtils.lerpColor(flat, ColorUtils.lerpColor(CHECKER_DARK(), targetRgb, colorAlpha), pattern), alpha);
        final float cell = r;
        final float gx0 = x0 + r;
        final float gx1 = x1 - r;
        final float centerY = (y0 + y1) / 2f;
        final int cols = Math.max(1, (int) Math.ceil((gx1 - gx0) / cell));
        this.extractTintedCheckerCircle(graphics, x0 + r, centerY, r, dark, light, false);
        final boolean flip = (cols & 1) != 0;
        this.extractTintedCheckerCircle(graphics, x1 - r, centerY, r, dark, light, flip);
        for (int col = 0; col < cols; col++) {
            final float cx0 = gx0 + col * cell;
            final float cx1 = Math.min(gx1, cx0 + cell);
            if (cx1 <= cx0) {
                continue;
            }
            graphics.fill(cx0, y0, cx1, y0 + r, checkerColor(col, 0, dark, light), null);
            graphics.fill(cx0, y0 + r, cx1, y1, checkerColor(col, 1, dark, light), null);
        }
    }

    private void extractTintedCheckerCircle(final WLGraphics2D graphics, final float cx, final float cy, final float r, final int dark, final int light, final boolean flip) {
        final int tl = flip ? dark : light;
        final int tr = flip ? light : dark;
        final int br = flip ? dark : light;
        final int bl = flip ? light : dark;
        this.extractCheckerCircleQuadrant(graphics, cx, cy, r, cx - r, cy - r, tl);
        this.extractCheckerCircleQuadrant(graphics, cx, cy, r, cx, cy - r, tr);
        this.extractCheckerCircleQuadrant(graphics, cx, cy, r, cx, cy, br);
        this.extractCheckerCircleQuadrant(graphics, cx, cy, r, cx - r, cy, bl);
    }

    private void extractCheckerCircleQuadrant(final WLGraphics2D graphics, final float cx, final float cy, final float r, final float qx0, final float qy0, final int color) {
        final float margin = 1f;
        final boolean leftHalf = qx0 < cx;
        final boolean topHalf = qy0 < cy;
        final float sx0f = leftHalf ? qx0 - margin : qx0;
        final float sx1f = leftHalf ? qx0 + r : qx0 + r + margin;
        final float sy0f = topHalf ? qy0 - margin : qy0;
        final float sy1f = topHalf ? qy0 + r : qy0 + r + margin;
        final int sx0 = (int) Math.floor(sx0f);
        final int sy0 = (int) Math.floor(sy0f);
        final int sx1 = (int) Math.ceil(sx1f);
        final int sy1 = (int) Math.ceil(sy1f);
        graphics.enableScissor(sx0, sy0, sx1, sy1);
        try {
            graphics.fillRoundedGradient(cx - r, cy - r, cx + r, cy + r, color, color, color, color, r);
        } finally {
            graphics.disableScissor();
        }
    }

    private boolean isOverSwatch(final double mouseX, final double mouseY) {
        final float slop = 2f * this.uiScale;
        return mouseX >= this.backingX - slop && mouseX <= this.backingX + this.backingW + slop && mouseY >= this.backingY - slop && mouseY <= this.backingY + this.backingH + slop;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || this.ctx == null || this.ctx.onOpenPopover() == null) {
            return false;
        }
        if (!this.isOverSwatch(event.x(), event.y())) {
            return false;
        }
        this.pickerActive = true;
        final LiquidRect anchor = new LiquidRect(this.backingX, this.backingY, this.backingW, this.backingH, this.backingH / 2f);
        this.ctx.onOpenPopover().accept(anchor, new PickerPopover());
        return true;
    }

    private final class PickerPopover implements PopoverHost.PopoverContent {
        private static final int SECTIONS = 9;
        private Drag drag = Drag.NONE;
        private float dragT;
        private boolean chromaPressed;
        private boolean chromaDragStarted;
        private float chromaPressX;

        private enum Drag { NONE, HUE, BOX, ALPHA, SPEED, CHROMA }

        private float s() {
            return ColorOptionWidget.this.uiScale;
        }

        private float pad() {
            return PICKER_PAD * this.s();
        }

        private float gap() {
            return PICKER_GAP * this.s();
        }

        private boolean showAlphaRow() {
            return ColorOptionWidget.this.option.isAlphaEditable();
        }

        private boolean showChromaRow() {
            return ColorOptionWidget.this.option.isChromaEditable();
        }

        private boolean showSpeedRow() {
            return this.showChromaRow() && ColorOptionWidget.this.option.getValue().isRainbow();
        }

        private float boxY0(final int panelY) {
            return panelY + this.pad();
        }

        private float boxY1(final int panelY, final int panelW) {
            return this.boxY0(panelY) + (panelW - this.pad() * 2f) * BOX_ASPECT;
        }

        private float hueY(final int panelY, final int panelW) {
            return this.boxY1(panelY, panelW) + this.gap();
        }

        private float opacitySliderY(final int panelY, final int panelW) {
            return this.hueY(panelY, panelW) + HUE_H * this.s() + this.gap();
        }

        private float chromaY(final int panelY, final int panelW) {
            final float y = this.opacitySliderY(panelY, panelW);
            return this.showAlphaRow() ? y + HUE_H * this.s() + this.gap() : y;
        }

        private float speedY(final int panelY, final int panelW) {
            final float y = this.chromaY(panelY, panelW);
            return this.showChromaRow() ? y + CHROMA_ROW_H * this.s() + 4f * this.s() : y;
        }

        private float hexY(final int panelY, final int panelW) {
            final float y = this.speedY(panelY, panelW);
            return this.showSpeedRow() ? y + SLIDER_ROW_H * this.s() + this.gap() : y;
        }

        private float rgbY(final int panelY, final int panelW) {
            return this.hexY(panelY, panelW) + HEX_ROW_H * this.s() + this.gap();
        }

        private float swatchSize(final int panelW) {
            final float contentWidth = panelW - this.pad() * 2f;
            return (contentWidth - (ColorPickerBody.SWATCH_COLUMNS - 1) * SWATCH_GAP * this.s()) / ColorPickerBody.SWATCH_COLUMNS;
        }

        private float popularLabelY(final int panelY, final int panelW) {
            return this.rgbY(panelY, panelW) + HEX_ROW_H * this.s() + this.gap();
        }

        private float popularSwatchY(final int panelY, final int panelW) {
            return this.popularLabelY(panelY, panelW) + SWATCH_ROW_LABEL_H * this.s() + SWATCH_LABEL_GAP * this.s();
        }

        private float favoritesLabelY(final int panelY, final int panelW) {
            return this.popularSwatchY(panelY, panelW) + this.swatchSize(panelW) + this.gap();
        }

        private float favoritesSwatchY(final int panelY, final int panelW) {
            return this.favoritesLabelY(panelY, panelW) + SWATCH_ROW_LABEL_H * this.s() + SWATCH_LABEL_GAP * this.s();
        }

        private float chromaSwitchX0(final float x1) {
            return x1 - SWITCH_W * this.s();
        }

        private float chromaHalfRestW() {
            final float restH = SWITCH_H * this.s() - SWITCH_KNOB_INSET * this.s() * 2f;
            return restH * SWITCH_KNOB_RATIO / 2f;
        }

        private float chromaTravelMin(final float x1) {
            return this.chromaSwitchX0(x1) + SWITCH_KNOB_INSET * this.s() + this.chromaHalfRestW();
        }

        private float chromaTravelMax(final float x1) {
            return x1 - SWITCH_KNOB_INSET * this.s() - this.chromaHalfRestW();
        }

        private float speedTrackX1(final float x1) {
            return x1 - SPEED_FIELD_RESERVE * this.s() - SPEED_FIELD_GAP * this.s();
        }

        @Override
        public int preferredWidth() {
            return Math.round(PICKER_WIDTH * this.s());
        }

        @Override
        public int preferredHeight() {
            final int w = this.preferredWidth();
            return Math.round(this.favoritesSwatchY(0, w) + this.swatchSize(w) + this.pad());
        }

        @Override
        public GlassStyle originStyle() {
            final WLColor c = ColorOptionWidget.this.option.getValue();
            final int apparent = ColorUtils.lerpColor(CHECKER_MEAN(), c.getRGB() | 0xFF000000, c.getAlpha() / 255f);
            return ColorOptionWidget.this.swatchStyle.scaled(ColorOptionWidget.this.uiScale)
                .layer(PopoverHost.PANEL_LAYER)
                .tint(apparent & 0x00FFFFFF, SWATCH_TINT_BOOST);
        }

        @Override
        public int bloomColor() {
            return 0x50000000 | (ColorOptionWidget.this.option.getValue().getRGB() & 0x00FFFFFF);
        }

        @Override
        public boolean growFromTop() {
            return true;
        }

        @Override
        public void onClosed() {
            ColorOptionWidget.this.pickerActive = false;
            ColorOptionWidget.this.patternReveal = 0f;
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final ColorPickerBody colorPickerBody = ColorOptionWidget.this.colorPickerBody;
            final WLColor color = ColorOptionWidget.this.option.getValue();
            final float x0 = panelX + this.pad();
            final float x1 = panelX + panelW - this.pad();
            final float cx = (x0 + x1) / 2f;
            final float dtSeconds = delta / 20f;
            int section = 0;
            float a;
            final float by0 = this.boxY0(panelY);
            final float by1 = this.boxY1(panelY, panelW);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, (by0 + by1) / 2f, this.s());
            colorPickerBody.extractBox(graphics, dtSeconds, x0, x1, by0, by1, this.drag == Drag.BOX, mouseX, mouseY, a);
            Materialize.exit(graphics.pose());
            final float hy0 = this.hueY(panelY, panelW);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, hy0 + HUE_H * this.s() / 2f, this.s());
            colorPickerBody.extractHueStrip(graphics, dtSeconds, x0, x1, hy0, this.drag == Drag.HUE, mouseX, mouseY, a);
            Materialize.exit(graphics.pose());
            if (this.showAlphaRow()) {
                final float sliderY = this.opacitySliderY(panelY, panelW);
                a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, sliderY + HUE_H * this.s() / 2f, this.s());
                this.extractAlphaSlider(graphics, dtSeconds, color, x0, x1, sliderY, this.drag == Drag.ALPHA ? this.dragT : color.getAlpha() / 255f, this.drag == Drag.ALPHA, a);
                Materialize.exit(graphics.pose());
            } else {
                section++;
            }
            if (this.showChromaRow()) {
                final float chY = this.chromaY(panelY, panelW);
                a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, chY + CHROMA_ROW_H * this.s() / 2f, this.s());
                this.extractChromaRow(graphics, dtSeconds, color, x0, x1, chY, a);
                Materialize.exit(graphics.pose());
            } else {
                section++;
            }
            if (this.showSpeedRow()) {
                final float spY = this.speedY(panelY, panelW);
                final float speedX1 = this.speedTrackX1(x1);
                a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, spY + SLIDER_ROW_H * this.s() / 2f, this.s());
                this.extractSlider(graphics, dtSeconds, x0, speedX1, spY, this.drag == Drag.SPEED ? this.dragT : speed01(color.getRainbowSpeed()), this.drag == Drag.SPEED, ColorOptionWidget.this.speedKnob, a);
                final SpeedField speed = ColorOptionWidget.this.speedField;
                speed.setX(Math.round(speedX1 + SPEED_FIELD_GAP * this.s()));
                speed.setY(Math.round(spY));
                speed.setWidth(Math.round(SPEED_FIELD_RESERVE * this.s()));
                speed.setHeight(Math.round(SLIDER_ROW_H * this.s()));
                if (!speed.isFocused()) {
                    speed.setValue(String.valueOf(color.getRainbowSpeed()));
                }
                speed.setAlpha(a);
                graphics.widget(speed, mouseX, mouseY, delta);
                Materialize.exit(graphics.pose());
            } else {
                section++;
            }
            final float hy = this.hexY(panelY, panelW);
            final float hexH = HEX_ROW_H * this.s();
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, hy + hexH / 2f, this.s());
            colorPickerBody.extractHexField(graphics, x0, hy, x1 - x0, hexH, mouseX, mouseY, delta, a);
            Materialize.exit(graphics.pose());
            final float rgy = this.rgbY(panelY, panelW);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, rgy + HEX_ROW_H * this.s() / 2f, this.s());
            colorPickerBody.extractRgbRow(graphics, x0, x1, rgy, HEX_ROW_H * this.s(), mouseX, mouseY, delta, a);
            Materialize.exit(graphics.pose());
            final float swSize = this.swatchSize(panelW);
            final float pLabelY = this.popularLabelY(panelY, panelW);
            final float pSwatchY = this.popularSwatchY(panelY, panelW);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, pLabelY + (SWATCH_ROW_LABEL_H * this.s() + SWATCH_LABEL_GAP * this.s() + swSize) / 2f, this.s());
            colorPickerBody.extractSwatchLabel(graphics, "Main", x0, pLabelY, a);
            colorPickerBody.extractPopularRow(graphics, dtSeconds, x0, pSwatchY, swSize, mouseX, mouseY, a);
            Materialize.exit(graphics.pose());
            final float fLabelY = this.favoritesLabelY(panelY, panelW);
            final float fSwatchY = this.favoritesSwatchY(panelY, panelW);
            a = Materialize.enter(graphics.pose(), Materialize.stagger(reveal, section++, SECTIONS), cx, fLabelY + (SWATCH_ROW_LABEL_H * this.s() + SWATCH_LABEL_GAP * this.s() + swSize) / 2f, this.s());
            colorPickerBody.extractFavoritesLabel(graphics, x0, fLabelY, mouseX, mouseY, a);
            colorPickerBody.extractFavoritesRow(graphics, dtSeconds, x0, fSwatchY, swSize, mouseX, mouseY, a);
            Materialize.exit(graphics.pose());
        }

        private void extractAlphaSlider(final WLGraphics2D graphics, final float dt, final WLColor color, final float x0, final float x1, final float rowY, final float target01, final boolean dragging, final float reveal) {
            final float y0 = WLScreen.snapPhysical(rowY);
            final float y1 = WLScreen.snapPhysical(y0 + HUE_H * this.s());
            final float r = (y1 - y0) / 2f;
            final int rgb = color.getRGB() | 0xFF000000;
            final float gx0 = x0 + r;
            final float gx1 = x1 - r;
            WLRenderSystem.contentStratum().beginPopover();
            try {
                final int light = ColorUtils.withAlpha(CHECKER_LIGHT(), reveal);
                final int dark = ColorUtils.withAlpha(CHECKER_DARK(), reveal);
                final float cell = ALPHA_CHECKER_CELL * this.s();
                final int cols = Math.max(1, (int) Math.ceil((gx1 - gx0) / cell));
                final float centerY = (y0 + y1) / 2f;
                graphics.fillCheckerCircle(x0 + r, centerY, r, reveal, false);
                for (int col = 0; col < cols; col++) {
                    final float cx0 = gx0 + col * cell;
                    final float cx1 = Math.min(gx1, cx0 + cell);
                    if (cx1 <= cx0) {
                        continue;
                    }
                    graphics.fill(cx0, y0, cx1, y0 + r, checkerColor(col, 0, dark, light), null);
                    graphics.fill(cx0, y0 + r, cx1, y1, checkerColor(col, 1, dark, light), null);
                }
            } finally {
                WLRenderSystem.contentStratum().endPopover();
            }
            WLRenderSystem.contentStratum().beginPopover();
            try {
                final int transparent = ColorUtils.withAlpha(rgb, 0f);
                final int opaque = ColorUtils.withAlpha(rgb, reveal);
                graphics.fillRoundedGradient(gx0, y0, gx1, y1, transparent, opaque, opaque, transparent, 0f);
                graphics.fillRoundedGradient(x1 - 2f * r, y0, x1, y1, opaque, opaque, opaque, opaque, r);
            } finally {
                WLRenderSystem.contentStratum().endPopover();
            }
            final float centerY = (y0 + y1) / 2f;
            final float insetX = HUE_KNOB_REST_DIAMETER * this.s() / 2f;
            ColorOptionWidget.this.alphaKnob.extract(graphics, dt, target01, 0.5f, dragging, false, x0 + insetX, x1 - insetX, centerY, centerY, reveal);
        }

        private void extractSlider(final WLGraphics2D graphics, final float dt, final float x0, final float x1, final float y, final float target01, final boolean dragging, final LiquidKnob knob, final float reveal) {
            final float h = SLIDER_TRACK_H * this.s();
            final float sy0 = y + (SLIDER_ROW_H * this.s() - h) / 2f;
            graphics.glass()
                .dimensions(x0, sy0, x1 - x0, h)
                .cornerRadius(h / 2f)
                .style(ColorOptionWidget.this.trackStyle.copy().scaled(this.s()).opacity(reveal))
                .extract();
            final float restH = SLIDER_KNOB_REST_H * this.s();
            final float travelMin = x0 + restH * SLIDER_KNOB_RATIO / 2f;
            final float travelMax = x1 - restH * SLIDER_KNOB_RATIO / 2f;
            knob.extract(graphics, dt, target01, dragging, dragging, travelMin, travelMax, sy0 + h / 2f, restH, SLIDER_KNOB_PRESS_H * this.s(), reveal);
            final float fillW = travelMin + (travelMax - travelMin) * Mth.clamp(knob.position01(), 0f, 1f) - x0;
            graphics.glass()
                .dimensions(x0, sy0, fillW, h)
                .cornerRadius(h / 2f)
                .style(ColorOptionWidget.this.fillStyle.copy().scaled(this.s()).opacity(reveal))
                .extract();
        }

        private void extractChromaRow(final WLGraphics2D graphics, final float dt, final WLColor color, final float x0, final float x1, final float y, final float reveal) {
            final float textSize = LABEL_SIZE * this.s();
            final float cap = textSize * 0.59043f;
            final float baseline = y + CHROMA_ROW_H * this.s() / 2f + cap * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, "Chroma", x0, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(Theme.get().label(), reveal));
            final float swW = SWITCH_W * this.s();
            final float swH = SWITCH_H * this.s();
            final float sx0 = this.chromaSwitchX0(x1);
            final float syc = y + CHROMA_ROW_H * this.s() / 2f;
            final float restH = swH - SWITCH_KNOB_INSET * this.s() * 2f;
            final boolean dragging = this.drag == Drag.CHROMA && this.chromaPressed && this.chromaDragStarted;
            final float knobTarget = dragging ? this.dragT : (color.isRainbow() ? 1f : 0f);
            ColorOptionWidget.this.chromaKnob.extract(graphics, dt, knobTarget, this.chromaPressed, dragging, this.chromaTravelMin(x1), this.chromaTravelMax(x1), syc, restH, swH + SWITCH_KNOB_OUTSET * 2f * this.s(), reveal);
            final GlassStyle track = GlassStyle.lerp(ColorOptionWidget.this.switchOffStyle, ColorOptionWidget.this.switchOnStyle, ColorOptionWidget.this.chromaKnob.position01())
                .layer(PICKER_TRACK_LAYER)
                .fadeKey(ColorOptionWidget.this.switchTrackFadeKey)
                .scaled(this.s())
                .opacity(reveal);
            graphics.glass().dimensions(sx0, syc - swH / 2f, swW, swH).cornerRadius(swH / 2f).style(track).extract();
        }

        private float alphaT(final double mouseX, final float x0, final float x1) {
            return Mth.clamp((float) ((mouseX - x0) / (x1 - x0)), 0f, 1f);
        }

        private float sliderT(final double mouseX, final float x0, final float x1) {
            final float restH = SLIDER_KNOB_REST_H * this.s();
            final float travelMin = x0 + restH * SLIDER_KNOB_RATIO / 2f;
            final float travelMax = x1 - restH * SLIDER_KNOB_RATIO / 2f;
            return Mth.clamp((float) ((mouseX - travelMin) / (travelMax - travelMin)), 0f, 1f);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (event.button() != InputConstants.MOUSE_BUTTON_LEFT && event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            final ColorPickerBody colorPickerBody = ColorOptionWidget.this.colorPickerBody;
            final double mx = event.x();
            final double my = event.y();
            final float x0 = panelX + this.pad();
            final float x1 = panelX + panelW - this.pad();
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                colorPickerBody.handleFavoritesRowClick(event, mx, my, x0, this.favoritesSwatchY(panelY, panelW), this.swatchSize(panelW));
                return true;
            }
            final WLColor color = ColorOptionWidget.this.option.getValue();
            if (colorPickerBody.fieldClicked(event, doubleClick, mx, my)) {
                for (final AbstractTextEditWidget field : ColorOptionWidget.this.textFields) {
                    field.setFocused(false);
                }
                return true;
            }
            AbstractTextEditWidget clicked = null;
            for (final AbstractTextEditWidget field : ColorOptionWidget.this.textFields) {
                if (field == ColorOptionWidget.this.speedField && !this.showSpeedRow()) {
                    continue;
                }
                if (mx >= field.getX() && mx < field.getX() + field.getWidth() && my >= field.getY() && my < field.getY() + field.getHeight()) {
                    clicked = field;
                }
            }
            colorPickerBody.clearFieldFocus();
            for (final AbstractTextEditWidget field : ColorOptionWidget.this.textFields) {
                field.setFocused(field == clicked);
            }
            if (clicked != null) {
                clicked.onClick(event, doubleClick);
                return true;
            }
            final float by0 = this.boxY0(panelY);
            final float by1 = this.boxY1(panelY, panelW);
            if (my >= by0 && my < by1 && mx >= x0 && mx < x1) {
                this.drag = Drag.BOX;
                colorPickerBody.applyBox(mx, my, x0, x1, by0, by1);
                return true;
            }
            final float hy = this.hueY(panelY, panelW);
            if (my >= hy && my < hy + HUE_H * this.s() && mx >= x0 && mx < x1) {
                this.drag = Drag.HUE;
                colorPickerBody.applyHue(mx, x0, x1);
                return true;
            }
            final float ay = this.opacitySliderY(panelY, panelW);
            if (ColorOptionWidget.this.option.isAlphaEditable() && my >= ay && my < ay + HUE_H * this.s() && mx >= x0 && mx < x1) {
                this.drag = Drag.ALPHA;
                this.dragT = this.alphaT(mx, x0, x1);
                ColorOptionWidget.this.mutate(c -> c.setAlpha(Math.round(this.dragT * 255f)));
                return true;
            }
            final float cy = this.chromaY(panelY, panelW);
            final float syc = cy + CHROMA_ROW_H * this.s() / 2f;
            final float swH = SWITCH_H * this.s();
            final float chromaSlop = 2f * this.s();
            if (this.showChromaRow() && mx >= this.chromaSwitchX0(x1) - chromaSlop && mx <= x1 + chromaSlop && my >= syc - swH / 2f - chromaSlop && my <= syc + swH / 2f + chromaSlop) {
                this.drag = Drag.CHROMA;
                this.chromaPressed = true;
                this.chromaDragStarted = false;
                this.chromaPressX = (float) mx;
                return true;
            }
            final float sy = this.speedY(panelY, panelW);
            final float speedX1 = this.speedTrackX1(x1);
            if (this.showSpeedRow() && my >= sy && my < sy + SLIDER_ROW_H * this.s() && mx >= x0 && mx < speedX1) {
                this.drag = Drag.SPEED;
                this.dragT = this.sliderT(mx, x0, speedX1);
                ColorOptionWidget.this.mutate(c -> c.setRainbowSpeed(Math.round(this.dragT * 100f)));
                return true;
            }
            final float swSize = this.swatchSize(panelW);
            final float pSwatchY = this.popularSwatchY(panelY, panelW);
            if (colorPickerBody.handlePopularRowClick(event, mx, my, x0, pSwatchY, swSize)) {
                return true;
            }
            final float fSwatchY = this.favoritesSwatchY(panelY, panelW);
            if (colorPickerBody.handleFavoritesRowClick(event, mx, my, x0, fSwatchY, swSize)) {
                return true;
            }
            return true;
        }

        @Override
        public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
            final ColorPickerBody colorPickerBody = ColorOptionWidget.this.colorPickerBody;
            final double mx = event.x();
            final double my = event.y();
            final float x0 = panelX + this.pad();
            final float x1 = panelX + panelW - this.pad();
            switch (this.drag) {
                case BOX -> colorPickerBody.applyBox(mx, my, x0, x1, this.boxY0(panelY), this.boxY1(panelY, panelW));
                case HUE -> colorPickerBody.applyHue(mx, x0, x1);
                case ALPHA -> {
                    this.dragT = this.alphaT(mx, x0, x1);
                    ColorOptionWidget.this.mutate(c -> c.setAlpha(Math.round(this.dragT * 255f)));
                }
                case SPEED -> {
                    this.dragT = this.sliderT(mx, x0, this.speedTrackX1(x1));
                    ColorOptionWidget.this.mutate(c -> c.setRainbowSpeed(Math.round(this.dragT * 100f)));
                }
                case CHROMA -> {
                    if (!this.chromaDragStarted && Math.abs((float) mx - this.chromaPressX) > CHROMA_DRAG_START_THRESHOLD * this.s()) {
                        this.chromaDragStarted = true;
                    }
                    if (this.chromaDragStarted) {
                        final float travelMin = this.chromaTravelMin(x1);
                        final float travelMax = this.chromaTravelMax(x1);
                        this.dragT = Mth.clamp((float) ((mx - travelMin) / (travelMax - travelMin)), 0f, 1f);
                    }
                }
                default -> {
                    if (colorPickerBody.isFieldFocused()) {
                        return colorPickerBody.fieldDragged(event, dragX, dragY);
                    }
                    final AbstractTextEditWidget focused = this.focusedTextField();
                    return focused != null && focused.mouseDragged(event, dragX, dragY);
                }
            }
            return true;
        }

        @Override
        public boolean mouseReleased(final MouseButtonEvent event) {
            if (this.drag == Drag.CHROMA) {
                final boolean value = ColorOptionWidget.this.option.getValue().isRainbow();
                if (this.chromaDragStarted) {
                    final boolean next = this.dragT > 0.5f;
                    if (next != value) {
                        ColorOptionWidget.this.mutate(c -> c.setRainbow(next));
                    }
                } else if (this.chromaPressed) {
                    ColorOptionWidget.this.mutate(c -> c.setRainbow(!value));
                }
                this.chromaPressed = false;
                this.chromaDragStarted = false;
            }
            this.drag = Drag.NONE;
            return false;
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            final ColorPickerBody colorPickerBody = ColorOptionWidget.this.colorPickerBody;
            if (colorPickerBody.isFieldFocused()) {
                return colorPickerBody.fieldKeyPressed(event);
            }
            final AbstractTextEditWidget focused = this.focusedTextField();
            return focused != null && focused.keyPressed(event);
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            final ColorPickerBody colorPickerBody = ColorOptionWidget.this.colorPickerBody;
            if (colorPickerBody.isFieldFocused()) {
                return colorPickerBody.fieldCharTyped(event);
            }
            final AbstractTextEditWidget focused = this.focusedTextField();
            return focused != null && focused.charTyped(event);
        }

        private AbstractTextEditWidget focusedTextField() {
            for (final AbstractTextEditWidget field : ColorOptionWidget.this.textFields) {
                if (field.isFocused()) {
                    return field;
                }
            }
            return null;
        }
    }

    private final class SpeedField extends AbstractTextEditWidget {
        SpeedField() {
            super(0, 0, 1, 1);
            this.setMaxLength(3);
        }

        @Override
        protected void insertText(final String raw) {
            final StringBuilder digits = new StringBuilder(raw.length());
            for (int i = 0; i < raw.length(); i++) {
                final char c = raw.charAt(i);
                if (Character.isDigit(c)) {
                    digits.append(c);
                }
            }
            super.insertText(digits.toString());
        }

        @Override
        protected void onTextChanged() {
            final String text = this.getValue();
            if (text.isEmpty()) {
                return;
            }
            final int parsed;
            try {
                parsed = Integer.parseInt(text);
            } catch (final NumberFormatException ignored) {
                return;
            }
            final int clamped = Math.max(0, Math.min(100, parsed));
            ColorOptionWidget.this.mutate(c -> c.setRainbowSpeed(clamped));
        }

        @Override
        protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
            final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
            if (alpha <= 0.001f) {
                return;
            }
            final float dt = WLRenderSystem.frameDeltaSeconds();
            this.extractFieldGlass(graphics, ColorOptionWidget.this.speedFieldStyle, alpha, ColorOptionWidget.this.uiScale, dt);
            final float textStartX = this.getX() + 4f * ColorOptionWidget.this.uiScale;
            final float innerWidth = Math.max(4f, this.getWidth() - 8f * ColorOptionWidget.this.uiScale);
            final float size = Math.min(LABEL_SIZE * ColorOptionWidget.this.uiScale, this.getHeight() * 0.55f);
            final String visible = this.layoutText(textStartX, innerWidth, size);
            final float capHeight = size * 0.7f;
            final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
            final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
            if (!visible.isEmpty()) {
                Fonts.SFPRO_DISPLAY.extract(graphics, visible, textStartX, textTop, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
            }
            this.extractCaretAndSelection(graphics, "", textStartX, size, alpha, ColorOptionWidget.this.uiScale);
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
