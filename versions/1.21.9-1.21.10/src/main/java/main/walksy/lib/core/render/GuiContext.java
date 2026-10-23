package main.walksy.lib.core.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import main.walksy.lib.core.render.element.VanillaElement;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public final class GuiContext {
    private static GuiGraphics lastExtractor;
    private static WLGraphics2D lastGraphics;
    private static RenderPipeline cornerPipeline;
    private static RenderPipeline checkerPipeline;
    private static RenderPipeline bitmapTextPipeline;
    private static RenderPipeline pixelTextPipeline;
    private final GuiGraphics extractor;

    private GuiContext(final GuiGraphics extractor) {
        this.extractor = extractor;
    }

    public static WLGraphics2D graphics(final GuiGraphics extractor) {
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

    public GuiGraphics extractor() {
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
    }

    public void nextStratum() {
        this.extractor.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.extractor.blurBeforeThisStratum();
    }

    public TextureAtlasSprite sprite(final Identifier atlas, final Identifier texture) {
        return this.extractor.getSprite(new Material(atlas, texture));
    }

    public void item(final ItemStack stack, final int x, final int y) {
        this.extractor.renderItem(stack, x, y);
    }

    public void itemDecorations(final Font font, final ItemStack stack, final int x, final int y) {
        this.extractor.renderItemDecorations(font, stack, x, y);
    }

    public void text(final Font font, final String text, final int x, final int y, final int color, final boolean shadow) {
        this.extractor.drawString(font, text, x, y, color, shadow);
    }

    public void blitSprite(final Identifier sprite, final int x, final int y, final int width, final int height) {
        this.extractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public WLTexture texture(final Identifier id) {
        return WLRenderSystem.texture(id);
    }

    public void widget(final AbstractWidget widget, final int mouseX, final int mouseY, final float delta) {
        widget.render(this.extractor, mouseX, mouseY, delta);
    }

    public boolean hudHidden() {
        return Minecraft.getInstance().options.hideGui;
    }

    public void extractHud() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.render(this.extractor, minecraft.getDeltaTracker());
    }

    public void flushHud() {
    }

    public static boolean vanillaBatching() {
        return true;
    }

    public void submit(final WLElement element) {
        this.extractor.guiRenderState.submitGuiElement(vanilla(element));
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
        return TextureSetup.singleTexture(texture.view());
    }

    private static RenderPipeline pipeline(final WLElementKind kind) {
        return switch (kind) {
            case FLAT -> RenderPipelines.GUI;
            case INVERT -> RenderPipelines.GUI_INVERT;
            case TEXTURED, ROUNDED_TEXTURED -> RenderPipelines.GUI_TEXTURED;
            case PREMULTIPLIED -> RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
            case GRAYSCALE -> RenderPipelines.GUI_TEXT_INTENSITY;
            case CORNER -> cornerPipeline();
            case CHECKER -> checkerPipeline();
            case BITMAP_TEXT -> bitmapTextPipeline();
            case BITMAP_TEXT_PIXEL -> pixelTextPipeline();
        };
    }

    private static RenderPipeline cornerPipeline() {
        if (cornerPipeline == null) {
            cornerPipeline = matricesPipeline("pipeline/rounded_gradient_rect", "program/rounded_corner");
        }
        return cornerPipeline;
    }

    private static RenderPipeline checkerPipeline() {
        if (checkerPipeline == null) {
            checkerPipeline = matricesPipeline("pipeline/rounded_checker", "program/rounded_checker");
        }
        return checkerPipeline;
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
            .withSampler("Sampler0")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();
        RenderSystem.getDevice().precompilePipeline(pipeline);
        return pipeline;
    }

    private static RenderPipeline matricesPipeline(final String location, final String fragment) {
        final RenderPipeline pipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("walksylib", location))
            .withVertexShader(Identifier.fromNamespaceAndPath("walksylib", "core/rounded_corner"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("walksylib", fragment))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();
        RenderSystem.getDevice().precompilePipeline(pipeline);
        return pipeline;
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
