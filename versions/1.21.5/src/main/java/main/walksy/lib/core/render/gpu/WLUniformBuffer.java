package main.walksy.lib.core.render.gpu;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class WLUniformBuffer implements AutoCloseable {
    private final ByteBuffer data;
    private int buffer;
    private boolean allocated;

    public WLUniformBuffer(final String label, final int size) {
        this.data = MemoryUtil.memCalloc(size);
    }

    public WLMapping map() {
        return new WLMapping(this.data, this::upload);
    }

    ByteBuffer data() {
        return this.data;
    }

    int glBuffer() {
        if (!this.allocated) {
            this.upload();
        }
        return this.buffer;
    }

    private void upload() {
        if (VulkanBackend.active()) {
            return;
        }
        if (this.buffer == 0) {
            this.buffer = GL15.glGenBuffers();
        }
        this.data.clear();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.buffer);
        if (this.allocated) {
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0L, this.data);
        } else {
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, this.data, GL15.GL_DYNAMIC_DRAW);
            this.allocated = true;
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void close() {
        if (this.buffer != 0) {
            GL15.glDeleteBuffers(this.buffer);
            this.buffer = 0;
        }
        this.allocated = false;
        MemoryUtil.memFree(this.data);
    }
}
