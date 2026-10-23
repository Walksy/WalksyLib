package main.walksy.lib.core.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import main.walksy.lib.core.render.element.VanillaElement;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public final class GuiContext {
    private static GuiGraphicsExtractor lastExtractor;
    private static WLGraphics2D lastGraphics;
    private static RenderPipeline cornerPipeline;
    private static RenderPipeline checkerPipeline;
    private static RenderPipeline bitmapTextPipeline;
    private static RenderPipeline pixelTextPipeline;
    private final GuiGraphicsExtractor extractor;

    private GuiContext(final GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
    }

    public static WLGraphics2D graphics(final GuiGraphicsExtractor extractor) {
        if (lastExtractor != extractor || lastGraphics == null) {
            lastGraphics = new WLGraphics2D(new GuiContext(extractor));
            lastExtractor = extractor;
        }
        return lastGraphics;
    }

    public static @Nullable WLRect rect(@Nullable final ScreenRectangle rectangle) {
        return rectangle == null ? null : new WLRect(rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height());
    }

    public static @Nullable ScreenRectangle screen(@Nullable final WLRect rect) {
        return rect == null ? null : new ScreenRectangle(rect.left(), rect.top(), rect.width(), rect.height());
    }

    public GuiGraphicsExtractor extractor() {
        return this.extractor;
    }

    public Matrix3x2fStack pose() {
        return this.extractor.pose();
    }

    public @Nullable WLRect scissor() {
        return rect(this.extractor.scissorStack.peek());
    }

    public void enableScissor(final int x0, final int y0, final int x1, final int y1) {
        this.extractor.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        this.extractor.disableScissor();
    }

    public boolean containsPointInScissor(final int x, final int y) {
        return this.extractor.containsPointInScissor(x, y);
    }

    public int guiWidth() {
        return this.extractor.guiWidth();
    }

    public int guiHeight() {
        return this.extractor.guiHeight();
    }

    public void pointingHandCursor() {
        this.extractor.requestCursor(CursorTypes.POINTING_HAND);
    }

    public void nextStratum() {
        this.extractor.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.extractor.blurBeforeThisStratum();
    }

    public TextureAtlasSprite sprite(final Identifier atlas, final Identifier texture) {
        return this.extractor.getSprite(new SpriteId(atlas, texture));
    }

    public void item(final ItemStack stack, final int x, final int y) {
        this.extractor.item(stack, x, y);
    }

    public void itemDecorations(final Font font, final ItemStack stack, final int x, final int y) {
        this.extractor.itemDecorations(font, stack, x, y);
    }

    public void text(final Font font, final String text, final int x, final int y, final int color, final boolean shadow) {
        this.extractor.text(font, text, x, y, color, shadow);
    }

    public void blitSprite(final Identifier sprite, final int x, final int y, final int width, final int height) {
        this.extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public WLTexture texture(final Identifier id) {
        final var texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        return WLTexture.wrap(texture.getTextureView(), texture.getSampler());
    }

    public void widget(final AbstractWidget widget, final int mouseX, final int mouseY, final float delta) {
        widget.extractRenderState(this.extractor, mouseX, mouseY, delta);
    }

    public boolean hudHidden() {
        return Minecraft.getInstance().gui.hud.isHidden();
    }

    public void extractHud() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.hud.extractRenderState(this.extractor, minecraft.getDeltaTracker());
    }

    public void flushHud() {
    }

    public static boolean vanillaBatching() {
        return true;
    }

    public void submit(final WLElement element) {
        this.extractor.guiRenderState.addGuiElement(vanilla(element));
    }

    public static GuiElementRenderState vanilla(final WLElement element) {
        if (element instanceof final VanillaElement vanilla) {
            return vanilla.state();
        }
        return new VanillaState(element, pipeline(element.kind()), textureSetup(element.texture()), screen(element.scissorArea()), screen(element.bounds()));
    }

    private static TextureSetup textureSetup(@Nullable final WLTexture texture) {
        if (texture == null) {
            return TextureSetup.noTexture();
        }
        return TextureSetup.singleTexture(texture.view(), texture.sampler() != null ? texture.sampler() : RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    private static RenderPipeline pipeline(final WLElementKind kind) {
        return switch (kind) {
            case FLAT -> RenderPipelines.GUI;
            case INVERT -> RenderPipelines.GUI_INVERT;
            case TEXTURED, ROUNDED_TEXTURED -> RenderPipelines.GUI_TEXTURED;
            case PREMULTIPLIED -> RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
            case GRAYSCALE -> RenderPipelines.GUI_TEXT_GRAYSCALE;
            case CORNER -> cornerPipeline();
            case CHECKER -> checkerPipeline();
            case BITMAP_TEXT -> bitmapTextPipeline();
            case BITMAP_TEXT_PIXEL -> pixelTextPipeline();
        };
    }

    private static RenderPipeline bitmapTextPipeline() {
        if (bitmapTextPipeline == null) {
            bitmapTextPipeline = textPipeline("pipeline/gui_bitmap_text", "program/bitmap_text");
        }
        return bitmapTextPipeline;
    }

    private static RenderPipeline pixelTextPipeline() {
        if (pixelTextPipeline == null) {
            pixelTextPipeline = textPipeline("pipeline/gui_bitmap_text_pixel", "program/bitmap_text_pixel");
        }
        return pixelTextPipeline;
    }

    private static RenderPipeline textPipeline(final String location, final String fragment) {
        final RenderPipeline pipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("walksylib", location))
            .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", "core/msdf_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", fragment))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
        RenderSystem.getDevice().precompilePipeline(pipeline);
        return pipeline;
    }

    private static RenderPipeline cornerPipeline() {
        if (cornerPipeline == null) {
            cornerPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("walksylib", "pipeline/rounded_gradient_rect"))
                .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", "core/rounded_corner"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", "program/rounded_corner"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();
            RenderSystem.getDevice().precompilePipeline(cornerPipeline);
        }
        return cornerPipeline;
    }

    private static RenderPipeline checkerPipeline() {
        if (checkerPipeline == null) {
            checkerPipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("walksylib", "pipeline/rounded_checker"))
                .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", "core/rounded_corner"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", "program/rounded_checker"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();
            RenderSystem.getDevice().precompilePipeline(checkerPipeline);
        }
        return checkerPipeline;
    }

    private record VanillaState(WLElement element, RenderPipeline pipeline, TextureSetup textureSetup, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            this.element.emit(new ConsumerSink(consumer));
        }
    }

    private record ConsumerSink(VertexConsumer consumer) implements WLVertexSink {
        @Override
        public WLVertexSink vertex(final float x, final float y) {
            this.consumer.addVertex(x, y, 0f);
            return this;
        }

        @Override
        public WLVertexSink uv(final float u, final float v) {
            this.consumer.setUv(u, v);
            return this;
        }

        @Override
        public WLVertexSink color(final int argb) {
            this.consumer.setColor(argb);
            return this;
        }
    }
}
