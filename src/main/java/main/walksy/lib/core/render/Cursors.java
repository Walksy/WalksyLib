package main.walksy.lib.core.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Cursors {
    private final long[] handles = new long[Cursor.values().length];
    private final boolean[] resolved = new boolean[Cursor.values().length];
    private Cursor requested = Cursor.DEFAULT;
    private Cursor applied;

    public enum Cursor {
        DEFAULT(GLFW.GLFW_ARROW_CURSOR),
        POINTER(GLFW.GLFW_POINTING_HAND_CURSOR),
        TEXT(GLFW.GLFW_IBEAM_CURSOR),
        RESIZE_NS(GLFW.GLFW_RESIZE_NS_CURSOR),
        RESIZE_DIAGONAL(GLFW.GLFW_RESIZE_NWSE_CURSOR),
        MOVE(GLFW.GLFW_RESIZE_ALL_CURSOR),
        NOT_ALLOWED(GLFW.GLFW_NOT_ALLOWED_CURSOR);

        private final int glfwShape;

        Cursor(final int glfwShape) {
            this.glfwShape = glfwShape;
        }
    }

    public void beginFrame() {
        this.requested = Cursor.DEFAULT;
    }

    public void request(final Cursor cursor) {
        if (cursor != null) {
            this.requested = cursor;
        }
    }

    public void requestIf(final boolean when, final Cursor cursor) {
        if (when) {
            this.request(cursor);
        }
    }

    public void requestControl(final boolean hovered, final boolean available) {
        if (hovered) {
            this.request(available ? Cursor.POINTER : Cursor.NOT_ALLOWED);
        }
    }

    public void apply(final WLGraphics2D graphics) {
        if (this.requested == this.applied) {
            return;
        }
        this.applied = this.requested;
        this.set(this.requested);
    }

    public void reset() {
        this.applied = null;
        this.requested = Cursor.DEFAULT;
        this.set(Cursor.DEFAULT);
    }

    private void set(final Cursor cursor) {
        final long window = Minecraft.getInstance().getWindow().handle();
        if (window == 0L) {
            return;
        }
        GLFW.glfwSetCursor(window, cursor == Cursor.DEFAULT ? 0L : this.handle(cursor));
    }

    private long handle(final Cursor cursor) {
        final int index = cursor.ordinal();
        if (!this.resolved[index]) {
            this.resolved[index] = true;
            this.handles[index] = GLFW.glfwCreateStandardCursor(cursor.glfwShape);
        }
        return this.handles[index];
    }
}
