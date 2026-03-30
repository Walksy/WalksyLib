package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class WarningPopUp extends PopUp {
    private final String title;
    public final ButtonWidget yesButton;
    public final ButtonWidget noButton;

    public WarningPopUp(WalksyLibConfigScreen parent, String title, String message, Runnable yesAction, Runnable noAction) {
        super(parent, message);
        this.title = title;
        this.yesButton = new ButtonWidget((x + width) - 60, (y + height) - 33, 50, 20, false, "Yes", yesAction);
        this.noButton = new ButtonWidget((x) + 10, (y + height) - 33, 50, 20, false, "No", noAction);
    }

    @Override
    public void render(GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.pose().pushMatrix();
        float scale = 1.5F;
        context.pose().scale(scale, scale);
        context.centeredText(
                parent.getFont(),
                title,
                (int) ((parent.width / 2) / scale),
                (int) ((y + 11) / scale),
                -1
        );
        context.pose().popMatrix();

        context.horizontalLine(x + 2, x + width - 3, this.y + 30, MainColors.OUTLINE_WHITE.getRGB());

        List<FormattedCharSequence> orderedTexts = parent.getFont().split(Component.literal(subText), this.width - 20);
        List<ClientTooltipComponent> tooltipComponents = orderedTexts.stream().map(ClientTooltipComponent::create).toList();
        int totalTextHeight = tooltipComponents.stream()
                .mapToInt(tc -> tc.getHeight(parent.getFont()))
                .sum();
        int yOffset = this.y + (this.height / 2) - (totalTextHeight / 2);

        for (ClientTooltipComponent tooltipComponent : tooltipComponents) {
            int lineHeight = tooltipComponent.getHeight(parent.getFont());
            tooltipComponent.extractText(
                    context,
                    parent.getFont(),
                    (this.x + (this.width / 2)) - tooltipComponent.getWidth(parent.getFont()) / 2,
                    yOffset
            );
            yOffset += lineHeight;
        }
        yesButton.extractRenderState(context, (int)mouseX, (int)mouseY, delta);
        noButton.extractRenderState(context, (int)mouseX, (int)mouseY, delta);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled)
    {
        yesButton.onClick(click, doubled);
        noButton.onClick(click, doubled);
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

    public int getHeightOffset() {
        List<FormattedCharSequence> orderedTexts = parent.getFont().split(Component.literal(subText), this.width - 20);
        List<ClientTooltipComponent> tooltipComponents = orderedTexts.stream().map(ClientTooltipComponent::create).toList();
        int height = 0;
        for (ClientTooltipComponent tooltipComponent : tooltipComponents) {
            height += tooltipComponent.getHeight(parent.getFont());
        }
        return height;
    }
}
