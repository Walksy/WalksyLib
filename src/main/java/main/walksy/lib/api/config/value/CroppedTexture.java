package main.walksy.lib.api.config.value;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public record CroppedTexture(@Nullable Identifier texture, float u0, float v0, float u1, float v1) {
    public static final CroppedTexture NONE = new CroppedTexture(null, 0f, 0f, 1f, 1f);
    private static final float MIN_SPAN = 0.001f;

    public CroppedTexture {
        u0 = Mth.clamp(u0, 0f, 1f - MIN_SPAN);
        v0 = Mth.clamp(v0, 0f, 1f - MIN_SPAN);
        u1 = Mth.clamp(u1, u0 + MIN_SPAN, 1f);
        v1 = Mth.clamp(v1, v0 + MIN_SPAN, 1f);
    }

    public static CroppedTexture of(@Nullable final Identifier texture) {
        return new CroppedTexture(texture, 0f, 0f, 1f, 1f);
    }

    public static CroppedTexture centered(@Nullable final Identifier texture, final int imageWidth, final int imageHeight, final float aspect) {
        if (aspect <= 0f || imageWidth <= 0 || imageHeight <= 0) {
            return of(texture);
        }
        final float imageAspect = (float) imageWidth / imageHeight;
        if (imageAspect > aspect) {
            final float span = aspect / imageAspect;
            return new CroppedTexture(texture, (1f - span) / 2f, 0f, (1f + span) / 2f, 1f);
        }
        final float span = imageAspect / aspect;
        return new CroppedTexture(texture, 0f, (1f - span) / 2f, 1f, (1f + span) / 2f);
    }

    public boolean isEmpty() {
        return this.texture == null;
    }

    public CroppedTexture withCrop(final float u0, final float v0, final float u1, final float v1) {
        return new CroppedTexture(this.texture, u0, v0, u1, v1);
    }
}
