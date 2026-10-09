package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.options.ModUpdateBanner;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.util.Mth;

public class ModWidget extends WLWidget {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x3A0D0EL);
    private static final float HOVER_SPEED = 14f;
    private static final float HOVER_HIGHLIGHT = 0.45f;
    private static final float ICON_SIZE = 13f;
    private static final float ICON_CORNER_RADIUS = 4f;
    private static final float ROW_CORNER_RADIUS = 5f;
    private static final float TEXT_SIZE = Type.SUBHEADLINE.size();
    private static final float ICON_GAP = 6f;
    private static final float MARKER_SIZE = 10f;
    private static final float MARKER_GAP = 5f;
    private static final float MARQUEE_START_HOLD = 0.6f;
    private static final float MARQUEE_SPEED = 20f;
    private static final float MARQUEE_GAP = 28f;
    private final Mod mod;
    private final String label;
    private final boolean showIcon;
    private final Icons.Icon glyph;
    private final Consumer<Mod> onSelect;
    private final Supplier<Mod> selectedModSupplier;
    private final float uiScale;
    private final float iconSize;
    private final float iconCornerRadius;
    private final float rowCornerRadius;
    private final float textSize;
    private final float iconGap;
    private IntSupplier labelColor = () -> Theme.get().label();
    private boolean updateMarker;
    private float hoverAnim;
    private float marqueeClock;

    public ModWidget(final Mod mod, final Consumer<Mod> onSelect, final Supplier<Mod> selectedModSupplier, final int x, final int y, final int width, final int height) {
        this(mod, onSelect, selectedModSupplier, null, true, null, x, y, width, height);
    }

    public ModWidget(final Mod mod, final Consumer<Mod> onSelect, final Supplier<Mod> selectedModSupplier, final String label, final Icons.Icon glyph, final int x, final int y, final int width, final int height) {
        this(mod, onSelect, selectedModSupplier, label, false, glyph, x, y, width, height);
    }

    public ModWidget(final Mod mod, final Consumer<Mod> onSelect, final Supplier<Mod> selectedModSupplier, final String label, final boolean showIcon, final Icons.Icon glyph, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.label = label;
        this.showIcon = showIcon;
        this.glyph = glyph;
        this.mod = mod;
        this.onSelect = onSelect;
        this.selectedModSupplier = selectedModSupplier;
        this.uiScale = WLScreen.uiScaleFactor();
        this.iconSize = ICON_SIZE * this.uiScale;
        this.iconCornerRadius = ICON_CORNER_RADIUS * this.uiScale;
        this.rowCornerRadius = ROW_CORNER_RADIUS * this.uiScale;
        this.textSize = TEXT_SIZE * this.uiScale;
        this.iconGap = ICON_GAP * this.uiScale;
    }

    public ModWidget labelColor(final IntSupplier color) {
        this.labelColor = color;
        return this;
    }

    public ModWidget withUpdateMarker() {
        this.updateMarker = true;
        return this;
    }

    private boolean hasUpdate() {
        if (!this.updateMarker) {
            return false;
        }
        final ModUpdate update = WalksyLib.getInstance().getModUpdates().of(this.mod);
        return update != null && update.isOutdated();
    }

    public static GlassStyle rowStyle() {
        return WalksyLibPanelScreen.sidebarStyle()
            .copy()
            .refractionThickness(6F)
            .layer(6)
            .fadeKey(FADE_KEYS.incrementAndGet())
            .tint(Theme.get().selectedRowTint(), Theme.get().selectedRowTintAlpha())
            .rimLight(Theme.get().rimLightColor(), 0f)
            .innerShadow(0f)
            .edgeDarken(Theme.get().edgeDarkenColor(), 0f)
            .fresnelFactor(0f)
            .fresnelRange(0f)
            .glareFactor(0f)
            .glareRange(0f)
            .brightEdge(0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    public static void extractHighlight(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float radius, final float amount) {
        if (amount <= 0.001f) {
            return;
        }
        final int fill = ColorUtils.withAlpha(0xFF000000 | Theme.get().selectedRowTint(), Theme.get().selectedRowTintAlpha() * amount);
        graphics.fillRoundedGradient(x, y, x + w, y + h, fill, fill, fill, fill, radius);
    }

    public static float rowIconGap(final float rowHeight) {
        return ICON_GAP * WLScreen.uiScaleFactor();
    }

    public static float rowCornerRadius(final float rowHeight) {
        return ROW_CORNER_RADIUS * WLScreen.uiScaleFactor();
    }

    public static float rowTextSize(final float rowHeight) {
        return TEXT_SIZE * WLScreen.uiScaleFactor();
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active;
        WLRenderSystem.cursors().requestControl(this.isHovered(), this.active);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        final boolean selected = this.selectedModSupplier != null && this.selectedModSupplier.get() == this.mod;
        final float highlight = selected ? 1f : this.hoverAnim * HOVER_HIGHLIGHT;
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f);
        if (alpha <= 0.001f) {
            return;
        }
        extractHighlight(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.rowCornerRadius, alpha * highlight);
        float leading = this.getX() + this.iconGap;
        if (this.hasUpdate()) {
            final float badge = MARKER_SIZE * this.uiScale;
            Icons.UPDATE.extract(graphics, leading, this.getY() + (this.getHeight() - badge) / 2f, badge, ColorUtils.withAlpha(ModUpdateBanner.green(), alpha));
            leading += badge + MARKER_GAP * this.uiScale;
        }
        float textX = leading;
        if (this.glyph != null) {
            final float glyphY = this.getY() + (this.getHeight() - this.iconSize) / 2f;
            this.glyph.extract(graphics, leading, glyphY, this.iconSize, ColorUtils.withAlpha(this.labelColor.getAsInt(), alpha));
            textX = leading + this.iconSize + this.iconGap;
        } else if (this.showIcon) {
            final float iconY = this.getY() + (this.getHeight() - this.iconSize) / 2f;
            final Identifier icon = this.mod.getModIcon();
            if (icon != null) {
                graphics.blitRounded(icon, leading, iconY, 0f, 0f, this.iconSize, this.iconSize, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE, ColorUtils.withAlpha(0xFFFFFFFF, alpha), this.iconCornerRadius);
            }
            textX = leading + this.iconSize + this.iconGap;
        }
        final float maxWidth = Math.max(0f, this.getX() + this.getWidth() - this.iconGap - textX);
        final String full = this.label != null ? this.label : this.mod.getDisplayName();
        final float fullWidth = Fonts.SFPRO_DISPLAY.width(full, this.textSize);
        final boolean scrolling = fullWidth > maxWidth + 0.5f && (hovered || selected);
        this.marqueeClock = scrolling ? this.marqueeClock + dt : 0f;
        final float capHeight = this.textSize * 0.59043f;
        final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
        final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, this.textSize);
        final int color = ColorUtils.withAlpha(this.labelColor.getAsInt(), alpha);
        if (scrolling) {
            this.extractMarquee(graphics, full, fullWidth, textX, maxWidth, textTop, color);
        } else {
            Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(full, this.textSize, maxWidth), textX, textTop, this.textSize, color);
        }
    }

    private void extractMarquee(final WLGraphics2D graphics, final String text, final float textWidth, final float x, final float maxWidth, final float top, final int color) {
        final float speed = MARQUEE_SPEED * this.uiScale;
        final float travel = textWidth + MARQUEE_GAP * this.uiScale;
        final float cycle = MARQUEE_START_HOLD + travel / speed;
        final float t = this.marqueeClock % cycle;
        final float offset = t <= MARQUEE_START_HOLD ? 0f : (t - MARQUEE_START_HOLD) * speed;
        final float right = x + maxWidth;
        final boolean moving = offset > 0f;
        graphics.enableScissorWithin(x, this.getY(), right, this.getY() + this.getHeight());
        if (moving) {
            WLRenderSystem.beginMotion();
        }
        try {
            Fonts.SFPRO_DISPLAY.extract(graphics, text, x - offset, top, this.textSize, color);
            final float trailing = x - offset + travel;
            if (trailing < right) {
                Fonts.SFPRO_DISPLAY.extract(graphics, text, trailing, top, this.textSize, color);
            }
        } finally {
            if (moving) {
                WLRenderSystem.endMotion();
            }
            graphics.disableScissor();
        }
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.onSelect != null) {
            this.onSelect.accept(this.mod);
        }
    }
}
