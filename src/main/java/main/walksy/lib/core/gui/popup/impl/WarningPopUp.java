package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.DrawContext;

public class WarningPopUp extends PopUp {
    private final String title;
    public final ButtonWidget yesButton;
    public final ButtonWidget noButton;

    public WarningPopUp(WalksyLibConfigScreen parent, String title, String message, Runnable yesAction, Runnable noAction)
    {
        super(parent, message);
        this.title = title;
        this.yesButton = new ButtonWidget((x + width) - 60, (y + height) - 33, 50, 20, false, "Yes", yesAction);
        this.noButton = new ButtonWidget((x) + 10, (y + height) - 33, 50, 20, false, "No", noAction);
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.getMatrices().push();
        float scale = 1.5F;
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawCenteredTextWithShadow(
                parent.getTextRenderer(),
                title,
                (int) ((parent.width / 2) / scale),
                (int) ((parent.height / 2 - 40) / scale),
                0xFFFFFF
        );
        context.getMatrices().pop();

        context.drawHorizontalLine(x + 2, x + width - 3, this.y + 30, MainColors.OUTLINE_WHITE.getRGB());
        context.drawCenteredTextWithShadow(parent.getTextRenderer(), subText, parent.width / 2 - (this.width / 2) + (this.width / 2), parent.height / 2 - (this.height / 2) + (this.height / 2) - 10, -1);

        yesButton.render(context, (int)mouseX, (int)mouseY, delta);
        noButton.render(context, (int)mouseX, (int)mouseY, delta);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button)
    {
        yesButton.onClick(mouseX, mouseY);
        noButton.onClick(mouseX, mouseY);
    }

    @Override
    protected void onClose() {

    }

    @Override
    public void layout(int width, int height) {
        super.layout(width, height);
        if (yesButton != null && noButton != null) {
            yesButton.setPosition((x + width) - 60, (y + height) - 33);
            noButton.setPosition((x) + 10, (y + height) - 33);
        }
    }
}
