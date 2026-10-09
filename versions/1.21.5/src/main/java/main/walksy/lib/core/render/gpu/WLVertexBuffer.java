package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class WLVertexBuffer implements AutoCloseable {
    private static final int MIN_CAPACITY = 65536;
    private static final int MIN_STRIDE = 12;
    private final String label;
    private GpuBuffer buffer;
    private ByteBuffer staging;
    private int capacity;

    public WLVertexBuffer(final String label) {
        this.label = label;
    }

    public WLMapping map(final int bytesNeeded) {
        if (VulkanBackend.active()) {
            if (this.staging == null || this.capacity < bytesNeeded) {
                this.close();
                this.capacity = Math.max(bytesNeeded, MIN_CAPACITY);
                this.staging = MemoryUtil.memAlloc(this.capacity);
            }
            return new WLMapping(this.staging, () -> {
            });
        }
        if (this.buffer == null || this.capacity < bytesNeeded) {
            this.close();
            this.capacity = Math.max(bytesNeeded, MIN_CAPACITY);
            this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, this.capacity);
            this.staging = MemoryUtil.memAlloc(this.capacity);
        }
        final GpuBuffer target = this.buffer;
        final ByteBuffer bytes = this.staging;
        return new WLMapping(bytes, () -> {
            bytes.position(0).limit(bytesNeeded);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(target, bytes, 0);
            RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(Math.max(6, bytesNeeded / MIN_STRIDE / 4 * 6));
        });
    }

    ByteBuffer staging() {
        return this.staging;
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
