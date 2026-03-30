package main.walksy.lib.core.gui.widgets.sub;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.*;

public abstract class SubWidget {

    protected int x, y, width, height;
    public SubWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);
    public abstract void onClick(MouseButtonEvent click, boolean doubled);
    public abstract void onDrag(int mouseX);
    public void onKeyPress(KeyEvent input) {}
    public void onCharTyped(CharacterEvent input) {}

    public void setPos(Point pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public Point getPos()
    {
        return new Point(this.x, this.y);
    }

    public int getHeight() {
        return this.height;
    }
}
