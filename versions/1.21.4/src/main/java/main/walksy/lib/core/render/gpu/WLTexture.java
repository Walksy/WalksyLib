package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class WLTexture implements AutoCloseable {
    private final int id;
    private final @Nullable RenderTarget target;
    private final @Nullable WLFilter filter;
    private final boolean owned;
    private int width = -1;
    private int height = -1;

    public WLTexture(final int id, @Nullable final RenderTarget target, @Nullable final WLFilter filter, final boolean owned) {
        this.id = id;
        this.target = target;
        this.filter = filter;
        this.owned = owned;
    }

    public static WLTexture wrap(final int id) {
        return new WLTexture(id, null, null, false);
    }

    public int width() {
        if (this.target != null) {
            return this.target.width;
        }
        this.querySize();
        return this.width;
    }

    public int height() {
        if (this.target != null) {
            return this.target.height;
        }
        this.querySize();
        return this.height;
    }

    public int glId() {
        return this.id;
    }

    @Nullable RenderTarget target() {
        return this.target;
    }

    public @Nullable WLFilter filter() {
        return this.filter;
    }

    private void querySize() {
        if (this.width >= 0) {
            return;
        }
        if (VulkanBackend.active()) {
            this.width = VulkanBackend.textureWidth(this.id);
            this.height = VulkanBackend.textureHeight(this.id);
            return;
        }
        RenderSystem.bindTexture(this.id);
        this.width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        this.height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
    }

    @Override
    public void close() {
        if (this.owned && this.target != null) {
            this.target.destroyBuffers();
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final WLTexture that && this.id == that.id && this.filter == that.filter;
    }

    @Override
    public int hashCode() {
        return 31 * this.id + (this.filter == null ? 0 : this.filter.ordinal() + 1);
    }
}
