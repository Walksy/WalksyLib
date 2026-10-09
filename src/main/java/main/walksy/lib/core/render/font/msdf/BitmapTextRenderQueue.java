package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.OverlayBounds;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLMapping;
import main.walksy.lib.core.render.gpu.WLPass;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.gpu.WLTransform;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import main.walksy.lib.core.render.gpu.WLVertexBuffer;
import main.walksy.lib.core.render.perf.UiProfiler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class BitmapTextRenderQueue {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final List<Item> QUEUE = new ArrayList<>();
    private static final List<Item> EARLY = new ArrayList<>();
    private static final List<Item> CONTENT = new ArrayList<>();
    private static final List<Item> POPOVER = new ArrayList<>();
    private static final List<Item> DEV = new ArrayList<>();
    private static final List<Item> TOP = new ArrayList<>();
    private static final WLVertexBuffer TAIL_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts");
    private static final WLVertexBuffer EARLY_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts (early)");
    private static final WLVertexBuffer CONTENT_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts (content)");
    private static final WLVertexBuffer POPOVER_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts (popover)");
    private static final WLVertexBuffer DEV_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts (dev)");
    private static final WLVertexBuffer TOP_VERTS = WLRenderSystem.vertexBuffer("walksylib bitmap text verts (top)");
    private static final Supplier<String> PASS_LABEL = () -> "walksylib bitmap text";

    private record Item(BitmapFontAtlas atlas, float layoutScale, String text, Matrix3x2f pose, float x, float y, int color, @Nullable WLRect scissor, @Nullable ContentStretch stretch, boolean snap) {}

    private BitmapTextRenderQueue() {
    }

    static void enqueue(final BitmapFontAtlas atlas, final float layoutScale, final String text, final Matrix3x2f pose, final float x, final float y, final int color, @Nullable final WLRect scissor) {
        final boolean snap = atlas.pixelArt() ? DevicePixels.isAxisAligned(pose) : !WLRenderSystem.inMotion() && DevicePixels.isUnscaled(pose);
        QUEUE.add(new Item(atlas, layoutScale, text, pose, x, y, color, scissor, ContentStretch.current(), snap));
    }

    public static void markEarlySince(final int mark) {
        if (mark >= QUEUE.size()) {
            return;
        }
        final List<Item> tail = QUEUE.subList(Math.max(0, mark), QUEUE.size());
        EARLY.addAll(tail);
        tail.clear();
    }

    public static int queuedCount() {
        return QUEUE.size();
    }

    public static void markContentSince(final int mark) {
        if (mark >= QUEUE.size()) {
            return;
        }
        final List<Item> tail = QUEUE.subList(Math.max(0, mark), QUEUE.size());
        CONTENT.addAll(tail);
        tail.clear();
    }

    public static void markPopoverContentSince(final int mark) {
        if (mark >= QUEUE.size()) {
            return;
        }
        final List<Item> tail = QUEUE.subList(Math.max(0, mark), QUEUE.size());
        POPOVER.addAll(tail);
        tail.clear();
    }

    public static void markDevSince(final int mark) {
        if (mark >= QUEUE.size()) {
            return;
        }
        final List<Item> tail = QUEUE.subList(Math.max(0, mark), QUEUE.size());
        DEV.addAll(tail);
        tail.clear();
    }

    public static void markTopSince(final int mark) {
        if (mark >= QUEUE.size()) {
            return;
        }
        final List<Item> tail = QUEUE.subList(Math.max(0, mark), QUEUE.size());
        TOP.addAll(tail);
        tail.clear();
    }

    public static boolean hasEarly() {
        return !EARLY.isEmpty();
    }

    public static boolean hasContent() {
        return !CONTENT.isEmpty();
    }

    public static boolean hasPopoverContent() {
        return !POPOVER.isEmpty();
    }

    public static void flushEarly(final WLTexture target) {
        render(EARLY, new WLTexture[] {target}, true, EARLY_VERTS, WLRenderSystem.earlyTextBounds());
    }

    public static void flushContent(final WLTexture overlay) {
        render(CONTENT, new WLTexture[] {overlay}, true, CONTENT_VERTS, WLRenderSystem.contentBounds());
    }

    public static void flushPopoverContent(final WLTexture overlay) {
        render(POPOVER, new WLTexture[] {overlay}, true, POPOVER_VERTS, WLRenderSystem.popoverContentBounds());
    }

    public static void flush() {
        if (!EARLY.isEmpty()) {
            QUEUE.addAll(0, EARLY);
            EARLY.clear();
        }
        if (!CONTENT.isEmpty()) {
            QUEUE.addAll(0, CONTENT);
            CONTENT.clear();
        }
        if (!POPOVER.isEmpty()) {
            QUEUE.addAll(0, POPOVER);
            POPOVER.clear();
        }
        render(QUEUE, new WLTexture[] {WLRenderSystem.mainTarget()}, true, TAIL_VERTS, null);
    }

    public static void flushDev() {
        render(DEV, new WLTexture[] {WLRenderSystem.mainTarget()}, true, DEV_VERTS, null);
    }

    public static void flushTop() {
        render(TOP, new WLTexture[] {WLRenderSystem.mainTarget()}, true, TOP_VERTS, null);
    }

    private static void render(final List<Item> queue, final WLTexture[] targets, final boolean ownProjection, final WLVertexBuffer vertexSlot, @Nullable final OverlayBounds bounds) {
        if (queue.isEmpty()) {
            return;
        }
        record GroupKey(BitmapFontAtlas atlas, @Nullable WLRect scissor, boolean stretched, boolean smooth) {}
        final Map<GroupKey, List<Item>> byAtlas = new LinkedHashMap<>();
        GroupKey key = null;
        List<Item> bucket = null;
        for (final Item item : queue) {
            final boolean stretched = item.stretch() != null;
            final boolean smooth = item.atlas().pixelArt() ? Math.abs(item.layoutScale() - Math.round(item.layoutScale())) > 1e-4f : !item.snap();
            if (bucket == null || key.atlas() != item.atlas() || key.stretched() != stretched || key.smooth() != smooth || !Objects.equals(key.scissor(), item.scissor())) {
                key = new GroupKey(item.atlas(), item.scissor(), stretched, smooth);
                bucket = byAtlas.computeIfAbsent(key, k -> new ArrayList<>());
            }
            bucket.add(item);
        }
        final Matrix4f modelView = MODEL_VIEW.setTranslation(0f, 0f, -11000f);
        final WLTransform transform = WLRenderSystem.transform(modelView);
        record Group(BitmapFontAtlas atlas, @Nullable WLRect scissor, boolean stretched, boolean smooth, List<Item> items, int totalGlyphs, int byteOffset, int byteLength) {}
        final List<Group> groups = new ArrayList<>();
        int totalBytes = 0;
        for (final var entry : byAtlas.entrySet()) {
            final BitmapFontAtlas atlas = entry.getKey().atlas();
            final List<Item> items = entry.getValue();
            int totalGlyphs = 0;
            for (final Item it : items) {
                totalGlyphs += atlas.countRenderable(it.text());
            }
            if (totalGlyphs == 0) {
                continue;
            }
            final int byteLength = totalGlyphs * 4 * MsdfRenderQueue.VERTEX_STRIDE;
            groups.add(new Group(atlas, entry.getKey().scissor(), entry.getKey().stretched(), entry.getKey().smooth(), items, totalGlyphs, totalBytes, byteLength));
            totalBytes += byteLength;
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.count(UiProfiler.Counter.GLYPHS, totalGlyphs);
            }
        }
        if (groups.isEmpty()) {
            queue.clear();
            return;
        }
        try (final WLMapping map = vertexSlot.map(totalBytes)) {
            final ByteBuffer buf = map.data();
            buf.order(ByteOrder.nativeOrder());
            for (final Group group : groups) {
                buf.position(group.byteOffset());
                for (final Item item : group.items()) {
                    group.atlas().buildVerticesRaw(buf, item.pose(), item.text(), item.x(), item.y(), item.color(), bounds, item.layoutScale(), item.stretch(), item.snap());
                }
            }
        }
        final int targetHeightPx = WLRenderSystem.targetHeight();
        final WLUniformBuffer projection = ownProjection ? MsdfRenderQueue.guiProjection() : null;
        for (final WLTexture target : targets) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
            }
            try (final WLPass pass = WLRenderSystem.pass(PASS_LABEL, target)) {
                for (final Group group : groups) {
                    int[] rect = null;
                    if (group.scissor() != null) {
                        rect = DevicePixels.scissorToRenderPass(group.scissor(), targetHeightPx);
                        if (rect == null) {
                            continue;
                        }
                    }
                    pass.pipeline(group.atlas().pixelArt() ? BitmapTextPipeline.pixel() : group.stretched() ? BitmapTextPipeline.stretched() : BitmapTextPipeline.get());
                    if (ownProjection) {
                        pass.uniform("Projection", projection);
                    } else {
                        pass.defaultUniforms();
                    }
                    pass.transforms(transform);
                    pass.texture("Sampler0", WLRenderSystem.texture(group.atlas().texture()), group.smooth() || !group.atlas().pixelArt() && group.stretched() ? WLFilter.LINEAR : WLFilter.NEAREST);
                    if (rect != null) {
                        pass.scissor(rect[0], rect[1], rect[2], rect[3]);
                    } else {
                        pass.noScissor();
                    }
                    pass.drawQuads(vertexSlot, group.byteOffset(), group.byteLength(), group.totalGlyphs() * 4);
                }
            }
        }
        queue.clear();
    }
}
