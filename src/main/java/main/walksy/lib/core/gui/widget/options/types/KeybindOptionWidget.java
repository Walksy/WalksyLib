package main.walksy.lib.core.gui.widget.options.types;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class KeybindOptionWidget extends OptionRowWidget<WLKeyMapping> {
    private static final float HOVER_SPEED = 14f;
    private static final float PILL_HEIGHT_RATIO = 0.7f;
    private static final float PILL_H_PADDING = 8f;
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x4B3791L);
    private static final float GLYPH_RATIO = 0.62f;
    private static final float GLYPH_GAP = 4f;
    private static final int MAX_CONFLICT_LINES = 2;
    private static final float CONFLICT_DESATURATE = 0.35f;

    private static KeybindOptionWidget capturing;
    private final GlassStyle pillStyle = ResetButton.knobStyle(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
    private float hoverAnim;
    private boolean listening;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;

    public KeybindOptionWidget(final Option<WLKeyMapping> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
    }

    public static KeybindOptionWidget capturing() {
        return capturing;
    }

    public static void cancelCapture() {
        if (capturing != null) {
            capturing.listening = false;
            capturing = null;
        }
    }

    private static int conflictColor() {
        final int argb = Theme.get().destructive();
        final int a = (argb >>> 24) & 0xFF;
        final int r = (argb >> 16) & 0xFF;
        final int g = (argb >> 8) & 0xFF;
        final int b = argb & 0xFF;
        final float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;
        return (a << 24) | (Math.round(r + (luma - r) * CONFLICT_DESATURATE) << 16) | (Math.round(g + (luma - g) * CONFLICT_DESATURATE) << 8) | Math.round(b + (luma - b) * CONFLICT_DESATURATE);
    }

    private List<String> conflicts() {
        return WalksyLib.getInstance().getKeyBinds().conflictsFor(this.option.getValue());
    }

    private List<String> conflictLines() {
        final List<String> names = this.conflicts();
        if (names.isEmpty() || Fonts.SFPRO_DISPLAY == null) {
            return List.of();
        }
        final List<String> lines = this.wrapAt("Also bound to " + String.join(", ", names), this.conflictWidth(), DESCRIPTION_SIZE * this.uiScale);
        return lines.size() <= MAX_CONFLICT_LINES ? lines : lines.subList(0, MAX_CONFLICT_LINES);
    }

    private float conflictWidth() {
        return Math.max(1f, this.getWidth() - PADDING * this.uiScale * 2f);
    }

    @Override
    protected float extraBandHeight() {
        final int lines = this.conflictLines().size();
        return lines == 0 ? 0f : DESCRIPTION_TOP_GAP * this.uiScale + lines * DESCRIPTION_SIZE * this.uiScale * DESCRIPTION_LINE_SPACING + DESCRIPTION_BOTTOM_GAP * this.uiScale;
    }

    private void extractConflicts(final WLGraphics2D graphics) {
        final List<String> lines = this.conflictLines();
        if (lines.isEmpty()) {
            return;
        }
        final float size = DESCRIPTION_SIZE * this.uiScale;
        final float lineHeight = size * DESCRIPTION_LINE_SPACING;
        final int color = ColorUtils.withAlpha(conflictColor(), this.bandAlpha());
        final float x = this.getX() + PADDING * this.uiScale;
        float y = this.extraBandTop() + DESCRIPTION_TOP_GAP * this.uiScale;
        for (final String line : lines) {
            Fonts.SFPRO_DISPLAY.extract(graphics, line, x, Fonts.SFPRO_DISPLAY.topForBaseline(y + size * 0.8f, size), size, color);
            y += lineHeight;
        }
    }

    private String label() {
        if (this.listening) {
            return "Press a key...";
        }
        final WLKeyMapping keymapping = this.option.getValue();
        return keymapping.isUnbound() ? "Unbound" : keymapping.displayKeyMessage().getString();
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final boolean hovered = available && this.isOverPill(mouseX, mouseY) && !this.coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(hovered, available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered || this.listening ? 1f : 0f, HOVER_SPEED, dt);
        final float textSize = LABEL_SIZE * this.uiScale * 0.9f;
        final String text = this.label();
        final float textWidth = Fonts.SFPRO_DISPLAY.width(text, textSize);
        this.pillH = this.rowLabelHeight() * PILL_HEIGHT_RATIO;
        this.pillY0 = this.getY() + (this.rowLabelHeight() - this.pillH) / 2f;
        final float glyphSize = this.pillH * GLYPH_RATIO;
        final float glyphGap = GLYPH_GAP * this.uiScale;
        final float contentWidth = glyphSize + glyphGap + textWidth;
        this.pillW = Math.min(this.controlAreaWidth(), contentWidth + PILL_H_PADDING * 2f * this.uiScale);
        this.pillX0 = this.getX() + this.getWidth() - PADDING * this.uiScale - this.pillW;
        final GlassStyle base = (available ? this.pillStyle : GlassStyle.inactive(this.pillStyle)).scaled(this.uiScale);
        graphics.glass()
            .dimensions(this.pillX0, this.pillY0, this.pillW, this.pillH)
            .cornerRadius(this.pillH / 2f)
            .style(alpha < 1f ? base.copy().opacity(base.getOpacity() * alpha) : base)
            .hover(available ? this.hoverAnim : 0f)
            .extract();
        final boolean conflicted = !this.listening && !this.conflicts().isEmpty();
        final int textColor = conflicted ? conflictColor() : available ? LABEL_COLOR() : LABEL_COLOR_DISABLED();
        final int glyphColor = available ? Theme.get().secondaryLabel() : LABEL_COLOR_DISABLED();
        final float contentX = this.pillX0 + (this.pillW - contentWidth) / 2f;
        final float capHeight = textSize * 0.59043f;
        final float baseline = this.pillY0 + this.pillH / 2f + capHeight * 0.5f;
        ResetButton.lift(false, () -> {
            Icons.KEYBOARD.extract(graphics, contentX, this.pillY0 + (this.pillH - glyphSize) / 2f, glyphSize, ColorUtils.withAlpha(glyphColor, alpha));
            Fonts.SFPRO_DISPLAY.extract(graphics, text, contentX + glyphSize + glyphGap, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(textColor, alpha));
        });
        this.extractConflicts(graphics);
    }

    private boolean isOverPill(final double mouseX, final double mouseY) {
        return mouseX >= this.pillX0 && mouseX <= this.pillX0 + this.pillW && mouseY >= this.pillY0 && mouseY <= this.pillY0 + this.pillH;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || this.coveredByOverlay(event.x(), event.y()) || !this.isOverPill(event.x(), event.y())) {
            return false;
        }
        this.listening = true;
        capturing = this;
        this.setFocused(true);
        return true;
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.stopListening();
        }
    }

    private void stopListening() {
        this.listening = false;
        if (capturing == this) {
            capturing = null;
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (!this.listening) {
            return super.keyPressed(event);
        }
        this.option.getValue().rebind(event.key() == InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN : InputConstants.getKey(event));
        this.stopListening();
        return true;
    }
}
