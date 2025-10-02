package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.Animation;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

public abstract class OpenableWidget extends OptionWidget {

    public boolean open = false;
    public int OPEN_HEIGHT;

    private final Animation heightAnim;

    public OpenableWidget(OptionGroup parent, WalksyLibConfigScreen screen, Option<?> option, int x, int y, int width, int height, String name, int openedHeight) {
        super(parent, screen, option, x, y, width, height, name);
        this.OPEN_HEIGHT = openedHeight;
        this.heightAnim = new Animation(height, 0.5f);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        heightAnim.update(delta);
        float currentAnimated = heightAnim.getCurrentValue();
        if (Math.abs(currentAnimated - this.height) >= 1f) {
            int animHeight = Math.round(currentAnimated);
            setHeight(animHeight);
            update();
        }



        super.renderWidget(context, mouseX, mouseY, delta);

        if (isVisible()) {
            WalksyLib.get2DRenderer().renderMiniArrow(
                    context,
                    getX() - 8,
                    getTextYCentered() + (open ? 4 : 5),
                    1,
                    open ? Renderer2D.ArrowDirection.DOWN : Renderer2D.ArrowDirection.RIGHT,
                    isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB()
            );
        }
    }


    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (isHovered() && button == 0) {
            toggleOpen();
        }
    }

    public float getCurrentHeight()
    {
        return this.heightAnim.getCurrentValue();
    }

    private void toggleOpen() {
        boolean prev = open;
        open = !open;

        float target = open ? OPEN_HEIGHT : WalksyLibScreenManager.Globals.OPTION_HEIGHT;
        heightAnim.setTargetValue(target);

        ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
        this.onOpen(prev);
    }

    public boolean fullyClosed()
    {
        return !this.open && Math.round(this.heightAnim.getCurrentValue()) == WalksyLibScreenManager.Globals.OPTION_HEIGHT;
    }

    protected abstract void onOpen(boolean prevValue);

    @Override
    public void onWidgetUpdate() {
    }
}
