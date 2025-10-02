package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

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
                (int) ((y + 11) / scale),
                0xFFFFFF
        );
        context.getMatrices().pop();

        context.drawHorizontalLine(x + 2, x + width - 3, this.y + 30, MainColors.OUTLINE_WHITE.getRGB());

        List<OrderedText> orderedTexts = parent.getTextRenderer().wrapLines(Text.of(subText), this.width - 20);
        List<TooltipComponent> tooltipComponents = orderedTexts.stream().map(TooltipComponent::of).toList();
        int totalTextHeight = tooltipComponents.stream()
                .mapToInt(tc -> tc.getHeight(parent.getTextRenderer()))
                .sum();
        int yOffset = this.y + (this.height / 2) - (totalTextHeight / 2);

        for (TooltipComponent tooltipComponent : tooltipComponents) {
            int lineHeight = tooltipComponent.getHeight(parent.getTextRenderer());
            tooltipComponent.drawText(
                    parent.getTextRenderer(),
                    (this.x + (this.width / 2)) - tooltipComponent.getWidth(parent.getTextRenderer()) / 2,
                    yOffset,
                    context.getMatrices().peek().getPositionMatrix(),
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
            );
            yOffset += lineHeight;
        }
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
        super.layout(width, getHeightOffset() + 90);
        if (yesButton != null && noButton != null) {
            yesButton.setPosition((x + width) - 60, (y + height) - 33);
            noButton.setPosition((x) + 10, (y + height) - 33);
        }
    }

    public int getHeightOffset()
    {
        List<OrderedText> orderedTexts = parent.getTextRenderer().wrapLines(Text.of(subText), this.width - 20);
        List<TooltipComponent> tooltipComponents = orderedTexts.stream().map(TooltipComponent::of).toList();
        int height = 0;
        for (TooltipComponent tooltipComponent : tooltipComponents)
        {
            height+=tooltipComponent.getHeight(parent.getTextRenderer());
        }
        return height;
    }
}
