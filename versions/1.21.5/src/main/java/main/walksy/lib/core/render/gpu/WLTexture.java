package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.textures.GpuTexture;
import org.jetbrains.annotations.Nullable;

public final class WLTexture implements AutoCloseable {
    private final GpuTexture texture;
    private final @Nullable WLFilter filter;
    private final boolean owned;

    public WLTexture(final GpuTexture texture, @Nullable final WLFilter filter, final boolean owned) {
        this.texture = texture;
        this.filter = filter;
        this.owned = owned;
    }

    public static WLTexture wrap(final GpuTexture texture) {
        return new WLTexture(texture, null, false);
    }

    public int width() {
        return this.texture.getWidth(0);
    }

    public int height() {
        return this.texture.getHeight(0);
    }

    public GpuTexture gpuTexture() {
        return this.texture;
    }

    public @Nullable WLFilter filter() {
        return this.filter;
    }

    @Override
    public void close() {
        if (this.owned) {
            this.texture.close();
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final WLTexture that && this.texture == that.texture && this.filter == that.filter;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(this.texture) + (this.filter == null ? 0 : this.filter.ordinal() + 1);
    }
}
