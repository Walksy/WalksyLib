package main.walksy.lib.core.render;

import main.walksy.lib.core.render.element.WLRect;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public final class DevicePixels {
    private static final float UNIT_EPSILON = 0.0005f;
    private static final float NEAR_UNIT_EPSILON = 0.04f;

    private DevicePixels() {
    }

    public static float perLogicalX() {
        return scale();
    }

    public static float perLogicalY() {
        return scale();
    }

    private static float scale() {
        final var window = Minecraft.getInstance().getWindow();
        return (float) Math.max(1, window.getGuiScale());
    }

    public static float logicalWidth() {
        return Minecraft.getInstance().getWindow().getWidth() / scale();
    }

    public static float logicalHeight() {
        return Minecraft.getInstance().getWindow().getHeight() / scale();
    }

    public static float snapX(final float logicalX) {
        final float s = perLogicalX();
        return s <= 0f ? logicalX : Math.round(logicalX * s) / s;
    }

    public static float snapY(final float logicalY) {
        final float s = perLogicalY();
        return s <= 0f ? logicalY : Math.round(logicalY * s) / s;
    }

    public static float snapSizeX(final float logicalSize) {
        final float s = perLogicalX();
        return s <= 0f ? logicalSize : Math.max(1, Math.round(logicalSize * s)) / s;
    }

    public static float snapSizeY(final float logicalSize) {
        final float s = perLogicalY();
        return s <= 0f ? logicalSize : Math.max(1, Math.round(logicalSize * s)) / s;
    }

    public static float snapX(final float logicalX, final Matrix3x2f pose) {
        if (pose == null) {
            return snapX(logicalX);
        }
        if (pose.m01() != 0f || pose.m10() != 0f || pose.m00() == 0f) {
            return logicalX;
        }
        final float s = perLogicalX();
        if (s <= 0f) {
            return logicalX;
        }
        final float device = (pose.m00() * logicalX + pose.m20()) * s;
        return (Math.round(device) / s - pose.m20()) / pose.m00();
    }

    public static float snapY(final float logicalY, final Matrix3x2f pose) {
        if (pose == null) {
            return snapY(logicalY);
        }
        if (pose.m01() != 0f || pose.m10() != 0f || pose.m11() == 0f) {
            return logicalY;
        }
        final float s = perLogicalY();
        if (s <= 0f) {
            return logicalY;
        }
        final float device = (pose.m11() * logicalY + pose.m21()) * s;
        return (Math.round(device) / s - pose.m21()) / pose.m11();
    }

    public static float[] snapEdgesX(final float origin, final float cellSize, final int count, final Matrix3x2f pose) {
        final float[] edges = new float[Math.max(0, count) + 1];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = snapX(origin + i * cellSize, pose);
        }
        return edges;
    }

    public static float[] snapEdgesY(final float origin, final float cellSize, final int count, final Matrix3x2f pose) {
        final float[] edges = new float[Math.max(0, count) + 1];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = snapY(origin + i * cellSize, pose);
        }
        return edges;
    }

    public static boolean isUnscaled(final Matrix3x2f pose) {
        if (pose == null) {
            return true;
        }
        if (pose.m01() != 0f || pose.m10() != 0f) {
            return false;
        }
        return Math.abs(pose.m00() - 1f) <= UNIT_EPSILON && Math.abs(pose.m11() - 1f) <= UNIT_EPSILON;
    }

    public static boolean isNearlyUnscaled(final Matrix3x2f pose) {
        if (pose == null) {
            return true;
        }
        if (pose.m01() != 0f || pose.m10() != 0f) {
            return false;
        }
        return Math.abs(pose.m00() - 1f) <= NEAR_UNIT_EPSILON && Math.abs(pose.m11() - 1f) <= NEAR_UNIT_EPSILON;
    }

    public static boolean isAxisAligned(final Matrix3x2f pose) {
        return pose == null || (pose.m01() == 0f && pose.m10() == 0f);
    }

    public static int[] scissorToDevice(final WLRect scissor) {
        if (scissor == null) {
            return null;
        }
        final var window = Minecraft.getInstance().getWindow();
        final int maxX = Math.max(0, window.getWidth());
        final int maxY = Math.max(0, window.getHeight());
        final float sx = perLogicalX();
        final float sy = perLogicalY();
        final int left = Mth.clamp((int) Math.floor(scissor.left() * sx), 0, maxX);
        final int top = Mth.clamp((int) Math.floor(scissor.top() * sy), 0, maxY);
        final int right = Mth.clamp((int) Math.ceil((scissor.left() + scissor.width()) * sx), left, maxX);
        final int bottom = Mth.clamp((int) Math.ceil((scissor.top() + scissor.height()) * sy), top, maxY);
        return new int[] {left, top, right - left, bottom - top};
    }

    public static int[] scissorToRenderPass(final WLRect scissor, final int targetHeightPx) {
        final int[] rect = scissorToDevice(scissor);
        if (rect == null || rect[2] <= 0 || rect[3] <= 0 || targetHeightPx <= 0) {
            return null;
        }
        final int top = rect[1];
        if (top >= targetHeightPx) {
            return null;
        }
        final int height = Math.min(rect[3], targetHeightPx - top);
        final int y = targetHeightPx - top - height;
        if (height <= 0 || y < 0) {
            return null;
        }
        return new int[] {rect[0], y, rect[2], height};
    }
}
