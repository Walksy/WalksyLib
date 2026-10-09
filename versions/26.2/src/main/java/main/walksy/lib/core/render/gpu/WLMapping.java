package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class WLMapping implements AutoCloseable {
    private final GpuBufferSlice.MappedView view;

    public WLMapping(final GpuBufferSlice.MappedView view) {
        this.view = view;
    }

    public ByteBuffer data() {
        return this.view.data().order(ByteOrder.nativeOrder());
    }

    public Std140Writer writer() {
        return Std140Writer.into(this.view.data());
    }

    @Override
    public void close() {
        this.view.close();
    }
}
