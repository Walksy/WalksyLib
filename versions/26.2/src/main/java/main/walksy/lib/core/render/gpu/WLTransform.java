package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

public final class WLTransform {
    private final GpuBufferSlice slice;

    public WLTransform(final GpuBufferSlice slice) {
        this.slice = slice;
    }

    GpuBufferSlice slice() {
        return this.slice;
    }
}
