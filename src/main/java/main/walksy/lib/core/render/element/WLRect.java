package main.walksy.lib.core.render.element;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

public record WLRect(int left, int top, int width, int height) {

    public int right() {
        return this.left + this.width;
    }

    public int bottom() {
        return this.top + this.height;
    }

    public @Nullable WLRect intersection(final WLRect other) {
        final int l = Math.max(this.left(), other.left());
        final int t = Math.max(this.top(), other.top());
        final int r = Math.min(this.right(), other.right());
        final int b = Math.min(this.bottom(), other.bottom());
        if (l >= r || t >= b) {
            return null;
        }
        return new WLRect(l, t, r - l, b - t);
    }

    public WLRect transformMaxBounds(final Matrix3x2fc pose) {
        final float ax = pose.m00() * this.left() + pose.m10() * this.top() + pose.m20();
        final float ay = pose.m01() * this.left() + pose.m11() * this.top() + pose.m21();
        final float bx = pose.m00() * this.right() + pose.m10() * this.top() + pose.m20();
        final float by = pose.m01() * this.right() + pose.m11() * this.top() + pose.m21();
        final float cx = pose.m00() * this.left() + pose.m10() * this.bottom() + pose.m20();
        final float cy = pose.m01() * this.left() + pose.m11() * this.bottom() + pose.m21();
        final float dx = pose.m00() * this.right() + pose.m10() * this.bottom() + pose.m20();
        final float dy = pose.m01() * this.right() + pose.m11() * this.bottom() + pose.m21();
        final float minX = Math.min(Math.min(ax, cx), Math.min(bx, dx));
        final float maxX = Math.max(Math.max(ax, cx), Math.max(bx, dx));
        final float minY = Math.min(Math.min(ay, cy), Math.min(by, dy));
        final float maxY = Math.max(Math.max(ay, cy), Math.max(by, dy));
        return new WLRect((int) Math.floor(minX), (int) Math.floor(minY), (int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY));
    }

    public static @Nullable WLRect clipped(final WLRect bounds, @Nullable final WLRect scissor) {
        return scissor != null ? scissor.intersection(bounds) : bounds;
    }
}
