package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MappableRingBuffer;

public final class WLVertexBuffer implements AutoCloseable {
    private static final int MIN_CAPACITY = 65536;
    private static final int MIN_STRIDE = 12;
    private final String label;
    private MappableRingBuffer ring;
    private int capacity;

    public WLVertexBuffer(final String label) {
        this.label = label;
    }

    public WLMapping map(final int bytesNeeded) {
        if (this.ring == null || this.capacity < bytesNeeded) {
            if (this.ring != null) {
                this.ring.close();
            }
            this.capacity = Math.max(bytesNeeded, MIN_CAPACITY);
            this.ring = new MappableRingBuffer(() -> this.label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, this.capacity);
            WLGpu.reserveQuadIndices(this.capacity / MIN_STRIDE);
        } else {
            this.ring.rotate();
        }
        return new WLMapping(RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.ring.currentBuffer(), false, true));
    }

    GpuBuffer buffer() {
        return this.ring.currentBuffer();
    }

    @Override
    public void close() {
        if (this.ring != null) {
            this.ring.close();
            this.ring = null;
        }
    }
}
