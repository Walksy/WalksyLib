package main.walksy.lib.core.render.gpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class WLVertexBuffer implements AutoCloseable {
    private static final int MIN_CAPACITY = 65536;
    private final String label;
    private GpuBuffer buffer;
    private ByteBuffer staging;
    private int capacity;

    public WLVertexBuffer(final String label) {
        this.label = label;
    }

    public WLMapping map(final int bytesNeeded) {
        if (this.buffer == null || this.capacity < bytesNeeded) {
            this.close();
            this.capacity = Math.max(bytesNeeded, MIN_CAPACITY);
            this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, this.capacity);
            this.staging = MemoryUtil.memAlloc(this.capacity);
        }
        final GpuBuffer target = this.buffer;
        final ByteBuffer bytes = this.staging;
        return new WLMapping(bytes, () -> {
            if (bytesNeeded > 0) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(target.slice(0, bytesNeeded), bytes.clear().limit(bytesNeeded));
            }
        });
    }

    GpuBuffer buffer() {
        return this.buffer;
    }

    @Override
    public void close() {
        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
        if (this.staging != null) {
            MemoryUtil.memFree(this.staging);
            this.staging = null;
        }
    }
}
