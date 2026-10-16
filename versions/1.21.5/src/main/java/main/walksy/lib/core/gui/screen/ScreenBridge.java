package main.walksy.lib.core.gui.screen;

import main.walksy.lib.core.platform.WLInput;
import main.walksy.lib.core.platform.input.CharacterEvent;
import main.walksy.lib.core.platform.input.KeyEvent;
import main.walksy.lib.core.platform.input.MouseButtonEvent;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ScreenBridge extends Screen {
    protected ScreenBridge(final Component title) {
        super(title);
    }

    @Override
    public final void render(final GuiGraphics extractor, final int mouseX, final int mouseY, final float delta) {
        GuiContext.beginFrame(extractor);
        try {
            this.renderFrame(GuiContext.graphics(extractor), mouseX, mouseY, delta);
        } finally {
            GuiContext.endFrame();
        }
    }

    protected abstract void renderFrame(WLGraphics2D graphics, int mouseX, int mouseY, float delta);

    protected final void renderChildren(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        graphics.context().immediateScreen(extractor -> this.renderBackground(extractor, mouseX, mouseY, delta));
        for (final Renderable renderable : this.renderables) {
            graphics.context().renderable(renderable, mouseX, mouseY, delta);
        }
    }

    @Override
    public final void resize(final Minecraft minecraft, final int width, final int height) {
        this.resize(width, height);
    }

    public void resize(final int width, final int height) {
        super.resize(this.minecraft, width, height);
    }

    @Override
    protected final void renderBlurredBackground() {
        this.extractBlurredBackground(GuiContext.current());
    }

    protected void extractBlurredBackground(final WLGraphics2D graphics) {
        super.renderBlurredBackground();
    }

    @Override
    public final void renderBackground(final GuiGraphics extractor, final int mouseX, final int mouseY, final float delta) {
        this.extractBackground(GuiContext.graphics(extractor), mouseX, mouseY, delta);
    }

    protected void extractBackground(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        super.renderBackground(graphics.context().extractor(), mouseX, mouseY, delta);
    }

    protected final void extractPanorama(final WLGraphics2D graphics, final float delta) {
        super.renderPanorama(graphics.context().extractor(), delta);
    }

    protected final void extractScreenBlur(final WLGraphics2D graphics, final float delta) {
        this.renderBlurredBackground();
    }

    @Override
    public final void renderTransparentBackground(final GuiGraphics extractor) {
        this.extractTransparentBackground(GuiContext.graphics(extractor));
    }

    protected void extractTransparentBackground(final WLGraphics2D graphics) {
    }

    @Override
    protected final void renderMenuBackground(final GuiGraphics extractor, final int x, final int y, final int width, final int height) {
        this.extractMenuBackground(GuiContext.graphics(extractor), x, y, width, height);
    }

    protected void extractMenuBackground(final WLGraphics2D graphics, final int x, final int y, final int width, final int height) {
        super.renderMenuBackground(graphics.context().extractor(), x, y, width, height);
    }

    @Override
    public final boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.mouseClicked(WLInput.mouse(mouseX, mouseY, button), WLInput.doubleClick());
    }

    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return this.mouseReleased(WLInput.mouse(mouseX, mouseY, button));
    }

    public boolean mouseReleased(final MouseButtonEvent event) {
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        return this.mouseDragged(WLInput.mouse(mouseX, mouseY, button), dragX, dragY);
    }

    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public final boolean keyPressed(final int key, final int scancode, final int modifiers) {
        return this.keyPressed(new KeyEvent(key, scancode, modifiers));
    }

    public boolean keyPressed(final KeyEvent event) {
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean keyReleased(final int key, final int scancode, final int modifiers) {
        return this.keyReleased(new KeyEvent(key, scancode, modifiers));
    }

    public boolean keyReleased(final KeyEvent event) {
        return super.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean charTyped(final char character, final int modifiers) {
        return this.charTyped(new CharacterEvent(character, modifiers));
    }

    public boolean charTyped(final CharacterEvent event) {
        return super.charTyped((char) event.codepoint(), event.modifiers());
    }
}
