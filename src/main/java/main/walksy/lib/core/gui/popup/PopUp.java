package main.walksy.lib.core.gui.popup;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public abstract class PopUp {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 100;
    protected final String subText;
    protected WalksyLibConfigScreen parent;
    public boolean visible;
    public int x;
    public int y;
    public int width;
    public int height;
    protected boolean canClose = true;
    protected boolean loaded = false;

    public PopUp(final WalksyLibConfigScreen parent, final String subText) {
        this.parent = parent;
        this.subText = subText;
        this.visible = false;
        this.layout(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.loaded = true;
    }

    public PopUp(final WalksyLibConfigScreen parent, final String subText, final int width, final int height) {
        this.parent = parent;
        this.subText = subText;
        this.layout(width, height);
        this.loaded = true;
    }

    public void extract(final Graphics graphics, final double mouseX, final double mouseY, final float delta) {
        graphics.fillRoundedRectOutline((this.parent.width / 2) - (this.width) / 2, (this.parent.height / 2) - (this.height) / 2, this.width, this.height, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        graphics.fillRoundedRectOutline((this.parent.width / 2) - (this.width / 2) + 1, (this.parent.height / 2) - (this.height / 2) + 1, this.width - 2, this.height - 2, 2, 1, MainColors.OUTLINE_WHITE.getRGB());
        graphics.fillRoundedRect((this.parent.width / 2) - (this.width / 2) + 2, (this.parent.height / 2) - (this.height / 2) + 2, this.width - 4, this.height - 4, 2, Color.BLACK.getRGB());
    }

    public abstract void onClick(MouseButtonEvent click, boolean doubled);
    public void onScroll(final double mouseX, final double mouseY, final double verticalAmount) {}
    public void onMouseRelease(final MouseButtonEvent click) {}

    public void layout(final int requestedWidth, final int requestedHeight) {
        final int maxWidth = (int) (this.parent.width * 0.98);
        final int maxHeight = (int) (this.parent.height * 0.98);
        this.width = Math.min(requestedWidth, maxWidth);
        this.height = Math.min(requestedHeight, maxHeight);
        this.x = (this.parent.width / 2) - (this.width / 2);
        this.y = (this.parent.height / 2) - (this.height / 2);
    }

    public void close() {
        this.parent.popUp = null;
        this.onClose();
    }

    protected abstract void onClose();

    public boolean canClose() {
        return this.canClose;
    }

    public void setParentScreen(final WalksyLibConfigScreen screen) {
        this.parent = screen;
    }
}
