package main.walksy.lib.core.gui.widget.options.types;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class ButtonOptionWidget extends OptionRowWidget<Runnable> {

    @Override
    protected boolean dimsDescription() {
        return false;
    }

    private static final float HOVER_SPEED = 14f;
    private static final float CORNER_RATIO = 0.28f;
    private static final float CARD_RADIUS = 6f;
    private float hoverAnim;

    public ButtonOptionWidget(final Option<Runnable> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final boolean overRow = this.isOverRow(mouseX, mouseY) && !this.coveredByOverlay(mouseX, mouseY);
        final boolean hovered = available && overRow;
        WLRenderSystem.cursors().requestControl(overRow, available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        if (this.hoverAnim <= 0.001f) {
            return;
        }
        final float h = this.rowLabelHeight();
        final int fill = ColorUtils.withAlpha(Theme.get().hoverFillArgb(), alpha * this.hoverAnim);
        final float radius = Math.min(h * CORNER_RATIO, CARD_RADIUS * this.uiScale);
        graphics.fillRoundedGradient(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + h, fill, fill, fill, fill, radius);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.isInteractive() && this.isOverRow(event.x(), event.y())) {
            this.option.getValue().run();
        }
    }
}
