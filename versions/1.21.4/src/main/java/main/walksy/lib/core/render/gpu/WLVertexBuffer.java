package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class WLVertexBuffer implements AutoCloseable {
    private static final int MIN_CAPACITY = 65536;
    private int buffer;
    private ByteBuffer staging;
    private int capacity;

    public WLVertexBuffer(final String label) {
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
        if (this.buffer == 0 || this.capacity < bytesNeeded) {
            this.close();
            this.capacity = Math.max(bytesNeeded, MIN_CAPACITY);
            this.buffer = GlStateManager._glGenBuffers();
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, this.buffer);
            GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, (long) this.capacity, GL15.GL_DYNAMIC_DRAW);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            this.staging = MemoryUtil.memAlloc(this.capacity);
        }
        final int target = this.buffer;
        final ByteBuffer bytes = this.staging;
        return new WLMapping(bytes, () -> {
            bytes.position(0).limit(bytesNeeded);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, target);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, bytes);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        });
    }

    ByteBuffer staging() {
        return this.staging;
    }

    int glBuffer() {
        return this.buffer;
    }

    @Override
    public void close() {
        if (this.buffer != 0) {
            GlStateManager._glDeleteBuffers(this.buffer);
            this.buffer = 0;
        }
        if (this.staging != null) {
            MemoryUtil.memFree(this.staging);
            this.staging = null;
        }
    }
}
