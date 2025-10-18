package main.walksy.lib.core.gui.popup;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public abstract class PopUp {

    protected final String subText;
    protected WalksyLibConfigScreen parent;
    private final int DEFAULT_WIDTH = 300;
    private final int DEFAULT_HEIGHT = 100;
    public boolean visible;
    public int x;
    public int y;
    public int width;
    public int height;
    protected boolean canClose = true;
    protected boolean loaded = false;

    public PopUp(WalksyLibConfigScreen parent, String subText)
    {
        this.parent = parent;
        this.subText = subText;
        this.visible = false;
        layout(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        loaded = true;
    }

    public PopUp(WalksyLibConfigScreen parent, String subText, int width, int height)
    {
        this.parent = parent;
        this.subText = subText;
        layout(width, height);
        this.loaded = true;
    }

    public void render(DrawContext context, double mouseX, double mouseY, float delta)
    {
        Renderer2D.fillRoundedRectOutline(context,(parent.width / 2) - (this.width) / 2, (parent.height / 2) - (this.height) / 2, this.width, this.height, 2, 1, MainColors.OUTLINE_BLACK.getRGB());
        Renderer2D.fillRoundedRectOutline(context,(parent.width / 2) - (this.width / 2) + 1, (parent.height / 2) - (this.height / 2) + 1, this.width - 2, this.height - 2, 2, 1, MainColors.OUTLINE_WHITE.getRGB());
        Renderer2D.fillRoundedRect(context,(parent.width / 2) - (this.width / 2) + 2, (parent.height / 2) - (this.height / 2) + 2, this.width - 4, this.height - 4, 2, Color.BLACK.getRGB());
    }

    public abstract void onClick(double mouseX, double mouseY, int button);
    public void onScroll(double mouseX, double mouseY, double verticalAmount) {}
    public void onMouseRelease(double mouseX, double mouseY, int button) {}

    public void layout(int requestedWidth, int requestedHeight) {
        int maxWidth = (int) (parent.width * 0.98);
        int maxHeight = (int) (parent.height * 0.98);

        this.width = Math.min(requestedWidth, maxWidth);
        this.height = Math.min(requestedHeight, maxHeight);

        this.x = (parent.width / 2) - (this.width / 2);
        this.y = (parent.height / 2) - (this.height / 2);
    }

    public void close()
    {
        this.parent.popUp = null;
        onClose();
    }

    protected abstract void onClose();

    public boolean canClose()
    {
        return this.canClose;
    }

    public void setParentScreen(WalksyLibConfigScreen screen)
    {
        this.parent = screen;
    }
}
