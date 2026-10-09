package main.walksy.lib.core.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;

final class IconRaster {
    private static final Map<Identifier, NativeImage> SOURCES = new HashMap<>();
    private static final Set<Identifier> FAILED = new HashSet<>();
    private static final Map<Key, Identifier> VARIANTS = new HashMap<>();
    private static final float MIN_POSE_SCALE = 0.5f;

    private record Key(Identifier source, int widthPx, int heightPx, int quarterTurns) {}

    private IconRaster() {
    }

    static boolean extract(final Icons.Icon icon, final WLGraphics2D graphics, final float x, final float y, final float size, final int color, final int quarterTurns) {
        return extract(icon, graphics, x, y, size, color, quarterTurns, false);
    }

    static boolean extractCentered(final Icons.Icon icon, final WLGraphics2D graphics, final float cx, final float cy, final float size, final int color, final int quarterTurns) {
        return extract(icon, graphics, cx, cy, size, color, quarterTurns, true);
    }

    private static boolean extract(final Icons.Icon icon, final WLGraphics2D graphics, final float x, final float y, final float size, final int color, final int quarterTurns, final boolean centred) {
        if (!DevicePixels.isAxisAligned(graphics.pose()) || Math.abs(graphics.pose().m00()) < MIN_POSE_SCALE || Math.abs(graphics.pose().m11()) < MIN_POSE_SCALE) {
            return false;
        }
        final NativeImage source = source(icon.id());
        if (source == null) {
            return false;
        }
        final Matrix3x2f pose = graphics.pose();
        final float poseX = DevicePixels.isAxisAligned(pose) && pose.m00() != 0f ? Math.abs(pose.m00()) : 1f;
        final float poseY = DevicePixels.isAxisAligned(pose) && pose.m11() != 0f ? Math.abs(pose.m11()) : 1f;
        final float sx = DevicePixels.perLogicalX() * poseX;
        final float sy = DevicePixels.perLogicalY() * poseY;
        final int widthPx = Math.max(1, Math.round(size * sx));
        final int heightPx = Math.max(1, Math.round(size * sy));
        final Identifier variant = VARIANTS.computeIfAbsent(new Key(icon.id(), widthPx, heightPx, quarterTurns), key -> build(source, key));
        if (variant == null) {
            return false;
        }
        final float drawW = widthPx / sx;
        final float drawH = heightPx / sy;
        final float x0 = DevicePixels.snapX(centred ? x - drawW / 2f : x, pose);
        final float y0 = DevicePixels.snapY(centred ? y - drawH / 2f : y, pose);
        graphics.blitUv(variant, x0, y0, drawW, drawH, 0f, 0f, 1f, 1f, color);
        return true;
    }

    private static NativeImage source(final Identifier id) {
        if (FAILED.contains(id)) {
            return null;
        }
        final NativeImage cached = SOURCES.get(id);
        if (cached != null) {
            return cached;
        }
        try (final InputStream stream = Minecraft.getInstance().getResourceManager().open(id)) {
            final NativeImage image = NativeImage.read(stream);
            SOURCES.put(id, image);
            return image;
        } catch (final IOException | RuntimeException e) {
            FAILED.add(id);
            WalksyLib.getLogger().err("Could not read icon " + id + ": " + e.getMessage());
            return null;
        }
    }

    private static Identifier build(final NativeImage source, final Key key) {
        final boolean swapped = key.quarterTurns() == 1 || key.quarterTurns() == 3;
        final int scaledW = swapped ? key.heightPx() : key.widthPx();
        final int scaledH = swapped ? key.widthPx() : key.heightPx();
        final NativeImage scaled = resample(source, scaledW, scaledH);
        final NativeImage finalImage = rotate(scaled, key.quarterTurns());
        if (finalImage != scaled) {
            scaled.close();
        }
        final Identifier id = Identifier.fromNamespaceAndPath("walksylib", "icon_raster/" + key.source().getPath().replace('/', '_').replace(".png", "") + "_" + key.widthPx() + "x" + key.heightPx() + "_r" + key.quarterTurns());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(finalImage::toString, finalImage));
        return id;
    }

    private static NativeImage resample(final NativeImage source, final int targetW, final int targetH) {
        final int srcW = source.getWidth();
        final int srcH = source.getHeight();
        final NativeImage out = new NativeImage(NativeImage.Format.RGBA, targetW, targetH, true);
        if (targetW >= srcW || targetH >= srcH) {
            bilinear(source, out);
            return out;
        }
        final float scaleX = srcW / (float) targetW;
        final float scaleY = srcH / (float) targetH;
        for (int dy = 0; dy < targetH; dy++) {
            final float y0 = dy * scaleY;
            final float y1 = y0 + scaleY;
            final int sy0 = (int) Math.floor(y0);
            final int sy1 = Math.min(srcH, (int) Math.ceil(y1));
            for (int dx = 0; dx < targetW; dx++) {
                final float x0 = dx * scaleX;
                final float x1 = x0 + scaleX;
                final int sx0 = (int) Math.floor(x0);
                final int sx1 = Math.min(srcW, (int) Math.ceil(x1));
                double weightSum = 0;
                double alphaSum = 0;
                double rSum = 0;
                double gSum = 0;
                double bSum = 0;
                for (int sy = sy0; sy < sy1; sy++) {
                    final double wy = Math.min(y1, sy + 1) - Math.max(y0, sy);
                    if (wy <= 0) {
                        continue;
                    }
                    for (int sx = sx0; sx < sx1; sx++) {
                        final double wx = Math.min(x1, sx + 1) - Math.max(x0, sx);
                        if (wx <= 0) {
                            continue;
                        }
                        final double w = wx * wy;
                        final int argb = source.getPixel(sx, sy);
                        final int a = (argb >>> 24) & 0xFF;
                        weightSum += w;
                        alphaSum += w * a;
                        final double wa = w * a;
                        rSum += wa * ((argb >> 16) & 0xFF);
                        gSum += wa * ((argb >> 8) & 0xFF);
                        bSum += wa * (argb & 0xFF);
                    }
                }
                out.setPixel(dx, dy, pack(weightSum, alphaSum, rSum, gSum, bSum));
            }
        }
        return out;
    }

    private static void bilinear(final NativeImage source, final NativeImage out) {
        final int srcW = source.getWidth();
        final int srcH = source.getHeight();
        final int targetW = out.getWidth();
        final int targetH = out.getHeight();
        for (int dy = 0; dy < targetH; dy++) {
            final float fy = ((dy + 0.5f) * srcH / targetH) - 0.5f;
            final int y0 = (int) Math.floor(fy);
            final float ty = fy - y0;
            for (int dx = 0; dx < targetW; dx++) {
                final float fx = ((dx + 0.5f) * srcW / targetW) - 0.5f;
                final int x0 = (int) Math.floor(fx);
                final float tx = fx - x0;
                double weightSum = 0;
                double alphaSum = 0;
                double rSum = 0;
                double gSum = 0;
                double bSum = 0;
                for (int oy = 0; oy <= 1; oy++) {
                    final int sy = clamp(y0 + oy, srcH);
                    final double wy = oy == 0 ? 1 - ty : ty;
                    for (int ox = 0; ox <= 1; ox++) {
                        final int sx = clamp(x0 + ox, srcW);
                        final double w = wy * (ox == 0 ? 1 - tx : tx);
                        if (w <= 0) {
                            continue;
                        }
                        final int argb = source.getPixel(sx, sy);
                        final int a = (argb >>> 24) & 0xFF;
                        weightSum += w;
                        alphaSum += w * a;
                        final double wa = w * a;
                        rSum += wa * ((argb >> 16) & 0xFF);
                        gSum += wa * ((argb >> 8) & 0xFF);
                        bSum += wa * (argb & 0xFF);
                    }
                }
                out.setPixel(dx, dy, pack(weightSum, alphaSum, rSum, gSum, bSum));
            }
        }
    }

    private static int pack(final double weightSum, final double alphaSum, final double rSum, final double gSum, final double bSum) {
        if (weightSum <= 0) {
            return 0;
        }
        final int a = (int) Math.round(alphaSum / weightSum);
        int r;
        int g;
        int b;
        if (alphaSum <= 0) {
            r = g = b = 0xFF;
        } else {
            r = (int) Math.round(rSum / alphaSum);
            g = (int) Math.round(gSum / alphaSum);
            b = (int) Math.round(bSum / alphaSum);
        }
        return (clampByte(a) << 24) | (clampByte(r) << 16) | (clampByte(g) << 8) | clampByte(b);
    }

    private static NativeImage rotate(final NativeImage image, final int quarterTurns) {
        if (quarterTurns == 0) {
            return image;
        }
        final int w = image.getWidth();
        final int h = image.getHeight();
        final boolean swapped = quarterTurns == 1 || quarterTurns == 3;
        final NativeImage out = new NativeImage(NativeImage.Format.RGBA, swapped ? h : w, swapped ? w : h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int argb = image.getPixel(x, y);
                switch (quarterTurns) {
                    case 1 -> out.setPixel(h - 1 - y, x, argb);
                    case 2 -> out.setPixel(w - 1 - x, h - 1 - y, argb);
                    default -> out.setPixel(y, w - 1 - x, argb);
                }
            }
        }
        return out;
    }

    private static int clamp(final int v, final int size) {
        return Math.max(0, Math.min(size - 1, v));
    }

    private static int clampByte(final int v) {
        return Math.max(0, Math.min(255, v));
    }
}
