package main.walksy.lib.core.gui.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BaseScreen extends Screen {

    public final Screen parent;
    public int tickCount = 0;

    protected BaseScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    public void addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }


    protected void renderBackgroundLayer(GuiGraphicsExtractor context, float delta) {
        if (this.minecraft.level == null) {
            this.extractPanorama(context, delta);
        }
        this.extractBlurredBackground(context);
        this.extractMenuBackground(context);
    }
}
