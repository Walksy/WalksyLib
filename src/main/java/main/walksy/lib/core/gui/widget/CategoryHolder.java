package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class CategoryHolder extends WLWidget {
    private static final float CHEVRON_RATIO = 0.8f;
    private static final float CHEVRON_GAP = 4f;
    private static final float HOVER_SPEED = 14f;
    private final String label;
    private final float textSize;
    private final int color;
    private final @Nullable Runnable onToggle;
    private final @Nullable BooleanSupplier expanded;
    private float hoverAnim;

    public CategoryHolder(final int x, final int y, final int width, final int height, final String label, final float textSize, final int color) {
        this(x, y, width, height, label, textSize, color, null, null);
    }

    public CategoryHolder(final int x, final int y, final int width, final int height, final String label, final float textSize, final int color, @Nullable final BooleanSupplier expanded, @Nullable final Runnable onToggle) {
        super(x, y, width, height);
        this.label = label;
        this.textSize = textSize;
        this.color = color;
        this.expanded = expanded;
        this.onToggle = onToggle;
    }

    private boolean collapsible() {
        return this.onToggle != null && this.expanded != null;
    }

    @Override
    public boolean isMouseOver(final double mouseX, final double mouseY) {
        if (!this.collapsible()) {
            return super.isMouseOver(mouseX, mouseY);
        }
        return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY < this.getY() + Math.max(this.getHeight(), this.textSize);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final boolean hovered = this.collapsible() && this.isMouseOver(mouseX, mouseY);
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        final float dt = WLRenderSystem.frameDeltaSeconds();
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        final int textColor = this.collapsible() ? ColorUtils.lerpColor(this.color, Theme.get().label(), this.hoverAnim) : this.color;
        Type.SUBHEADLINE.emphasized().extract(graphics, this.label, this.getX(), this.getY(), this.textSize, ColorUtils.withAlpha(textColor, alpha));
        if (!this.collapsible()) {
            return;
        }
        final float size = this.textSize * CHEVRON_RATIO;
        final float x = this.getX() + this.getWidth() - size - CHEVRON_GAP;
        final float y = this.getY() + (this.textSize - size) / 2f;
        Icons.CHEVRON.extractRotated(graphics, x, y, size, ColorUtils.withAlpha(textColor, alpha), this.expanded.getAsBoolean() ? 90f : 0f);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.collapsible()) {
            this.onToggle.run();
        }
    }
}
