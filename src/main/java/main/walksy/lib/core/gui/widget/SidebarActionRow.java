package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.BooleanSupplier;
import net.minecraft.util.Mth;

public final class SidebarActionRow extends WLWidget {
    private static final float ICON_SIZE = 13f;
    private static final float HOVER_SPEED = 14f;
    private static final float HOVER_HIGHLIGHT = 0.55f;
    private static final float AVAIL_SPEED = 10f;
    private static final float AVAIL_DIM = 0.4f;
    private final String label;
    private final Icons.Icon glyph;
    private final Runnable onPress;
    private final BooleanSupplier selected;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final float iconSize = ICON_SIZE * this.uiScale;
    private final float iconGap;
    private final float rowCornerRadius;
    private final float textSize;
    private float hoverAnim;
    private float availAnim = 1f;

    public SidebarActionRow(final int x, final int y, final int width, final int height, final String label, final Icons.Icon glyph, final Runnable onPress, final BooleanSupplier selected) {
        super(x, y, width, height);
        this.label = label;
        this.glyph = glyph;
        this.onPress = onPress;
        this.selected = selected;
        this.iconGap = ModWidget.rowIconGap(height);
        this.rowCornerRadius = ModWidget.rowCornerRadius(height);
        this.textSize = ModWidget.rowTextSize(height);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active;
        WLRenderSystem.cursors().requestControl(this.isHovered(), this.active);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        this.availAnim = MathUtils.approach(this.availAnim, this.active ? 1f : AVAIL_DIM, AVAIL_SPEED, dt);
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f) * this.availAnim;
        if (alpha <= 0.001f) {
            return;
        }
        final boolean isSelected = this.selected != null && this.selected.getAsBoolean();
        final float highlight = isSelected ? 1f : this.hoverAnim * HOVER_HIGHLIGHT;
        ModWidget.extractHighlight(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.rowCornerRadius, alpha * highlight);
        final float glyphY = this.getY() + (this.getHeight() - this.iconSize) / 2f;
        final int tint = ColorUtils.withAlpha(this.active ? Theme.get().label() : Theme.get().tertiaryLabel(), alpha);
        this.glyph.extract(graphics, this.getX() + this.iconGap, glyphY, this.iconSize, tint);
        final float textX = this.getX() + this.iconGap + this.iconSize + this.iconGap;
        final float capHeight = this.textSize * 0.59043f;
        final float baseline = this.getY() + this.getHeight() / 2f + capHeight * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, this.label, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, this.textSize), this.textSize, tint);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.active && this.onPress != null) {
            this.onPress.run();
        }
    }
}
