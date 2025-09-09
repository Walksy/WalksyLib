package main.walksy.lib.core.gui.widgets.sub;

import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

public interface SubWidget {
    void render(DrawContext context, int mouseX, int mouseY);
    void onClick(int mouseX, int mouseY, int button);
    void onDrag(int mouseX);
    void onWidgetUpdate();
}
