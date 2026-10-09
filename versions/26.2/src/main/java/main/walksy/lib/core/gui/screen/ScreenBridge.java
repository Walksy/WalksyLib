package main.walksy.lib.core.gui.screen;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ScreenBridge extends Screen {
    protected ScreenBridge(final Component title) {
        super(title);
    }

    @Override
    public final void extractRenderState(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float delta) {
        this.renderFrame(GuiContext.graphics(extractor), mouseX, mouseY, delta);
    }

    protected abstract void renderFrame(WLGraphics2D graphics, int mouseX, int mouseY, float delta);

    protected final void renderChildren(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics.context().extractor(), mouseX, mouseY, delta);
    }

    @Override
    protected final void extractBlurredBackground(final GuiGraphicsExtractor extractor) {
        this.extractBlurredBackground(GuiContext.graphics(extractor));
    }

    protected void extractBlurredBackground(final WLGraphics2D graphics) {
        super.extractBlurredBackground(graphics.context().extractor());
    }

    @Override
    public final void extractTransparentBackground(final GuiGraphicsExtractor extractor) {
        this.extractTransparentBackground(GuiContext.graphics(extractor));
    }

    protected void extractTransparentBackground(final WLGraphics2D graphics) {
        super.extractTransparentBackground(graphics.context().extractor());
    }

    @Override
    protected final void extractMenuBackground(final GuiGraphicsExtractor extractor, final int x, final int y, final int width, final int height) {
        this.extractMenuBackground(GuiContext.graphics(extractor), x, y, width, height);
    }

    protected void extractMenuBackground(final WLGraphics2D graphics, final int x, final int y, final int width, final int height) {
        super.extractMenuBackground(graphics.context().extractor(), x, y, width, height);
    }
}
