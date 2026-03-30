package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.gui.widgets.ButtonWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public class ConflictedConfigScreen extends BaseScreen {

    private final ButtonWidget screenButton;
    private final ButtonWidget configButton;

    public ConflictedConfigScreen(String title, Screen parent, Screen toScreen, Screen toConfig, String[] buttonTitles) {
        super(title, parent);
        Minecraft client = Minecraft.getInstance();
        int centerX = client.getWindow().getGuiScaledWidth() / 2 - 25;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        this.screenButton = new ButtonWidget(centerX, centerY - 50 - 4, 50, 50, true, buttonTitles[0], () -> client.setScreen(toScreen));
        this.configButton = new ButtonWidget(centerX, centerY + 4, 50, 50, true, buttonTitles[1], () -> client.setScreen(toConfig));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        this.screenButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
        this.configButton.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.screenButton.onClick(event, doubleClick);
        this.configButton.onClick(event, doubleClick);
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        int centerX = this.width / 2 - 25;
        this.screenButton.setPosition(centerX, this.height / 2 - 50 - 4);
        this.configButton.setPosition(centerX, this.height / 2 + 4);
    }
}
