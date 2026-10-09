package main.walksy.lib.core.render;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.element.ByteVertexSink;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.gpu.WLBlend;
import main.walksy.lib.core.render.gpu.WLMapping;
import main.walksy.lib.core.render.gpu.WLPass;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.gpu.WLTransform;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import main.walksy.lib.core.render.gpu.WLVertexBuffer;
import main.walksy.lib.core.render.gpu.WLVertexFormat;
import main.walksy.lib.core.render.perf.UiProfiler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class ContentStratum {
    private static final List<String> MATRICES = List.of("Projection", "DynamicTransforms");
    private static final List<String> SAMPLER = List.of("Sampler0");
    private static final WLPipelineSpec COLOR = new WLPipelineSpec("content_color", "core/content_color", "program/content_color", WLVertexFormat.POSITION_COLOR, WLBlend.TRANSLUCENT, MATRICES, List.of());
    private static final WLPipelineSpec TEXTURED = new WLPipelineSpec("content_tex", "core/msdf_text", "program/content_tex", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, MATRICES, SAMPLER);
    private static final WLPipelineSpec PREMULTIPLIED = new WLPipelineSpec("content_tex_premultiplied", "core/msdf_text", "program/content_tex", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.PREMULTIPLIED, MATRICES, SAMPLER);
    private static final WLPipelineSpec GRAYSCALE = new WLPipelineSpec("content_tex_grayscale", "core/msdf_text", "program/content_tex_grayscale", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, MATRICES, SAMPLER);
    private static final WLPipelineSpec ROUNDED_TEXTURED = new WLPipelineSpec("content_tex_rounded", "core/msdf_text", "program/rounded_tex", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, MATRICES, SAMPLER);
    private static final WLPipelineSpec CORNER = new WLPipelineSpec("content_corner", "core/rounded_corner", "program/rounded_corner", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, MATRICES, List.of());
    private static final WLPipelineSpec CHECKER = new WLPipelineSpec("content_checker", "core/rounded_corner", "program/rounded_checker", WLVertexFormat.POSITION_TEX_COLOR, WLBlend.TRANSLUCENT, MATRICES, List.of());
    private static final Supplier<String> PASS_LABEL = () -> "walksylib content stratum";
    private WLUniformBuffer projectionUbo;
    private final Channel row = new Channel();
    private final Channel popover = new Channel();
    private final Channel dev = new Channel();
    private final Channel top = new Channel();
    private final Channel direct = new Channel();
    private static final int DEFAULT_POPOVER_BOUNDARY = 30;
    private int popoverContentBoundary = DEFAULT_POPOVER_BOUNDARY;

    private final class Channel {
        private final List<WLElement> queue = new ArrayList<>();
        private final ByteVertexSink sink = new ByteVertexSink();
        private boolean active;
        private WLVertexBuffer vertices;

        boolean isActive() {
            return this.active;
        }

        void begin() {
            this.active = true;
        }

        void end() {
            this.active = false;
        }

        void add(final WLElement element) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.count(UiProfiler.Counter.STRATUM_SHAPES, 1L);
            }
            this.queue.add(element);
        }

        void discardStale() {
            this.queue.clear();
            this.active = false;
        }

        boolean hasQueued() {
            return !this.queue.isEmpty();
        }

        void flush(final WLTexture[] targets, @Nullable final OverlayBounds bounds) {
            if (this.queue.isEmpty()) {
                return;
            }
            try {
                this.render(targets, bounds);
            } finally {
                this.queue.clear();
            }
        }

        private void render(final WLTexture[] targets, @Nullable final OverlayBounds bounds) {
            record Group(WLElementKind kind, @Nullable WLTexture texture, @Nullable WLRect scissor, List<WLElement> items) {}
            final List<Group> groups = new ArrayList<>();
            Group current = null;
            for (final WLElement item : this.queue) {
                final WLElementKind kind = batchKind(item);
                final WLRect scissor = item.scissorArea();
                if (current == null || current.kind() != kind || !Objects.equals(current.texture(), item.texture()) || !Objects.equals(current.scissor(), scissor)) {
                    current = new Group(kind, item.texture(), scissor, new ArrayList<>());
                    groups.add(current);
                }
                current.items().add(item);
            }
            record Built(Group group, int vertexCount, int byteOffset, int byteLength) {}
            final List<Built> built = new ArrayList<>();
            final ByteVertexSink sink = this.sink;
            sink.reset();
            for (final Group group : groups) {
                sink.format(group.kind().format());
                final int start = sink.position();
                for (final WLElement item : group.items()) {
                    item.emit(sink);
                    if (bounds != null) {
                        final WLRect box = item.bounds();
                        if (box != null) {
                            bounds.add(box.left(), box.top());
                            bounds.add(box.right(), box.bottom());
                        }
                    }
                }
                final int vertexCount = sink.finish();
                if (vertexCount == 0) {
                    continue;
                }
                built.add(new Built(group, vertexCount, start, sink.position() - start));
            }
            if (built.isEmpty()) {
                return;
            }
            if (this.vertices == null) {
                this.vertices = WLRenderSystem.vertexBuffer("walksylib content stratum verts");
            }
            try (final WLMapping map = this.vertices.map(sink.position())) {
                map.data().position(0).put(sink.contents());
            }
            final Matrix4f modelView = new Matrix4f().setTranslation(0f, 0f, -11000f);
            final WLTransform transform = WLRenderSystem.transform(modelView);
            final int targetHeightPx = WLRenderSystem.targetHeight();
            final WLUniformBuffer projection = ContentStratum.this.guiProjection();
            for (final WLTexture target : targets) {
                if (WalksyLibConstants.DEV_BUILD) {
                    UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
                }
                try (final WLPass pass = WLRenderSystem.pass(PASS_LABEL, target)) {
                    for (final Built b : built) {
                        final WLRect sc = b.group().scissor();
                        int[] rect = null;
                        if (sc != null) {
                            rect = DevicePixels.scissorToRenderPass(sc, targetHeightPx);
                            if (rect == null) {
                                continue;
                            }
                        }
                        pass.pipeline(pipelineFor(b.group().kind()));
                        pass.uniform("Projection", projection);
                        pass.transforms(transform);
                        if (b.group().kind().textured()) {
                            pass.texture("Sampler0", b.group().texture(), null);
                        }
                        if (rect != null) {
                            pass.scissor(rect[0], rect[1], rect[2], rect[3]);
                        } else {
                            pass.noScissor();
                        }
                        pass.drawQuads(this.vertices, b.byteOffset(), b.byteLength(), b.vertexCount());
                    }
                }
            }
        }
    }

    private static WLElementKind batchKind(final WLElement element) {
        final WLElementKind kind = element.kind();
        if (kind == WLElementKind.CORNER || kind == WLElementKind.CHECKER) {
            return kind;
        }
        if (element.texture() == null) {
            return WLElementKind.FLAT;
        }
        if (kind == WLElementKind.PREMULTIPLIED || kind == WLElementKind.GRAYSCALE || kind == WLElementKind.ROUNDED_TEXTURED) {
            return kind;
        }
        return WLElementKind.TEXTURED;
    }

    private static WLPipeline pipelineFor(final WLElementKind kind) {
        return WLRenderSystem.pipeline(switch (kind) {
            case FLAT, INVERT -> COLOR;
            case TEXTURED -> TEXTURED;
            case PREMULTIPLIED -> PREMULTIPLIED;
            case GRAYSCALE -> GRAYSCALE;
            case ROUNDED_TEXTURED -> ROUNDED_TEXTURED;
            case CORNER -> CORNER;
            case CHECKER -> CHECKER;
        });
    }

    public boolean isActive() {
        return this.row.isActive();
    }

    public void begin() {
        this.row.begin();
    }

    public void end() {
        this.row.end();
    }

    public void add(final WLElement element) {
        this.row.add(element);
    }

    public void flush(final WLTexture... targets) {
        this.row.flush(targets, WLRenderSystem.contentBounds());
    }

    public boolean hasRowContent() {
        return this.row.hasQueued();
    }

    public boolean hasPopoverContent() {
        return this.popover.hasQueued();
    }

    public boolean isPopoverActive() {
        return this.popover.isActive();
    }

    public void beginPopover() {
        this.beginPopover(DEFAULT_POPOVER_BOUNDARY);
    }

    public void beginPopover(final int panelLayer) {
        this.popover.begin();
        this.popoverContentBoundary = Math.max(this.popoverContentBoundary, panelLayer);
    }

    public int popoverContentBoundary() {
        return this.popoverContentBoundary;
    }

    public void endPopover() {
        this.popover.end();
    }

    public void addPopover(final WLElement element) {
        this.popover.add(element);
    }

    public void flushPopover(final WLTexture... targets) {
        this.popover.flush(targets, WLRenderSystem.popoverContentBounds());
    }

    public boolean isDevActive() {
        return this.dev.isActive();
    }

    public void beginDev() {
        this.dev.begin();
    }

    public void endDev() {
        this.dev.end();
    }

    public void addDev(final WLElement element) {
        this.dev.add(element);
    }

    public void flushDev(final WLTexture target) {
        this.dev.flush(new WLTexture[] {target}, null);
    }

    public boolean isTopActive() {
        return this.top.isActive();
    }

    public void beginTop() {
        this.top.begin();
    }

    public void endTop() {
        this.top.end();
    }

    public void addTop(final WLElement element) {
        this.top.add(element);
    }

    public void flushTop(final WLTexture target) {
        this.top.flush(new WLTexture[] {target}, null);
    }

    public void addDirect(final WLElement element) {
        this.direct.add(element);
    }

    public boolean hasDirect() {
        return this.direct.hasQueued();
    }

    public void flushDirect(final WLTexture target) {
        this.direct.flush(new WLTexture[] {target}, null);
    }

    public void discardStale() {
        this.popoverContentBoundary = DEFAULT_POPOVER_BOUNDARY;
        this.row.discardStale();
        this.popover.discardStale();
        this.dev.discardStale();
        this.top.discardStale();
        this.direct.discardStale();
    }

    private WLUniformBuffer guiProjection() {
        if (this.projectionUbo == null) {
            this.projectionUbo = WLRenderSystem.uniformBuffer("walksylib content projection", 64);
        }
        final Matrix4f proj = new Matrix4f().setOrtho(0f, DevicePixels.logicalWidth(), DevicePixels.logicalHeight(), 0f, 1000f, 21000f);
        try (final WLMapping map = this.projectionUbo.map()) {
            map.writer().putMat4f(proj);
        }
        return this.projectionUbo;
    }
}
