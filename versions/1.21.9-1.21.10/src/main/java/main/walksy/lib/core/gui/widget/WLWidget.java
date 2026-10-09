package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public abstract class WLWidget extends AbstractWidget {
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
}
