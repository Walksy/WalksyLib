package main.walksy.lib.core.render.gpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.MappableRingBuffer;

public final class WLVertexBuffer implements AutoCloseable {
    private static final int MIN_CAPACITY = 65536;
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
        } else {
            this.ring.rotate();
        }
        return new WLMapping(this.ring.currentBuffer().map(false, true));
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
