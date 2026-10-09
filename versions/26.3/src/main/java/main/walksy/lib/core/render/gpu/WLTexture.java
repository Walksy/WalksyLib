package main.walksy.lib.core.render.gpu;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

public final class WLTexture implements AutoCloseable {
    private final GpuTexture texture;
    private final GpuTextureView view;
    private final @Nullable GpuSampler sampler;
    private final boolean owned;

    public WLTexture(final GpuTexture texture, final GpuTextureView view, @Nullable final GpuSampler sampler, final boolean owned) {
        this.texture = texture;
        this.view = view;
        this.sampler = sampler;
        this.owned = owned;
    }

    public static WLTexture wrap(final GpuTextureView view, @Nullable final GpuSampler sampler) {
        return new WLTexture(view.texture(), view, sampler, false);
    }

    public int width() {
        return this.view.getWidth(0);
    }

    public int height() {
        return this.view.getHeight(0);
    }

    public GpuTexture gpuTexture() {
        return this.texture;
    }

    public GpuTextureView view() {
        return this.view;
    }

    public @Nullable GpuSampler sampler() {
        return this.sampler;
    }

    @Override
    public void close() {
        if (this.owned) {
            this.view.close();
            this.texture.close();
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final WLTexture that && this.view == that.view && this.sampler == that.sampler;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(this.view) + System.identityHashCode(this.sampler);
    }
}
