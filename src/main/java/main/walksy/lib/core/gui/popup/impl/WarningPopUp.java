package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WarningPopUp extends PopUp {
    private final String title;
    public final ButtonWidget yesButton;
    public final ButtonWidget noButton;

    public WarningPopUp(final WalksyLibConfigScreen parent, final String title, final String message, final Runnable yesAction, final Runnable noAction) {
        super(parent, message);
        this.title = title;
        this.yesButton = new ButtonWidget((this.x + this.width) - 60, (this.y + this.height) - 33, 50, 20, false, "Yes", yesAction);
        this.noButton = new ButtonWidget((this.x) + 10, (this.y + this.height) - 33, 50, 20, false, "No", noAction);
    }

    @Override
    public void extract(final Graphics graphics, final double mouseX, final double mouseY, final float delta) {
        super.extract(graphics, mouseX, mouseY, delta);
        final GuiGraphicsExtractor extractor = graphics.extractor();
        extractor.pose().pushMatrix();
        final float scale = 1.5F;
        extractor.pose().scale(scale, scale);
        extractor.centeredText(
                this.parent.getFont(),
                this.title,
                (int) ((this.parent.width / 2) / scale),
                (int) ((this.y + 11) / scale),
                -1
        );
        extractor.pose().popMatrix();
        extractor.horizontalLine(this.x + 2, this.x + this.width - 3, this.y + 30, MainColors.OUTLINE_WHITE.getRGB());
        final List<net.minecraft.util.FormattedCharSequence> orderedTexts = this.parent.getFont().split(Component.literal(this.subText), this.width - 20);
        final List<ClientTooltipComponent> tooltipComponents = orderedTexts.stream().map(ClientTooltipComponent::create).toList();
        final int totalTextHeight = tooltipComponents.stream()
                .mapToInt(tc -> tc.getHeight(this.parent.getFont()))
                .sum();
        int yOffset = this.y + (this.height / 2) - (totalTextHeight / 2);
        for (final ClientTooltipComponent tooltipComponent : tooltipComponents) {
            final int lineHeight = tooltipComponent.getHeight(this.parent.getFont());
            tooltipComponent.extractText(
                    extractor,
                    this.parent.getFont(),
                    (this.x + (this.width / 2)) - tooltipComponent.getWidth(this.parent.getFont()) / 2,
                    yOffset
            );
            yOffset += lineHeight;
        }
        this.yesButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
        this.noButton.extractRenderState(extractor, (int) mouseX, (int) mouseY, delta);
    }

    @Override
    public void onClick(final MouseButtonEvent click, final boolean doubled) {
        this.yesButton.onClick(click, doubled);
        this.noButton.onClick(click, doubled);
    }

    @Override
    protected void onClose() {}

    @Override
    public void layout(final int width, final int height) {
        super.layout(width, this.getHeightOffset() + 90);
        if (this.yesButton != null && this.noButton != null) {
            this.yesButton.setPosition((this.x + width) - 60, (this.y + this.height) - 33);
            this.noButton.setPosition((this.x) + 10, (this.y + this.height) - 33);
        }
    }

    public int getHeightOffset() {
        final List<net.minecraft.util.FormattedCharSequence> orderedTexts = this.parent.getFont().split(Component.literal(this.subText), this.width - 20);
        final List<ClientTooltipComponent> tooltipComponents = orderedTexts.stream().map(ClientTooltipComponent::create).toList();
        int height = 0;
        for (final ClientTooltipComponent tooltipComponent : tooltipComponents) {
            height += tooltipComponent.getHeight(this.parent.getFont());
        }
        return height;
    }
}
