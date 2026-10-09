package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;

public final class WLTexture implements AutoCloseable {
    private final GpuTexture texture;
    private final GpuTextureView view;
    private final @Nullable WLFilter filter;
    private final boolean owned;

    public WLTexture(final GpuTexture texture, final GpuTextureView view, @Nullable final WLFilter filter, final boolean owned) {
        this.texture = texture;
        this.view = view;
        this.filter = filter;
        this.owned = owned;
    }

    public static WLTexture wrap(final GpuTextureView view) {
        return new WLTexture(view.texture(), view, null, false);
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

    public @Nullable WLFilter filter() {
        return this.filter;
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
        return other instanceof final WLTexture that && this.view == that.view && this.filter == that.filter;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(this.view) + (this.filter == null ? 0 : this.filter.ordinal() + 1);
    }
}
