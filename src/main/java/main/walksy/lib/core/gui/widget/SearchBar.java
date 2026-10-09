package main.walksy.lib.core.gui.widget;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.texture.Icons;
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

public class SearchBar extends AbstractTextEditWidget {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x5EA2C0L);
    private static final float HOVER_SPEED = 14f;
    private static final float FOCUS_SPEED = 12f;
    private static final float CAP_HEIGHT_RATIO = 0.70f;
    private static final int FIELD_LAYER = 6;
    private static final float LENS_THICKNESS_RATIO = 0.85f;
    private static final float LENS_REACH = 0.42f;
    private static final float LENS_DISPERSION = 1.5f;
    private static final float LENS_STRENGTH = 1.6f;
    private static final float LENS_FACTOR = 1.35f;
    private static final float DEEP_LENS_REACH = 0.225f;
    private String placeholder = "Search";
    private Consumer<String> onChanged;
    private Runnable onSubmit;
    private float cornerRadius;
    private float textSize;
    private int textColor = Theme.get().label();
    private int placeholderColor = Theme.get().secondaryLabel();
    private int accentColor = Theme.get().accent();
    private final GlassStyle style;
    private final float uiScale;
    private float hoverAnim;
    private float focusAnim;
    private boolean liftedText;
    private boolean deepLens;

    public SearchBar(final int x, final int y, final int width, final int height, final Consumer<String> onChanged) {
        super(x, y, width, height);
        this.onChanged = onChanged;
        this.uiScale = WLScreen.uiScaleFactor();
        this.cornerRadius = 0.5f * Math.min(width, height);
        this.textSize = Mth.clamp(height * 0.50f, Type.CAPTION1.size() * this.uiScale, Type.TITLE3.size() * this.uiScale);
        this.style = LiquidKnob.bevelKnobStyle(FIELD_LAYER).fadeKey(FADE_KEYS.incrementAndGet());
    }

    public static GlassStyle chromeStyle() {
        return WalksyLibPanelScreen.floatingChromeStyle()
            .layer(FIELD_LAYER)
            .tint(WalksyLibPanelScreen.fieldTintColor(), WalksyLibPanelScreen.fieldTintAlpha());
    }

    public static void sizeChromeLens(final GlassStyle style, final float height, final float uiScale) {
        style.refractionThickness(height / Math.max(0.01f, uiScale) * LENS_THICKNESS_RATIO)
            .refractionReach(LENS_REACH)
            .refractionDispersion(LENS_DISPERSION)
            .refractionStrength(LENS_STRENGTH)
            .refractionFactor(LENS_FACTOR);
    }

    public static GlassStyle defaultStyle() {
        return WalksyLibPanelScreen.sidebarStyle()
            .copy()
            .refractionThickness(6F)
            .layer(FIELD_LAYER)
            .tint(Theme.get().fieldTint(), Theme.get().fieldTintAlpha())
            .rimLight(Theme.get().rimLightColor(), 0f)
            .innerShadow(0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), 0f)
            .fresnelFactor(0f)
            .fresnelRange(0f)
            .glareFactor(0f)
            .glareRange(0f)
            .brightEdge(LiquidGlassConstants.CHROME_BRIGHT_EDGE)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active;
        final boolean focused = this.isFocused() && this.active;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.TEXT);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        this.focusAnim = MathUtils.approach(this.focusAnim, focused ? 1f : 0f, FOCUS_SPEED, dt);
        this.caretTimer += dt;
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
        if (alpha <= 0.001f) {
            return;
        }
        final GlassStyle base = (this.active ? this.style.hoverDarken(this.hoverAnim) : GlassStyle.inactive(this.style)).scaled(this.uiScale);
        if (this.deepLens) {
            base.refractionThickness(GlassStyle.centreLensBand(graphics.pose(), this.getHeight(), LiquidGlassConstants.FOCUS_SCALE_PX * this.focusAnim))
                .refractionDispersion(0f);
        }
        final GlassStyle drawStyle = alpha < 1f ? base.copy().opacity(base.getOpacity() * alpha) : base;
        graphics.glass()
            .dimensions(this.getX(), this.getY(), this.getWidth(), this.getHeight())
            .cornerRadius(this.cornerRadius)
            .style(drawStyle)
            .focus(this.focusAnim)
            .extract();
        final float h = this.getHeight();
        final float cy = this.getY() + h / 2f;
        final float size = this.textSize;
        final float iconSize = h * 0.55f;
        final float iconCx = this.getX() + h * 0.5f;
        final int iconColor = Theme.get().label();
        final boolean lift = this.liftedText && !WLRenderSystem.contentStratum().isActive();
        final TextQueueMark mark = lift ? WLRenderSystem.textMark() : null;
        if (lift) {
            WLRenderSystem.contentStratum().begin();
        }
        try {
            Icons.SEARCH.extractCentered(graphics, iconCx, cy, iconSize, ColorUtils.withAlpha(iconColor, alpha), 0f);
            this.extractFieldInk(graphics, focused, alpha, cy, size);
        } finally {
            if (lift) {
                WLRenderSystem.contentStratum().end();
                WLRenderSystem.markContentText(mark);
            }
        }
    }

    private void extractFieldInk(final WLGraphics2D graphics, final boolean focused, final float alpha, final float cy, final float size) {
        final float textStartX = this.textStartX();
        final float capHeight = size * CAP_HEIGHT_RATIO;
        final float caretHalf = capHeight * 0.5f + 1.5f * this.uiScale;
        final float caretTop = snapToPhysicalPixelY(cy - caretHalf);
        final float caretBottom = snapToPhysicalPixelY(cy + caretHalf);
        this.layoutText(textStartX, this.innerWidth(), size);
        final String value = this.text.toString();
        final int visEnd = this.lastVisibleEnd;
        final float snappedTextStartX = snapToPhysicalPixel(textStartX);
        if (this.hasSelection()) {
            final int s = Math.max(this.selStart(), this.displayPos);
            final int e = Math.min(this.selEnd(), visEnd);
            if (e > s) {
                final float x1 = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(value.substring(this.displayPos, s), size));
                final float x2 = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(value.substring(this.displayPos, e), size));
                graphics.fill(x1, caretTop, x2, caretBottom, ColorUtils.withAlpha(this.accentColor, 0.35f * alpha), null);
            }
        }
        this.extractLabel(graphics, alpha);
        if (focused && this.caret >= this.displayPos && this.caret <= visEnd && this.caretVisible()) {
            final float cx = snapToPhysicalPixel(snappedTextStartX + Fonts.SFPRO_DISPLAY.width(value.substring(this.displayPos, this.caret), size));
            graphics.fill(cx, caretTop, cx + caretWidth(this.uiScale), caretBottom, ColorUtils.withAlpha(this.textColor, alpha), null);
        }
    }

    private void extractLabel(final WLGraphics2D graphics, final float alpha) {
        final float size = this.textSize;
        final float textStartX = this.textStartX();
        final float baseline = this.getY() + this.getHeight() / 2f + size * CAP_HEIGHT_RATIO * 0.5f;
        final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size) - 0.5F * this.uiScale;
        final String visible = this.layoutText(textStartX, this.innerWidth(), size);
        if (this.text.isEmpty() && !this.isFocused()) {
            Fonts.SFPRO_DISPLAY.extract(graphics, this.placeholder, textStartX, textTop, size, ColorUtils.withAlpha(this.placeholderColor, (1f - 0.35f * this.focusAnim) * alpha));
        }
        if (!visible.isEmpty()) {
            Fonts.SFPRO_DISPLAY.extract(graphics, visible, textStartX, textTop, size, ColorUtils.withAlpha(this.textColor, alpha));
        }
    }

    private float textStartX() {
        return this.getX() + this.getHeight();
    }

    private float innerWidth() {
        return Math.max(8f * this.uiScale, this.getX() + this.getWidth() - this.getHeight() * 0.4f - this.textStartX());
    }

    @Override
    protected void onTextChanged() {
        this.fireChanged();
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        this.handleTextClick(event.x(), doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (!this.isFocused() || !this.active || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.updateCaretFromDrag(event.x());
        return true;
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.handleCharTyped(event);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.handleCommonKeyPress(event)) {
            return true;
        }
        if (!this.isFocused() || !this.active) {
            return false;
        }
        final int key = event.key();
        switch (key) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (this.onSubmit != null) {
                    this.onSubmit.run();
                }
                return true;
            }
            case InputConstants.KEY_ESCAPE -> {
                this.setFocused(false);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public SearchBar clear() {
        this.setValue("");
        this.fireChanged();
        return this;
    }

    public SearchBar setPlaceholder(final String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public SearchBar setOnChanged(final Consumer<String> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public SearchBar setOnSubmit(final Runnable onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    public SearchBar setLiftedText(final boolean liftedText) {
        this.liftedText = liftedText;
        this.style.bakesText(!liftedText);
        return this;
    }

    public SearchBar deepLens() {
        this.deepLens = true;
        this.style.softLens(true).refractionReach(DEEP_LENS_REACH);
        return this;
    }

    public SearchBar setCornerRadius(final float radiusPx) {
        this.cornerRadius = Math.max(0f, radiusPx);
        return this;
    }

    public SearchBar setTextSize(final float size) {
        this.textSize = Math.max(1f, size);
        return this;
    }

    public SearchBar setTextColor(final int color) {
        this.textColor = color;
        return this;
    }

    public SearchBar setPlaceholderColor(final int color) {
        this.placeholderColor = color;
        return this;
    }

    public SearchBar setAccentColor(final int color) {
        this.accentColor = color;
        return this;
    }

    public GlassStyle style() {
        return this.style;
    }

    private void fireChanged() {
        if (this.onChanged != null) {
            this.onChanged.accept(this.text.toString());
        }
    }
}
