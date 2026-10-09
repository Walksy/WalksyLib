package main.walksy.lib.core.platform.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public interface InputWithModifiers {
    int NOT_DIGIT = -1;

    int input();

    int modifiers();

    default boolean isSelection() {
        return this.input() == GLFW.GLFW_KEY_ENTER || this.input() == GLFW.GLFW_KEY_SPACE || this.input() == GLFW.GLFW_KEY_KP_ENTER;
    }

    default boolean isConfirmation() {
        return this.input() == GLFW.GLFW_KEY_ENTER || this.input() == GLFW.GLFW_KEY_KP_ENTER;
    }

    default boolean isEscape() {
        return this.input() == GLFW.GLFW_KEY_ESCAPE;
    }

    default boolean isLeft() {
        return this.input() == GLFW.GLFW_KEY_LEFT;
    }

    default boolean isRight() {
        return this.input() == GLFW.GLFW_KEY_RIGHT;
    }

    default boolean isUp() {
        return this.input() == GLFW.GLFW_KEY_UP;
    }

    default boolean isDown() {
        return this.input() == GLFW.GLFW_KEY_DOWN;
    }

    default boolean isCycleFocus() {
        return this.input() == GLFW.GLFW_KEY_TAB;
    }

    default int getDigit() {
        final int digit = this.input() - GLFW.GLFW_KEY_0;
        return digit >= 0 && digit <= 9 ? digit : NOT_DIGIT;
    }

    default boolean hasAltDown() {
        return (this.modifiers() & GLFW.GLFW_MOD_ALT) != 0;
    }

    default boolean hasShiftDown() {
        return (this.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    default boolean hasControlDown() {
        return (this.modifiers() & (Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL)) != 0;
    }

    default boolean isSelectAll() {
        return this.input() == GLFW.GLFW_KEY_A && this.hasControlDown() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default boolean isCopy() {
        return this.input() == GLFW.GLFW_KEY_C && this.hasControlDown() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default boolean isPaste() {
        return this.input() == GLFW.GLFW_KEY_V && this.hasControlDown() && !this.hasShiftDown() && !this.hasAltDown();
    }

    default boolean isCut() {
        return this.input() == GLFW.GLFW_KEY_X && this.hasControlDown() && !this.hasShiftDown() && !this.hasAltDown();
    }
}
