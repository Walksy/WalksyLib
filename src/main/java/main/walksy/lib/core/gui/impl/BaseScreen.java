package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.gui.Graphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BaseScreen extends Screen {

    public final Screen parent;
    private int upTime = 0;
    protected float delta;
    protected boolean suppressWidgetMouse = false;

    protected BaseScreen(final String title, final Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void tick() {
        super.tick();
        this.upTime++;
    }

    public void addWidget(final AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        this.delta = delta;
        final int renderMouseX = this.suppressWidgetMouse ? 0 : mouseX;
        final int renderMouseY = this.suppressWidgetMouse ? 0 : mouseY;
        this.suppressWidgetMouse = false;
        super.extractRenderState(context, renderMouseX, renderMouseY, delta);
        this.extract(new Graphics(context), mouseX, mouseY);
    }

    protected void extract(final Graphics graphics, final int mouseX, final int mouseY) {}

    @Override
    protected void extractBlurredBackground(final GuiGraphicsExtractor graphics) {
        graphics.blurBeforeThisStratum();
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.isInGameUi()) {
            this.extractTransparentBackground(graphics);
        } else {
            if (this.minecraft.level == null) {
                this.extractPanorama(graphics, a);
            }

            this.extractBlurredBackground(graphics);
        }

        this.minecraft.gui.hud.extractDeferredSubtitles();
    }

    public int getUpTime() {
        return this.upTime;
    }
}
