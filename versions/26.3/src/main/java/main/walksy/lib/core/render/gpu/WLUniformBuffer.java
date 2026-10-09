package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;

public final class WLUniformBuffer implements AutoCloseable {
    private final GpuBuffer buffer;

    public WLUniformBuffer(final String label, final int size) {
        this.buffer = RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, (long) size);
    }

    public WLMapping map() {
        return new WLMapping(this.buffer.map(false, true));
    }

    GpuBuffer buffer() {
        return this.buffer;
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
