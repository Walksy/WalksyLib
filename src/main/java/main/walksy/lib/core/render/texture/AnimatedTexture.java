package main.walksy.lib.core.render.texture;


import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.serialization.SpriteCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimatedTexture {
    private static final int MIN_FRAME_DELAY_MS = 20;
    private static final int DEFAULT_FRAME_DELAY_MS = 100;
    private final Identifier[] frames;
    private final int[] cumulativeMs;
    private final int totalMs;
    private static final String DISPOSAL_RESTORE_TO_BACKGROUND = "restoreToBackgroundColor";
    private static final String DISPOSAL_RESTORE_TO_PREVIOUS = "restoreToPrevious";

    private AnimatedTexture(final Identifier[] frames, final int[] delaysMs) {
        this.frames = frames;
        this.cumulativeMs = new int[frames.length];
        int total = 0;
        for (int i = 0; i < frames.length; i++) {
            total += Math.max(MIN_FRAME_DELAY_MS, delaysMs[i]);
            this.cumulativeMs[i] = total;
        }
        this.totalMs = Math.max(1, total);
    }

    public boolean isAnimated() {
        return this.frames.length > 1;
    }

    public int frameCount() {
        return this.frames.length;
    }

    public Identifier currentFrame() {
        if (this.frames.length == 1) {
            return this.frames[0];
        }
        final int t = (int) (System.currentTimeMillis() % this.totalMs);
        for (int i = 0; i < this.cumulativeMs.length; i++) {
            if (t < this.cumulativeMs[i]) {
                return this.frames[i];
            }
        }
        return this.frames[this.frames.length - 1];
    }

    public static AnimatedTexture decode(final byte[] bytes, final Identifier baseId) {
        final List<NativeImage> images = new ArrayList<>();
        final List<Integer> delays = new ArrayList<>();
        if (!decodeFrames(bytes, images, delays)) {
            return null;
        }
        return registerFrames(images, delays, baseId);
    }

    public static void decodeAsync(final byte[] bytes, final Identifier baseId, final Runnable onComplete) {
        final Thread thread = new Thread(() -> {
            final List<NativeImage> images = new ArrayList<>();
            final List<Integer> delays = new ArrayList<>();
            final boolean decoded = decodeFrames(bytes, images, delays);
            Minecraft.getInstance().execute(() -> {
                if (decoded) {
                    registerFrames(images, delays, baseId);
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }, "walksylib-texture-decode");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean decodeFrames(final byte[] bytes, final List<NativeImage> outImages, final List<Integer> outDelays) {
        if (tryDecodeGifFrames(bytes, outImages, outDelays)) {
            return true;
        }
        final NativeImage single = SpriteCache.readImage(bytes);
        if (single == null) {
            return false;
        }
        outImages.add(single);
        outDelays.add(DEFAULT_FRAME_DELAY_MS);
        return true;
    }

    private static AnimatedTexture registerFrames(final List<NativeImage> images, final List<Integer> delays, final Identifier baseId) {
        final Identifier[] ids = new Identifier[images.size()];
        final int[] delayArr = new int[images.size()];
        for (int i = 0; i < images.size(); i++) {
            final Identifier frameId = images.size() == 1 ? baseId : Identifier.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_frame" + i);
            ids[i] = frameId;
            delayArr[i] = delays.get(i);
            final DynamicTexture texture = new DynamicTexture(frameId::toString, images.get(i));
            Minecraft.getInstance().getTextureManager().register(frameId, texture);
        }
        final AnimatedTexture animated = new AnimatedTexture(ids, delayArr);
        AnimatedTextureRegistry.register(baseId, animated);
        return animated;
    }

    private static boolean tryDecodeGifFrames(final byte[] bytes, final List<NativeImage> outImages, final List<Integer> outDelays) {
        try (final ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return false;
            }
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return false;
            }
            final ImageReader reader = readers.next();
            try {
                if (!"gif".equalsIgnoreCase(reader.getFormatName())) {
                    return false;
                }
                reader.setInput(iis);
                final int count = reader.getNumImages(true);
                if (count <= 1) {
                    return false;
                }
                final int[] canvasSize = readLogicalScreenSize(reader);
                final int canvasW = canvasSize[0];
                final int canvasH = canvasSize[1];
                final NativeImage canvas = new NativeImage(NativeImage.Format.RGBA, canvasW, canvasH, false);
                canvas.fillRect(0, 0, canvasW, canvasH, 0);
                String prevDisposal = "none";
                Rectangle prevRect = null;
                NativeImage pendingRestore = null;
                try {
                    for (int i = 0; i < count; i++) {
                        if (i > 0) {
                            switch (prevDisposal) {
                                case DISPOSAL_RESTORE_TO_BACKGROUND -> {
                                    if (prevRect != null) {
                                        canvas.fillRect(prevRect.x, prevRect.y, prevRect.width, prevRect.height, 0);
                                    }
                                }
                                case DISPOSAL_RESTORE_TO_PREVIOUS -> {
                                    if (pendingRestore != null) {
                                        canvas.copyFrom(pendingRestore);
                                    }
                                }
                                default -> {
                                }
                            }
                        }
                        if (pendingRestore != null) {
                            pendingRestore.close();
                            pendingRestore = null;
                        }
                        final IIOMetadata metadata = reader.getImageMetadata(i);
                        final Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                        final Rectangle rect = readImageRect(root, canvasW, canvasH);
                        final String disposal = readDisposalMethod(root);
                        if (DISPOSAL_RESTORE_TO_PREVIOUS.equals(disposal)) {
                            pendingRestore = new NativeImage(NativeImage.Format.RGBA, canvasW, canvasH, false);
                            pendingRestore.copyFrom(canvas);
                        }
                        compositeFrame(canvas, reader.read(i), rect, canvasW, canvasH);
                        final NativeImage out = new NativeImage(NativeImage.Format.RGBA, canvasW, canvasH, false);
                        out.copyFrom(canvas);
                        outImages.add(out);
                        outDelays.add(readFrameDelayMs(root));
                        prevDisposal = disposal;
                        prevRect = rect;
                    }
                    return true;
                } finally {
                    canvas.close();
                    if (pendingRestore != null) {
                        pendingRestore.close();
                    }
                }
            } finally {
                reader.dispose();
            }
        } catch (final IOException e) {
            return false;
        }
    }

    private static int[] readLogicalScreenSize(final ImageReader reader) throws IOException {
        try {
            final IIOMetadata streamMetadata = reader.getStreamMetadata();
            if (streamMetadata != null) {
                final Node root = streamMetadata.getAsTree(streamMetadata.getNativeMetadataFormatName());
                for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if ("LogicalScreenDescriptor".equals(node.getNodeName())) {
                        final NamedNodeMap attrs = node.getAttributes();
                        final int w = Integer.parseInt(attrs.getNamedItem("logicalScreenWidth").getNodeValue());
                        final int h = Integer.parseInt(attrs.getNamedItem("logicalScreenHeight").getNodeValue());
                        if (w > 0 && h > 0) {
                            return new int[]{w, h};
                        }
                    }
                }
            }
        } catch (final IOException | NumberFormatException | NullPointerException ignored) {
        }
        return new int[]{reader.getWidth(0), reader.getHeight(0)};
    }

    private static Rectangle readImageRect(final Node imageMetadataRoot, final int fallbackW, final int fallbackH) {
        for (Node node = imageMetadataRoot.getFirstChild(); node != null; node = node.getNextSibling()) {
            if ("ImageDescriptor".equals(node.getNodeName())) {
                final NamedNodeMap attrs = node.getAttributes();
                try {
                    final int x = Integer.parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue());
                    final int y = Integer.parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue());
                    final int w = Integer.parseInt(attrs.getNamedItem("imageWidth").getNodeValue());
                    final int h = Integer.parseInt(attrs.getNamedItem("imageHeight").getNodeValue());
                    return new Rectangle(x, y, w, h);
                } catch (final NumberFormatException | NullPointerException ignored) {
                    break;
                }
            }
        }
        return new Rectangle(0, 0, fallbackW, fallbackH);
    }

    private static String readDisposalMethod(final Node imageMetadataRoot) {
        for (Node node = imageMetadataRoot.getFirstChild(); node != null; node = node.getNextSibling()) {
            if ("GraphicControlExtension".equals(node.getNodeName())) {
                final Node attr = node.getAttributes().getNamedItem("disposalMethod");
                if (attr != null) {
                    return attr.getNodeValue();
                }
            }
        }
        return "none";
    }

    private static void compositeFrame(final NativeImage canvas, final BufferedImage frame, final Rectangle rect, final int canvasW, final int canvasH) {
        final int fw = frame.getWidth();
        final int fh = frame.getHeight();
        final int[] argb = frame.getRGB(0, 0, fw, fh, null, 0, fw);
        for (int y = 0; y < fh; y++) {
            final int destY = rect.y + y;
            if (destY < 0 || destY >= canvasH) {
                continue;
            }
            final int rowOffset = y * fw;
            for (int x = 0; x < fw; x++) {
                final int argbPixel = argb[rowOffset + x];
                if ((argbPixel >>> 24) == 0) {
                    continue;
                }
                final int destX = rect.x + x;
                if (destX < 0 || destX >= canvasW) {
                    continue;
                }
                final int abgr = (argbPixel & 0xFF00FF00) | ((argbPixel & 0x00FF0000) >>> 16) | ((argbPixel & 0x000000FF) << 16);
                canvas.setPixelABGR(destX, destY, abgr);
            }
        }
    }

    private static int readFrameDelayMs(final Node imageMetadataRoot) {
        for (Node node = imageMetadataRoot.getFirstChild(); node != null; node = node.getNextSibling()) {
            if ("GraphicControlExtension".equals(node.getNodeName())) {
                final Node delayNode = node.getAttributes().getNamedItem("delayTime");
                if (delayNode != null) {
                    try {
                        return Integer.parseInt(delayNode.getNodeValue()) * 10;
                    } catch (final NumberFormatException e) {
                        WalksyLib.getLogger().err("Failed to read GIF frame delay: " + e.getMessage());
                    }
                }
            }
        }
        return DEFAULT_FRAME_DELAY_MS;
    }
}
