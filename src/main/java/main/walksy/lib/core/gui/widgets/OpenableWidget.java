package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import main.walksy.lib.core.utils.ScreenGlobals;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class OpenableWidget extends OptionWidget {

    public boolean open = false;
    public int OPEN_HEIGHT;

    private final Animation heightAnim;

    public OpenableWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final Option<?> option, final int x, final int y, final int width, final int height, final String name, final int openedHeight) {
        super(parent, screen, option, x, y, width, height, name);
        this.OPEN_HEIGHT = openedHeight;
        this.heightAnim = new Animation(height, 0.5f);
    }

    public OpenableWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final Option<?> option, final int x, final int y, final int width, final int height, final String name) {
        super(parent, screen, option, x, y, width, height, name);
        this.heightAnim = new Animation(height, 0.5f);
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        this.heightAnim.update(delta);
        final float currentAnimated = this.heightAnim.getCurrentValue();
        if (Math.abs(currentAnimated - this.height) >= 1f) {
            final int animHeight = Math.round(currentAnimated);
            this.setHeight(animHeight);
            this.update();
        }
        context.enableScissor(0, 49, this.screen.width, this.screen.height - 28);
        if (this.isVisible()) {
            new Graphics(context).renderMiniArrow(
                    this.getX() - 8,
                    this.getTextYCentered() + (this.open ? 4 : 5),
                    1,
                    this.open ? Graphics.ArrowDirection.DOWN : Graphics.ArrowDirection.RIGHT,
                    this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
            );
        }
        context.disableScissor();
        if (!this.heightAnim.isAnimating() && !this.open) {
            this.setHeight(ScreenGlobals.OPTION_HEIGHT);
        }
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {}

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        if (this.isHovered() && click.button() == 0) {
            this.toggleOpen();
        }
    }

    public float getCurrentHeight() {
        return this.heightAnim.getCurrentValue();
    }

    private void toggleOpen() {
        final boolean prev = this.open;
        this.open = !this.open;

        final float target = this.open ? this.OPEN_HEIGHT : ScreenGlobals.OPTION_HEIGHT;
        this.heightAnim.setTargetValue(target);
        this.onOpen(prev);
    }

    public boolean fullyClosed() {
        return !this.open && Math.round(this.heightAnim.getCurrentValue()) == ScreenGlobals.OPTION_HEIGHT;
    }

    protected abstract void onOpen(boolean prevValue);

    @Override
    public void onWidgetUpdate() {}
}
