package main.walksy.lib.core.gui.widget.options.control;

import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.ScrollGlide;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.liquid.MorphProfile;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.ModWidget;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;
import net.minecraft.util.Mth;

public final class PillSelectorWidget<T> {
    private static final float TEXT_INSET = 8f;
    private static final float HOVER_SPEED = 14f;
    private static final float ROW_HOVER_HIGHLIGHT = 0.5f;
    private static final float ROW_INSET_X = 4f;
    private static final float ROW_INSET_Y = 3f;
    private static final float DIM_ALPHA = 0.75f;
    private static final float MATERIALIZE_IN_SPEED = 20f;
    private static final float MATERIALIZE_OUT_SPEED = 12f;
    private static final int GLASS_BLUR = LiquidGlassConstants.BLUR_POPOVER;
    private static final float SCROLL_GLIDE_SPEED = 18f;
    private static final float SCROLL_BAR_W = 1.5f;
    private static final float SCROLL_BAR_INSET = 2.5f;
    private static final float SCROLL_BAR_END_INSET = 6f;
    private final Supplier<T> value;
    private final Consumer<T> onChange;
    private final List<T> values;
    private final Function<T, String> label;
    private final GlassStyle pillStyle;
    private final float uiScale;
    private final int panelLayer;
    private final RowContext.OpenAt openAt;
    private final Runnable closePopover;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;
    private float screenX0;
    private float screenY0;
    private float screenW;
    private float screenH;
    private boolean localAnchors;
    private int maxVisibleRows = Integer.MAX_VALUE;
    private boolean popoverActive;
    private float landing;
    private float hoverAnim;

    public PillSelectorWidget(final Supplier<T> value, final Consumer<T> onChange, final List<T> values, final Function<T, String> label, final GlassStyle pillStyle, final float uiScale, final int panelLayer, final RowContext.OpenAt openAt, final Runnable closePopover) {
        this.value = value;
        this.onChange = onChange;
        this.values = values;
        this.label = label;
        this.pillStyle = pillStyle;
        this.uiScale = uiScale;
        this.panelLayer = panelLayer;
        this.openAt = openAt;
        this.closePopover = closePopover;
    }

    public PillSelectorWidget<T> localAnchors() {
        this.localAnchors = true;
        return this;
    }

    public PillSelectorWidget<T> maxVisibleRows(final int rows) {
        this.maxVisibleRows = Math.max(1, rows);
        return this;
    }

    public static GlassStyle defaultStyle() {
        return ResetButton.knobStyle(ControlLayers.KNOB);
    }

    private float widestValueWidth(final float textSize) {
        float widest = 0f;
        for (final T v : this.values) {
            widest = Math.max(widest, Fonts.SFPRO_DISPLAY.width(this.label.apply(v), textSize));
        }
        return widest;
    }

    public float preferredWidth() {
        final float size = OptionRowWidget.LABEL_SIZE * this.uiScale;
        return this.widestValueWidth(size) + size * 0.7f + 5f * this.uiScale + 16f * this.uiScale;
    }

    public void extract(final WLGraphics2D graphics, final float alpha, final float x1, final float rowY, final float rowHeight, final int mouseX, final int mouseY, final float delta) {
        final float pillH = 16f * this.uiScale;
        final float pillY = rowY + (rowHeight - pillH) / 2f;
        final String name = this.label.apply(this.value.get());
        final float size = OptionRowWidget.LABEL_SIZE * this.uiScale;
        final float chevronSize = size * 0.7f;
        final float pillW = this.preferredWidth();
        final float pillX0 = x1 - pillW;
        this.pillX0 = pillX0;
        this.pillY0 = pillY;
        this.pillW = pillW;
        this.pillH = pillH;
        final var pose = graphics.pose();
        final boolean posed = !this.localAnchors && DevicePixels.isAxisAligned(pose);
        this.screenX0 = posed ? pose.m00() * pillX0 + pose.m20() : pillX0;
        this.screenY0 = posed ? pose.m11() * pillY + pose.m21() : pillY;
        this.screenW = posed ? pose.m00() * pillW : pillW;
        this.screenH = posed ? pose.m11() * pillH : pillH;
        final boolean hovered = !this.popoverActive && this.isOverPill(mouseX, mouseY);
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, delta / 20f);
        if (this.popoverActive) {
            if (this.landing > 0.001f) {
                graphics.glass()
                    .dimensions(pillX0, pillY, pillW, pillH)
                    .cornerRadius(pillH / 2f)
                    .style(this.pillStyle.scaled(this.uiScale).opacity(alpha * this.landing))
                    .hover(this.hoverAnim)
                    .extract();
            }
            return;
        }
        graphics.glass()
            .dimensions(pillX0, pillY, pillW, pillH)
            .cornerRadius(pillH / 2f)
            .style(this.pillStyle.scaled(this.uiScale).opacity(alpha))
            .hover(this.hoverAnim)
            .extract();
        ResetButton.lift(this.pillStyle.getLayer() > PopoverHost.PANEL_LAYER, this.pillStyle.getLayer(), () -> this.extractPillLabel(graphics, name, size, chevronSize, alpha));
    }

    private void extractPillLabel(final WLGraphics2D graphics, final String name, final float size, final float chevronSize, final float alpha) {
        final float capHeight = size * 0.59043f;
        final float baseline = this.pillY0 + this.pillH / 2f + capHeight * 0.5f;
        final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size);
        final float textX = this.pillX0 + TEXT_INSET * this.uiScale;
        Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, name, textX, textTop, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
        final float chevronX = this.pillX0 + this.pillW - 8f * this.uiScale - chevronSize;
        final float chevronY = this.pillY0 + (this.pillH - chevronSize) / 2f;
        Icons.CHEVRON.extractRotated(graphics, chevronX, chevronY, chevronSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha), 90f);
    }

    public boolean isOverPill(final double mouseX, final double mouseY) {
        return mouseX >= this.pillX0 && mouseX < this.pillX0 + this.pillW && mouseY >= this.pillY0 && mouseY < this.pillY0 + this.pillH;
    }

    private GlassStyle flatBase() {
        return this.pillStyle.copy()
            .layer(this.panelLayer)
            .outlineDirectional(Theme.get().outlineWidth(), Theme.get().shadowColor(), 0f, 0f, 0f, 0f);
    }

    private GlassStyle glassBase() {
        return LiquidKnob.bevelKnobStyle(this.panelLayer).blurRadius(GLASS_BLUR).fadeKey(this.pillStyle.getFadeKey());
    }

    private LiquidRect screenRect() {
        return new LiquidRect(this.screenX0, this.screenY0, this.screenW, this.screenH, this.screenH / 2f);
    }

    public boolean isOpen() {
        return this.popoverActive;
    }

    public void open() {
        if (this.popoverActive) {
            return;
        }
        final T current = this.value.get();
        final List<T> reordered = new ArrayList<>(this.values.size());
        reordered.add(current);
        for (final T v : this.values) {
            if (!v.equals(current)) {
                reordered.add(v);
            }
        }
        final LiquidRect anchor = this.screenRect();
        final float rowHeight = this.pillH * 1.4f;
        final float textSize = OptionRowWidget.LABEL_SIZE * this.uiScale;
        final float targetX = this.screenX0;
        final float targetY = this.screenY0 + this.screenH / 2f - rowHeight / 2f;
        this.popoverActive = true;
        this.openAt.accept(anchor, targetX, targetY, new ValueListPopover(reordered, rowHeight, textSize));
    }

    private final class ValueListPopover implements PopoverHost.PopoverContent {
        private static final MorphProfile DROPDOWN_MORPH = MorphProfile.dropdown();
        private final List<T> constants;
        private final float rowHeight;
        private final float textSize;
        private final float[] hoverAnim;
        private final GlassStyle flatBase;
        private final GlassStyle glassBase;
        private final GlassStyle hoverShade;
        private final ScrollGlide glide = new ScrollGlide();
        private float scrollY;
        private float scrollTarget;
        private float materialize;
        private float hostAlpha = 1f;
        private boolean closing;
        private T confirmedForClose;

        ValueListPopover(final List<T> constants, final float rowHeight, final float textSize) {
            this.constants = constants;
            this.rowHeight = rowHeight;
            this.textSize = textSize;
            this.hoverAnim = new float[constants.size()];
            this.flatBase = PillSelectorWidget.this.flatBase();
            this.glassBase = PillSelectorWidget.this.glassBase();
            this.hoverShade = ModWidget.rowStyle()
                .layer(PillSelectorWidget.this.panelLayer + 1)
                .fadeKey(PillSelectorWidget.this.pillStyle.getFadeKey() ^ 0x54ADEL);
        }

        @Override
        public GlassStyle originStyle() {
            final float m = Mth.clamp(this.materialize, 0f, 1f);
            final GlassStyle s = GlassStyle.lerp(this.flatBase, this.glassBase, m);
            s.blurRadius(m < 0.5f ? this.flatBase.getBlurRadius() : GLASS_BLUR);
            return s.scaled(PillSelectorWidget.this.uiScale);
        }

        @Override
        public boolean keepsOriginStyle() {
            return true;
        }

        @Override
        public boolean bouncyOpen() {
            return false;
        }

        @Override
        public boolean ridesContent() {
            return false;
        }

        @Override
        public MorphProfile morphProfile() {
            return DROPDOWN_MORPH;
        }

        @Override
        public boolean bridged() {
            return false;
        }

        @Override
        public void onCloseBegan() {
            this.closing = true;
            this.scrollTarget = 0f;
            this.confirmedForClose = PillSelectorWidget.this.value.get();
        }

        @Override
        public void onClosed() {
            PillSelectorWidget.this.popoverActive = false;
            PillSelectorWidget.this.landing = 0f;
        }

        @Override
        public LiquidRect liveAnchor() {
            return PillSelectorWidget.this.screenRect();
        }

        @Override
        public boolean landsOnAnchor() {
            return true;
        }

        @Override
        public void landing(final float amount) {
            PillSelectorWidget.this.landing = amount;
        }

        @Override
        public void hostAlpha(final float alpha) {
            this.hostAlpha = alpha;
        }

        @Override
        public int preferredWidth() {
            return Math.round(PillSelectorWidget.this.pillW);
        }

        @Override
        public int preferredHeight() {
            return Math.round(this.rowHeight * Math.min(this.constants.size(), PillSelectorWidget.this.maxVisibleRows));
        }

        private float maxScroll(final float panelH) {
            return Math.max(0f, this.rowHeight * this.constants.size() - panelH);
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final float dtSeconds = delta / 20f;
            final float max = this.maxScroll(panelH);
            if (max <= 0f) {
                this.extractRows(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, dtSeconds, reveal, 0f);
                return;
            }
            this.scrollTarget = Mth.clamp(this.scrollTarget, 0f, max);
            this.scrollY = Mth.clamp(this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dtSeconds), 0f, max);
            graphics.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
            try {
                this.extractRows(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, dtSeconds, reveal, ScrollGlide.shown(this.scrollY));
                this.extractScrollIndicator(graphics, panelX, panelY, panelW, panelH, max, reveal);
            } finally {
                graphics.disableScissor();
            }
        }

        private void extractScrollIndicator(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float max, final float alpha) {
            final float uiScale = PillSelectorWidget.this.uiScale;
            final float trackTop = y + SCROLL_BAR_END_INSET * uiScale;
            final float trackH = h - SCROLL_BAR_END_INSET * uiScale * 2f;
            if (trackH <= 0f || alpha <= 0.01f) {
                return;
            }
            final float thumbH = Math.max(this.rowHeight * 0.5f, trackH * (trackH / (trackH + max)));
            final float thumbY = trackTop + (trackH - thumbH) * Mth.clamp(this.scrollY / max, 0f, 1f);
            final float barW = SCROLL_BAR_W * uiScale;
            final float x0 = x + w - SCROLL_BAR_INSET * uiScale - barW;
            final int color = ColorUtils.withAlpha(Theme.get().label(), 0.28f * alpha);
            graphics.fillRoundedGradient(x0, thumbY, x0 + barW, thumbY + thumbH, color, color, color, color, barW / 2f);
        }

        private void extractRows(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float dtSeconds, final float reveal, final float scroll) {
            final float uiScale = PillSelectorWidget.this.uiScale;
            this.materialize = MathUtils.approach(this.materialize, this.closing ? 0f : 1f, this.closing ? MATERIALIZE_OUT_SPEED : MATERIALIZE_IN_SPEED, dtSeconds);
            final float capHeight = this.textSize * 0.59043f;
            final float rowInsetX = ROW_INSET_X * uiScale;
            final float rowInsetY = ROW_INSET_Y * uiScale;
            final float rowRadius = (this.rowHeight - rowInsetY * 2f) / 2f;
            for (int i = 0; i < this.constants.size(); i++) {
                if (this.confirmedForClose != null && i != 0 && this.constants.get(i).equals(this.confirmedForClose)) {
                    continue;
                }
                final float ry0 = panelY + i * this.rowHeight - scroll;
                if (ry0 + this.rowHeight <= panelY || ry0 >= panelY + panelH) {
                    continue;
                }
                final boolean selected = i == 0;
                final boolean hovered = !this.closing && mouseX >= panelX && mouseX < panelX + panelW && mouseY >= panelY && mouseY < panelY + panelH && mouseY >= ry0 && mouseY < ry0 + this.rowHeight;
                WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
                this.hoverAnim[i] = MathUtils.approach(this.hoverAnim[i], hovered ? 1f : 0f, HOVER_SPEED, dtSeconds);
                if (!selected && this.hoverAnim[i] > 0.001f) {
                    final float shadeAlpha = this.hoverShade.getOpacity() * reveal * this.hoverAnim[i] * ROW_HOVER_HIGHLIGHT;
                    graphics.glass()
                        .dimensions(panelX + rowInsetX, ry0 + rowInsetY, panelW - rowInsetX * 2f, this.rowHeight - rowInsetY * 2f)
                        .cornerRadius(rowRadius)
                        .style(this.hoverShade.scaled(uiScale).opacity(shadeAlpha))
                        .extract();
                }
                final String text = selected && this.confirmedForClose != null ? PillSelectorWidget.this.label.apply(this.confirmedForClose) : PillSelectorWidget.this.label.apply(this.constants.get(i));
                final float baseline = selected ? PillSelectorWidget.this.screenY0 + PillSelectorWidget.this.screenH / 2f + capHeight * 0.5f - scroll : ry0 + this.rowHeight / 2f + capHeight * 0.5f;
                final float top = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, this.textSize);
                final float textX = (selected ? PillSelectorWidget.this.screenX0 : panelX) + TEXT_INSET * uiScale;
                final float textAlpha = selected ? this.hostAlpha : reveal;
                final float brightness = Math.max(this.hoverAnim[i], selected ? 1f : 0f);
                final float colorAlpha = DIM_ALPHA + (1f - DIM_ALPHA) * brightness;
                final var font = selected ? Fonts.SFPRO_DISPLAY_MEDIUM : Fonts.SFPRO_DISPLAY;
                font.extract(graphics, text, textX, top, this.textSize, ColorUtils.withAlpha(Theme.get().label(), textAlpha * colorAlpha));
                if (selected) {
                    final float chevronSize = this.textSize * 0.7f;
                    final float chevronX = PillSelectorWidget.this.screenX0 + PillSelectorWidget.this.screenW - 8f * uiScale - chevronSize;
                    final float chevronY = PillSelectorWidget.this.screenY0 + (PillSelectorWidget.this.screenH - chevronSize) / 2f - scroll;
                    Icons.CHEVRON.extractRotated(graphics, chevronX, chevronY, chevronSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), this.hostAlpha), 90f);
                }
            }
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
            final float max = this.maxScroll(panelH);
            if (this.closing || max <= 0f) {
                return false;
            }
            this.scrollTarget = Mth.clamp(this.scrollTarget - (float) scrollY * this.rowHeight, 0f, max);
            return true;
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.closing) {
                return false;
            }
            if (event.y() < panelY || event.y() >= panelY + panelH) {
                return false;
            }
            final int row = (int) ((event.y() - panelY + this.scrollY) / this.rowHeight);
            if (row < 0 || row >= this.constants.size()) {
                return false;
            }
            PillSelectorWidget.this.onChange.accept(this.constants.get(row));
            if (PillSelectorWidget.this.closePopover != null) {
                PillSelectorWidget.this.closePopover.run();
            }
            return true;
        }
}
}
