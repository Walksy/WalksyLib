package main.walksy.lib.core.gui.widgets.sub;

import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.function.Consumer;

public abstract class SubWidget {

    protected int x, y, width, height;
    public SubWidget(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(DrawContext context, int mouseX, int mouseY);
    public abstract void onClick(int mouseX, int mouseY, int button);
    public abstract void onDrag(int mouseX);

    public void setPos(Point pos)
    {
        this.x = pos.x;
        this.y = pos.y;
    }
}
