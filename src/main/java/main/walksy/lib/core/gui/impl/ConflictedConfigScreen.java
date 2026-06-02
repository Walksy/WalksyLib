package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.gui.widgets.ButtonWidget;
import net.minecraft.client.gui.screens.Screen;

public class ConflictedConfigScreen extends BaseScreen {

    private final Screen toScreen;
    private final Screen toConfig;
    private final String[] buttonTitles;
    private ButtonWidget screenButton;
    private ButtonWidget configButton;

    public ConflictedConfigScreen(final String title, final Screen parent, final Screen toScreen, final Screen toConfig, final String[] buttonTitles) {
        super(title, parent);
        this.toScreen = toScreen;
        this.toConfig = toConfig;
        this.buttonTitles = buttonTitles;
    }

    @Override
    protected void init() {
        super.init();
        final int centerX = this.width / 2 - 25;
        final int centerY = this.height / 2;
        this.screenButton = new ButtonWidget(centerX, centerY - 50 - 4, 50, 50, true, this.buttonTitles[0], () -> this.minecraft.setScreenAndShow(this.toScreen));
        this.configButton = new ButtonWidget(centerX, centerY + 4, 50, 50, true, this.buttonTitles[1], () -> this.minecraft.setScreenAndShow(this.toConfig));
        this.addRenderableWidget(this.screenButton);
        this.addRenderableWidget(this.configButton);
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        if (this.screenButton != null) {
            final int centerX = this.width / 2 - 25;
            this.screenButton.setPosition(centerX, this.height / 2 - 50 - 4);
            this.configButton.setPosition(centerX, this.height / 2 + 4);
        }
    }
}
