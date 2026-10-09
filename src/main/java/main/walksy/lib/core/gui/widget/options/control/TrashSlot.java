package main.walksy.lib.core.gui.widget.options.control;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;

public final class TrashSlot {
    private static final float HOT_SPEED = 16f;
    private static final float ICON_RATIO = 0.62f;
    private static final float ARMED_ICON_MIX = 0.35f;
    private static final float HOT_TINT_ALPHA = 0.35f;
    private float x;
    private float y;
    private float size;
    private float hotAnim;

    public void layout(final float x, final float y, final float size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public boolean contains(final double mouseX, final double mouseY) {
        return this.size > 0f && mouseX >= this.x && mouseX < this.x + this.size && mouseY >= this.y && mouseY < this.y + this.size;
    }

    public void extract(final WLGraphics2D graphics, final float radius, final float ring, final boolean hovered, final boolean armed, final float alpha, final float dt) {
        if (!this.animate(hovered, armed, alpha, dt)) {
            return;
        }
        final int destructive = Theme.get().destructive();
        final int restEdge = ColorUtils.withAlpha(Theme.get().label(), 0.16f);
        final int edge = ColorUtils.withAlpha(ColorUtils.lerpColor(restEdge, ColorUtils.withAlpha(destructive, 0.95f), this.hotAnim), alpha);
        final int restWell = ColorUtils.withAlpha(Theme.get().shadowColor(), 0.24f);
        final int well = ColorUtils.withAlpha(ColorUtils.lerpColor(restWell, ColorUtils.withAlpha(destructive, 0.28f), this.hotAnim), alpha);
        final float inner = Math.max(0f, radius - ring);
        graphics.fillRoundedGradient(this.x, this.y, this.x + this.size, this.y + this.size, edge, edge, edge, edge, radius);
        graphics.fillRoundedGradient(this.x + ring, this.y + ring, this.x + this.size - ring, this.y + this.size - ring, well, well, well, well, inner);
        this.extractIcon(graphics, armed, alpha);
    }

    public void extractGlass(final WLGraphics2D graphics, final GlassStyle style, final float radius, final boolean hovered, final boolean armed, final float alpha, final float dt) {
        if (!this.animate(hovered, armed, alpha, dt)) {
            return;
        }
        if (this.hotAnim > 0.004f) {
            style.tint(Theme.get().destructive(), HOT_TINT_ALPHA * this.hotAnim);
        }
        graphics.glass()
            .dimensions(this.x, this.y, this.size, this.size)
            .cornerRadius(radius)
            .style(style)
            .hover(hovered ? 1f : 0f)
            .extract();
        this.extractIcon(graphics, armed, alpha);
    }

    private boolean animate(final boolean hovered, final boolean armed, final float alpha, final float dt) {
        final boolean hot = hovered && armed;
        this.hotAnim = MathUtils.approach(this.hotAnim, hot ? 1f : 0f, HOT_SPEED, dt);
        WLRenderSystem.cursors().requestIf(hot, Cursors.Cursor.POINTER);
        return alpha > 0.004f && this.size > 0f;
    }

    private void extractIcon(final WLGraphics2D graphics, final boolean armed, final float alpha) {
        final int destructive = Theme.get().destructive();
        final float tint = Math.max(this.hotAnim, armed ? ARMED_ICON_MIX : 0f);
        final int icon = ColorUtils.withAlpha(ColorUtils.lerpColor(Theme.get().secondaryLabel(), destructive, tint), alpha);
        Icons.TRASH.extractCentered(graphics, this.x + this.size / 2f, this.y + this.size / 2f, this.size * ICON_RATIO, icon, 0f);
    }
}
