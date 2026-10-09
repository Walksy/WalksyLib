package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Consumer;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;

public final class AllModsPaneWidget extends WLWidget {
    private static final float CARD_W = 150f;
    private static final float CARD_H = 62f;
    private static final float CARD_GAP = 8f;
    private static final float CARD_PAD = 10f;
    private static final float ICON_SIZE = 22f;
    private static final float CARD_CORNER = 10f;
    private static final float HOVER_SPEED = 14f;
    private static final float MARQUEE_START_HOLD = 1.2f;
    private static final float MARQUEE_SPEED = 20f;
    private static final float MARQUEE_GAP = 28f;
    private final List<Mod> mods;
    private final Consumer<Mod> onSelect;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final GlassStyle cardStyle;
    private final int columns;
    private final float cardW;
    private final float cardH;
    private final float[] hoverAnim;
    private float marqueeClock;

    public AllModsPaneWidget(final List<Mod> mods, final Consumer<Mod> onSelect, final int x, final int y, final int width) {
        super(x, y, width, 1);
        this.mods = mods;
        this.onSelect = onSelect;
        this.hoverAnim = new float[mods.size()];
        final float gap = CARD_GAP * this.uiScale;
        final float wanted = CARD_W * this.uiScale;
        this.columns = Math.max(1, (int) ((width + gap) / (wanted + gap)));
        this.cardW = (width - gap * (this.columns - 1)) / this.columns;
        this.cardH = CARD_H * this.uiScale;
        final int rows = Math.max(1, (mods.size() + this.columns - 1) / this.columns);
        this.setHeight(Math.round(rows * this.cardH + (rows - 1) * gap));
        this.cardStyle = ResetButton.knobStyle(ControlLayers.BASE);
    }

    private float cardX(final int index) {
        return this.getX() + (index % this.columns) * (this.cardW + CARD_GAP * this.uiScale);
    }

    private float cardY(final int index) {
        return this.getY() + (index / this.columns) * (this.cardH + CARD_GAP * this.uiScale);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float dt = WLRenderSystem.frameDeltaSeconds();
        if (dt > 0f && Float.isFinite(dt)) {
            this.marqueeClock += dt;
        }
        final float pad = CARD_PAD * this.uiScale;
        final float iconSize = ICON_SIZE * this.uiScale;
        final float nameSize = Type.SUBHEADLINE.size() * this.uiScale;
        final float bodySize = Type.CAPTION1.size() * this.uiScale;
        for (int i = 0; i < this.mods.size(); i++) {
            final Mod mod = this.mods.get(i);
            final float x = this.cardX(i);
            final float y = this.cardY(i);
            final boolean hovered = mouseX >= x && mouseX < x + this.cardW && mouseY >= y && mouseY < y + this.cardH;
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            this.hoverAnim[i] = MathUtils.approach(this.hoverAnim[i], hovered ? 1f : 0f, HOVER_SPEED, dt);
            graphics.glass()
                .dimensions(x, y, this.cardW, this.cardH)
                .cornerRadius(CARD_CORNER * this.uiScale)
                .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0xA11D0D5L + i))
                .hover(this.hoverAnim[i])
                .extract();
            ResetButton.lift(false, () -> this.extractCardContent(graphics, mod, x, y, pad, iconSize, nameSize, bodySize, alpha));
        }
    }

    private void extractCardContent(final WLGraphics2D graphics, final Mod mod, final float x, final float y, final float pad, final float iconSize, final float nameSize, final float bodySize, final float alpha) {
        final Identifier icon = mod.getModIcon();
        float textX = x + pad;
        if (icon != null) {
            graphics.blitRounded(icon, x + pad, y + pad, 0f, 0f, iconSize, iconSize, (int) iconSize, (int) iconSize, (int) iconSize, (int) iconSize, ColorUtils.withAlpha(0xFFFFFFFF, alpha), 5f * this.uiScale);
            textX = x + pad + iconSize + pad * 0.6f;
        }
        final float nameTop = Fonts.SFPRO_DISPLAY.topForBaseline(y + pad + nameSize, nameSize);
        final float room = Math.max(4f, x + this.cardW - pad - textX);
        Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(mod.getDisplayName(), nameSize, room), textX, nameTop, nameSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(version(mod), bodySize, room), textX, Fonts.SFPRO_DISPLAY.topForBaseline(y + pad + nameSize + bodySize * 1.5f, bodySize), bodySize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
        final String description = description(mod);
        if (!description.isEmpty()) {
            final float descRoom = this.cardW - pad * 2f;
            final float descTop = Fonts.SFPRO_DISPLAY.topForBaseline(y + this.cardH - pad, bodySize);
            final float descWidth = Fonts.SFPRO_DISPLAY.width(description, bodySize);
            final int descColor = ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha);
            if (descWidth <= descRoom) {
                Fonts.SFPRO_DISPLAY.extract(graphics, description, x + pad, descTop, bodySize, descColor);
            } else {
                this.extractMarquee(graphics, description, descWidth, x + pad, x, y, descTop, bodySize, descColor);
            }
        }
    }

    private void extractMarquee(final WLGraphics2D graphics, final String text, final float textWidth, final float x, final float cardX, final float cardY, final float top, final float size, final int color) {
        final float speed = MARQUEE_SPEED * this.uiScale;
        final float travel = textWidth + MARQUEE_GAP * this.uiScale;
        final float cycle = MARQUEE_START_HOLD + travel / speed;
        final float t = this.marqueeClock % cycle;
        final float offset = t <= MARQUEE_START_HOLD ? 0f : (t - MARQUEE_START_HOLD) * speed;
        final float cardRight = cardX + this.cardW;
        final boolean moving = offset > 0f;
        graphics.enableScissorWithin(cardX, cardY, cardRight, cardY + this.cardH);
        if (moving) {
            WLRenderSystem.beginMotion();
        }
        try {
            Fonts.SFPRO_DISPLAY.extract(graphics, text, x - offset, top, size, color);
            final float trailing = x - offset + travel;
            if (trailing < cardRight) {
                Fonts.SFPRO_DISPLAY.extract(graphics, text, trailing, top, size, color);
            }
        } finally {
            if (moving) {
                WLRenderSystem.endMotion();
            }
            graphics.disableScissor();
        }
    }

    private static String version(final Mod mod) {
        return mod.getContainer() == null ? "" : "v" + mod.getContainer().getMetadata().getVersion().getFriendlyString();
    }

    private static String description(final Mod mod) {
        return mod.getContainer() == null ? "" : mod.getContainer().getMetadata().getDescription();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        for (int i = 0; i < this.mods.size(); i++) {
            final float x = this.cardX(i);
            final float y = this.cardY(i);
            if (event.x() >= x && event.x() < x + this.cardW && event.y() >= y && event.y() < y + this.cardH) {
                this.onSelect.accept(this.mods.get(i));
                return true;
            }
        }
        return false;
    }
}
