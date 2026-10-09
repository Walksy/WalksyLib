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
public final class MsdfRenderQueue {
    static final int VERTEX_STRIDE = 12 + 8 + 4;
    private static final List<Item> QUEUE = new ArrayList<>();
    private static final List<Item> EARLY = new ArrayList<>();
    private static final List<Item> CONTENT = new ArrayList<>();
    private static final List<Item> POPOVER = new ArrayList<>();
    private static final List<Item> DEV = new ArrayList<>();
    private static final List<Item> TOP = new ArrayList<>();
    private static final WLVertexBuffer TAIL_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts");
    private static final WLVertexBuffer EARLY_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts (early)");
    private static final WLVertexBuffer CONTENT_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts (content)");
    private static final WLVertexBuffer POPOVER_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts (popover)");
    private static final WLVertexBuffer DEV_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts (dev)");
    private static final WLVertexBuffer TOP_VERTS = WLRenderSystem.vertexBuffer("walksylib msdf verts (top)");
    private static final Supplier<String> PASS_LABEL = () -> "walksylib msdf text";
    private static WLUniformBuffer projectionUbo;
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    record Item(MsdfFont font, String text, Matrix3x2f pose, float x, float y, float size, int color, @Nullable WLRect scissor, @Nullable ContentStretch stretch) {}

    private MsdfRenderQueue() {
    }

    static void enqueue(final MsdfFont font, final String text, final Matrix3x2f pose, final float x, final float y, final float size, final int color, @Nullable final WLRect scissor) {
        QUEUE.add(new Item(font, text, pose, x, y, size, color, scissor, ContentStretch.current()));
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

    static WLUniformBuffer guiProjection() {
        if (projectionUbo == null) {
            projectionUbo = WLRenderSystem.uniformBuffer("walksylib text projection", 64);
        }
        final Matrix4f proj = PROJECTION.setOrtho(0f, DevicePixels.logicalWidth(), DevicePixels.logicalHeight(), 0f, 1000f, 21000f);
        try (final WLMapping map = projectionUbo.map()) {
            map.writer().putMat4f(proj);
        }
        return projectionUbo;
    }

    private static void render(final List<Item> queue, final WLTexture[] targets, final boolean ownProjection, final WLVertexBuffer vertexSlot, @Nullable final OverlayBounds bounds) {
        if (queue.isEmpty()) {
            return;
        }
        record GroupKey(MsdfFont font, @Nullable WLRect scissor) {}
        final Map<GroupKey, List<Item>> byFont = new LinkedHashMap<>();
        GroupKey key = null;
        List<Item> bucket = null;
        for (final Item item : queue) {
            if (bucket == null || key.font() != item.font() || !Objects.equals(key.scissor(), item.scissor())) {
                key = new GroupKey(item.font(), item.scissor());
                bucket = byFont.computeIfAbsent(key, k -> new ArrayList<>());
            }
            bucket.add(item);
        }
        final Matrix4f modelView = MODEL_VIEW.setTranslation(0f, 0f, -11000f);
        final WLTransform transform = WLRenderSystem.transform(modelView);
        record Group(MsdfFont font, @Nullable WLRect scissor, List<Item> items, int totalGlyphs, int byteOffset, int byteLength) {}
        final List<Group> groups = new ArrayList<>();
        int totalBytes = 0;
        for (final var entry : byFont.entrySet()) {
            final MsdfFont font = entry.getKey().font();
            final List<Item> items = entry.getValue();
            int totalGlyphs = 0;
            for (final Item it : items) {
                totalGlyphs += font.countRenderable(it.text());
            }
            if (totalGlyphs == 0) {
                continue;
            }
            final int byteLength = totalGlyphs * 4 * VERTEX_STRIDE;
            groups.add(new Group(font, entry.getKey().scissor(), items, totalGlyphs, totalBytes, byteLength));
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
                    group.font().buildVerticesRaw(buf, item.pose(), item.text(), item.size(), item.x(), item.y(), item.color(), bounds, item.stretch());
                }
            }
        }
        final int targetHeightPx = WLRenderSystem.targetHeight();
        final WLUniformBuffer projection = ownProjection ? guiProjection() : null;
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
                    pass.pipeline(MsdfPipeline.get());
                    if (ownProjection) {
                        pass.uniform("Projection", projection);
                    } else {
                        pass.defaultUniforms();
                    }
                    pass.transforms(transform);
                    pass.uniform("MsdfParams", group.font().getParamsUbo());
                    pass.texture("Sampler0", WLRenderSystem.texture(group.font().getTexture()), WLFilter.LINEAR);
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
