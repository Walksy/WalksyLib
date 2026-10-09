package main.walksy.lib.core.render.gpu;

public final class Std140Size {
    private int size;

    public static int roundUp(final int value, final int alignment) {
        return (value + alignment - 1) / alignment * alignment;
    }

    public int get() {
        return this.size;
    }

    public Std140Size align(final int alignment) {
        this.size = roundUp(this.size, alignment);
        return this;
    }

    public Std140Size putFloat() {
        this.align(4);
        this.size += 4;
        return this;
    }

    public Std140Size putInt() {
        this.align(4);
        this.size += 4;
        return this;
    }

    public Std140Size putVec2() {
        this.align(8);
        this.size += 8;
        return this;
    }

    public Std140Size putVec3() {
        this.align(16);
        this.size += 16;
        return this;
    }

    public Std140Size putVec4() {
        this.align(16);
        this.size += 16;
        return this;
    }

    public Std140Size putMat4f() {
        this.align(16);
        this.size += 64;
        return this;
    }
}
