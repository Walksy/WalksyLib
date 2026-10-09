package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.platform.WLInput;
import main.walksy.lib.core.platform.input.CharacterEvent;
import main.walksy.lib.core.platform.input.KeyEvent;
import main.walksy.lib.core.platform.input.MouseButtonEvent;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class WLWidget extends AbstractWidget {
    private @Nullable MouseButtonEvent pendingEvent;
    private boolean pendingDoubleClick;

    public WLWidget(final int x, final int y, final int width, final int height) {
        this(x, y, width, height, Component.empty());
    }

    public WLWidget(final int x, final int y, final int width, final int height, final Component message) {
        super(x, y, width, height, message);
    }

    public float getAlpha() {
        return this.alpha;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}

    @Override
    public void playDownSound(final SoundManager soundManager) {}

    @Override
    protected final void renderWidget(final GuiGraphics extractor, final int mouseX, final int mouseY, final float a) {
        this.extract(GuiContext.graphics(extractor), mouseX, mouseY, a);
    }

    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
    }

    @Override
    public final boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.mouseClicked(WLInput.mouse(mouseX, mouseY, button), WLInput.doubleClick());
    }

    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        this.pendingEvent = event;
        this.pendingDoubleClick = doubleClick;
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public final void onClick(final double mouseX, final double mouseY) {
        this.onClick(this.pending(mouseX, mouseY), this.pendingDoubleClick);
    }

    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        super.onClick(event.x(), event.y());
    }

    @Override
    public final boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return this.mouseReleased(WLInput.mouse(mouseX, mouseY, button));
    }

    public boolean mouseReleased(final MouseButtonEvent event) {
        this.pendingEvent = event;
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public final void onRelease(final double mouseX, final double mouseY) {
        this.onRelease(this.pending(mouseX, mouseY));
    }

    public void onRelease(final MouseButtonEvent event) {
        super.onRelease(event.x(), event.y());
    }

    @Override
    public final boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX, final double dragY) {
        return this.mouseDragged(WLInput.mouse(mouseX, mouseY, button), dragX, dragY);
    }

    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        this.pendingEvent = event;
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    protected final void onDrag(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        this.onDrag(this.pending(mouseX, mouseY), dragX, dragY);
    }

    protected void onDrag(final MouseButtonEvent event, final double dragX, final double dragY) {
        super.onDrag(event.x(), event.y(), dragX, dragY);
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

    private MouseButtonEvent pending(final double mouseX, final double mouseY) {
        final MouseButtonEvent event = this.pendingEvent;
        if (event != null && event.x() == mouseX && event.y() == mouseY) {
            return event;
        }
        return WLInput.mouse(mouseX, mouseY, 0);
    }
}
