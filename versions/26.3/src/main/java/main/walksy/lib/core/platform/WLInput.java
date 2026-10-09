package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public final class WLInput {
    private WLInput() {
    }

    public static boolean mouseClicked(final AbstractWidget widget, final MouseButtonEvent event, final boolean doubleClick) {
        return widget.mouseClicked(event, doubleClick);
    }

    public static boolean mouseDragged(final AbstractWidget widget, final MouseButtonEvent event, final double dragX, final double dragY) {
        return widget.mouseDragged(event, dragX, dragY);
    }

    public static boolean mouseReleased(final AbstractWidget widget, final MouseButtonEvent event) {
        return widget.mouseReleased(event);
    }

    public static boolean keyPressed(final AbstractWidget widget, final KeyEvent event) {
        return widget.keyPressed(event);
    }

    public static boolean charTyped(final AbstractWidget widget, final CharacterEvent event) {
        return widget.charTyped(event);
    }

    public static void textFocusChanged(final Object owner, final boolean focused) {
        Minecraft.getInstance().textInputManager().onTextInputFocusChange(owner, focused);
    }
}
