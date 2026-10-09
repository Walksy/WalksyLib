package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.gpu.WLVertexFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ByteVertexSink implements WLVertexSink {
    private static final int VERTICES_PER_QUAD = 4;
    private ByteBuffer buffer = ByteBuffer.allocate(65536).order(ByteOrder.nativeOrder());
    private WLVertexFormat format = WLVertexFormat.POSITION_COLOR;
    private boolean pending;
    private float x;
    private float y;
    private float u;
    private float v;
    private int color;
    private int vertices;

    public void reset() {
        this.buffer.clear();
        this.pending = false;
        this.vertices = 0;
    }

    public void format(final WLVertexFormat format) {
        this.finish();
        this.format = format;
        this.vertices = 0;
        this.alignTo(format.bytesPerVertex() * VERTICES_PER_QUAD);
    }

    public int position() {
        return this.buffer.position();
    }

    public ByteBuffer contents() {
        final ByteBuffer view = this.buffer.duplicate().order(ByteOrder.nativeOrder());
        view.flip();
        return view;
    }

    @Override
    public WLVertexSink vertex(final float x, final float y) {
        if (this.pending) {
            this.write();
        }
        this.x = x;
        this.y = y;
        this.u = 0f;
        this.v = 0f;
        this.color = 0xFFFFFFFF;
        this.pending = true;
        return this;
    }

    @Override
    public WLVertexSink uv(final float u, final float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public WLVertexSink color(final int argb) {
        this.color = argb;
        return this;
    }

    public int finish() {
        if (this.pending) {
            this.write();
            this.pending = false;
        }
        return this.vertices;
    }

    private void write() {
        this.ensure(this.format.bytesPerVertex());
        this.buffer.putFloat(this.x).putFloat(this.y).putFloat(0f);
        if (this.format.hasUv()) {
            this.buffer.putFloat(this.u).putFloat(this.v);
        }
        if (this.format.hasColor()) {
            this.buffer.put((byte) ((this.color >> 16) & 0xFF));
            this.buffer.put((byte) ((this.color >> 8) & 0xFF));
            this.buffer.put((byte) (this.color & 0xFF));
            this.buffer.put((byte) ((this.color >>> 24) & 0xFF));
        }
        this.vertices++;
    }

    private void alignTo(final int stride) {
        final int remainder = this.buffer.position() % stride;
        if (remainder == 0) {
            return;
        }
        final int padding = stride - remainder;
        this.ensure(padding);
        for (int i = 0; i < padding; i++) {
            this.buffer.put((byte) 0);
        }
    }

    private void ensure(final int bytes) {
        if (this.buffer.remaining() >= bytes) {
            return;
        }
        final ByteBuffer bigger = ByteBuffer.allocate(Math.max(this.buffer.capacity() * 2, this.buffer.position() + bytes)).order(ByteOrder.nativeOrder());
        this.buffer.flip();
        bigger.put(this.buffer);
        this.buffer = bigger;
    }
}
