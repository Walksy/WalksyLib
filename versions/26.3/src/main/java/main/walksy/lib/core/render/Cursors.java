package main.walksy.lib.core.render;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;

public final class Cursors {
    private Cursor requested = Cursor.DEFAULT;

    public enum Cursor {
        DEFAULT(CursorTypes.ARROW),
        POINTER(CursorTypes.POINTING_HAND),
        TEXT(CursorTypes.IBEAM),
        RESIZE_NS(CursorTypes.RESIZE_NS),
        RESIZE_DIAGONAL(CursorTypes.RESIZE_ALL),
        MOVE(CursorTypes.RESIZE_ALL),
        NOT_ALLOWED(CursorTypes.NOT_ALLOWED);

        private final CursorType type;

        Cursor(final CursorType type) {
            this.type = type;
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
        graphics.context().extractor().requestCursor(this.requested.type);
    }

    public void reset() {
        this.requested = Cursor.DEFAULT;
        Minecraft.getInstance().getWindow().selectCursor(Cursor.DEFAULT.type);
    }
}
