package main.walksy.lib.core.render.gpu;

import org.joml.Matrix4fc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Std140Writer {
    private final ByteBuffer buffer;
    private final int start;

    private Std140Writer(final ByteBuffer buffer) {
        this.buffer = buffer.order(ByteOrder.nativeOrder());
        this.start = buffer.position();
    }

    public static Std140Writer into(final ByteBuffer buffer) {
        return new Std140Writer(buffer);
    }

    public ByteBuffer buffer() {
        return this.buffer;
    }

    public Std140Writer align(final int alignment) {
        final int offset = this.buffer.position() - this.start;
        this.buffer.position(this.start + Std140Size.roundUp(offset, alignment));
        return this;
    }

    public Std140Writer putFloat(final float value) {
        this.align(4);
        this.buffer.putFloat(value);
        return this;
    }

    public Std140Writer putInt(final int value) {
        this.align(4);
        this.buffer.putInt(value);
        return this;
    }

    public Std140Writer putVec2(final float x, final float y) {
        this.align(8);
        this.buffer.putFloat(x);
        this.buffer.putFloat(y);
        return this;
    }

    public Std140Writer putVec3(final float x, final float y, final float z) {
        this.align(16);
        this.buffer.putFloat(x);
        this.buffer.putFloat(y);
        this.buffer.putFloat(z);
        this.buffer.position(this.buffer.position() + 4);
        return this;
    }

    public Std140Writer putVec4(final float x, final float y, final float z, final float w) {
        this.align(16);
        this.buffer.putFloat(x);
        this.buffer.putFloat(y);
        this.buffer.putFloat(z);
        this.buffer.putFloat(w);
        return this;
    }

    public Std140Writer putMat4f(final Matrix4fc matrix) {
        this.align(16);
        matrix.get(this.buffer);
        this.buffer.position(this.buffer.position() + 64);
        return this;
    }
}
