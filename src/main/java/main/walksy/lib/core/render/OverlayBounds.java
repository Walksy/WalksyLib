package main.walksy.lib.core.render;

public final class OverlayBounds {
    private float left;
    private float top;
    private float right;
    private float bottom;
    private boolean any;

    public OverlayBounds() {
        this.reset();
    }

    public void reset() {
        this.left = Float.MAX_VALUE;
        this.top = Float.MAX_VALUE;
        this.right = -Float.MAX_VALUE;
        this.bottom = -Float.MAX_VALUE;
        this.any = false;
    }

    public void add(final float x, final float y) {
        if (x < this.left) {
            this.left = x;
        }
        if (x > this.right) {
            this.right = x;
        }
        if (y < this.top) {
            this.top = y;
        }
        if (y > this.bottom) {
            this.bottom = y;
        }
        this.any = true;
    }

    public boolean isEmpty() {
        return !this.any;
    }

    public float[] toDeviceRect() {
        if (!this.any) {
            return null;
        }
        final float scaleX = DevicePixels.perLogicalX();
        final float scaleY = DevicePixels.perLogicalY();
        final int fbH = WLRenderSystem.targetHeight();
        final float padX = scaleX + 1f;
        final float padY = scaleY + 1f;
        return new float[] { this.left * scaleX - padX, fbH - this.bottom * scaleY - padY, this.right * scaleX + padX, fbH - this.top * scaleY + padY };
    }
}
