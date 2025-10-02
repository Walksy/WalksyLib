package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

public class ButtonOptionWidget extends OptionWidget {

    private final Option<Runnable> option;
    private boolean hoveredButton;

    public ButtonOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<Runnable> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.hoveredButton = false;
        this.resetButton.visible = false;
    }

    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        hoveredButton = mouseX >= getWidth() - 30 && mouseX <= getWidth() - 30 + 38 &&
                mouseY >= getY() + 3 && mouseY <= getY() + 3 + getHeight() - 6;
        WalksyLib.get2DRenderer().fillRoundedRectOutline(context, getWidth() - 31, getY() + 2, 40, getHeight() - 4, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        WalksyLib.get2DRenderer().fillRoundedRectOutline(context, getWidth() - 30, getY() + 3, 38, getHeight() - 6, 2, 1, hoveredButton ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());
        context.drawTextWithShadow(screen.getTextRenderer(), "Press", getWidth() - 29 + (38 - screen.getTextRenderer().getWidth("Press")) / 2, getTextYCentered() + 1, -1);
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        if (hoveredButton && button == 0) {
            if (this.option.getValue() == null) return;
            this.option.getValue().run();
            ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
        }
        super.onMouseClick(mouseX, mouseY, button);
    }



    @Override
    public void onWidgetUpdate() {

    }

    @Override
    public boolean isHovered() {
        return false;
    }
}
