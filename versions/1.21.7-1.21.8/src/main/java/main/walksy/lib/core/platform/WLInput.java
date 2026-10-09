package main.walksy.lib.core.platform;

import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.platform.input.CharacterEvent;
import main.walksy.lib.core.platform.input.KeyEvent;
import main.walksy.lib.core.platform.input.MouseButtonEvent;
import main.walksy.lib.core.platform.input.MouseButtonInfo;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import org.lwjgl.glfw.GLFW;

public final class WLInput {
    private static final long DOUBLE_CLICK_MILLIS = 250L;
    private static long lastClickTime;
    private static int lastClickButton = -1;
    private static boolean doubleClick;
    private static int mouseModifiers;

    private WLInput() {
    }

    public static void mouseButton(final int button, final int action, final int modifiers) {
        mouseModifiers = modifiers;
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        final long now = Util.getMillis();
        doubleClick = now - lastClickTime < DOUBLE_CLICK_MILLIS && lastClickButton == button;
        lastClickTime = now;
        lastClickButton = button;
    }

    public static boolean doubleClick() {
        return doubleClick;
    }

    public static MouseButtonEvent mouse(final double x, final double y, final int button) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, mouseModifiers));
    }

    public static boolean mouseClicked(final AbstractWidget widget, final MouseButtonEvent event, final boolean doubleClick) {
        if (widget instanceof final WLWidget walksy) {
            return walksy.mouseClicked(event, doubleClick);
        }
        return widget.mouseClicked(event.x(), event.y(), event.button());
    }

    public static boolean mouseDragged(final AbstractWidget widget, final MouseButtonEvent event, final double dragX, final double dragY) {
        if (widget instanceof final WLWidget walksy) {
            return walksy.mouseDragged(event, dragX, dragY);
        }
        return widget.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    public static boolean mouseReleased(final AbstractWidget widget, final MouseButtonEvent event) {
        if (widget instanceof final WLWidget walksy) {
            return walksy.mouseReleased(event);
        }
        return widget.mouseReleased(event.x(), event.y(), event.button());
    }

    public static boolean keyPressed(final AbstractWidget widget, final KeyEvent event) {
        if (widget instanceof final WLWidget walksy) {
            return walksy.keyPressed(event);
        }
        return widget.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public static boolean charTyped(final AbstractWidget widget, final CharacterEvent event) {
        if (widget instanceof final WLWidget walksy) {
            return walksy.charTyped(event);
        }
        boolean handled = false;
        for (final char character : Character.toChars(event.codepoint())) {
            handled |= widget.charTyped(character, event.modifiers());
        }
        return handled;
    }

    public static void textFocusChanged(final Object owner, final boolean focused) {
    }
}
