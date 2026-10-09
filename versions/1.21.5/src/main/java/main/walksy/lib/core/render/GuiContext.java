package main.walksy.lib.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.glass.gui.LiquidGlassGuiElementRenderState;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public final class GuiContext {
    private static final Deque<Frame> FRAMES = new ArrayDeque<>();
    private static GuiGraphics lastExtractor;
    private static WLGraphics2D lastGraphics;
    private final GuiGraphics extractor;
    private final Matrix3x2fStack pose = new Matrix3x2fStack(64);

    private GuiContext(final GuiGraphics extractor) {
        this.extractor = extractor;
    }

    public static WLGraphics2D graphics(final GuiGraphics extractor) {
        if (lastExtractor != extractor || lastGraphics == null) {
            lastGraphics = new WLGraphics2D(new GuiContext(extractor));
            lastExtractor = extractor;
        }
        if (FRAMES.isEmpty()) {
            lastGraphics.context().syncPose();
        }
        return lastGraphics;
    }

    public static WLGraphics2D current() {
        return lastGraphics;
    }

    public static void beginFrame(final GuiGraphics extractor) {
        FRAMES.push(new Frame(graphics(extractor).context()));
    }

    public static void endFrame() {
        final Frame frame = FRAMES.poll();
        if (frame != null) {
            frame.resolve();
        }
    }

    public static void resolveAll() {
        while (!FRAMES.isEmpty()) {
            FRAMES.pop().resolve();
        }
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
        return this.pose;
    }

    public @Nullable WLRect scissor() {
        return rect(this.extractor.scissorStack.stack.peekLast());
    }

    public void enableScissor(final int x0, final int y0, final int x1, final int y1) {
        this.extractor.scissorStack.push(screen(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(this.pose)));
    }

    public void disableScissor() {
        this.extractor.scissorStack.pop();
    }

    public boolean containsPointInScissor(final int x, final int y) {
        final WLRect rect = this.scissor();
        return rect == null || (x >= rect.left() && x < rect.right() && y >= rect.top() && y < rect.bottom());
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
        if (this.recording()) {
            final Frame frame = FRAMES.pop();
            frame.resolve();
            FRAMES.push(new Frame(this));
        }
    }

    public void blurBeforeThisStratum() {
        if (this.recording()) {
            FRAMES.peek().glass = true;
        }
    }

    public TextureAtlasSprite sprite(final Identifier atlas, final Identifier texture) {
        return Minecraft.getInstance().getTextureAtlas(atlas).apply(texture);
    }

    public void item(final ItemStack stack, final int x, final int y) {
        this.vanilla(graphics -> graphics.renderItem(stack, x, y));
    }

    public void itemDecorations(final Font font, final ItemStack stack, final int x, final int y) {
        this.vanilla(graphics -> graphics.renderItemDecorations(font, stack, x, y));
    }

    public void text(final Font font, final String text, final int x, final int y, final int color, final boolean shadow) {
        this.vanilla(graphics -> graphics.drawString(font, text, x, y, color, shadow));
    }

    public void blitSprite(final Identifier sprite, final int x, final int y, final int width, final int height) {
        this.vanilla(graphics -> graphics.blitSprite(RenderType::guiTextured, sprite, x, y, width, height));
    }

    public WLTexture texture(final Identifier id) {
        return WLRenderSystem.texture(id);
    }

    public void widget(final AbstractWidget widget, final int mouseX, final int mouseY, final float delta) {
        this.renderable(widget, mouseX, mouseY, delta);
    }

    public void renderable(final Renderable renderable, final int mouseX, final int mouseY, final float delta) {
        if (renderable instanceof WLWidget && this.recording()) {
            renderable.render(this.extractor, mouseX, mouseY, delta);
            return;
        }
        this.vanilla(graphics -> renderable.render(graphics, mouseX, mouseY, delta));
    }

    public boolean hudHidden() {
        return Minecraft.getInstance().options.hideGui;
    }

    public void extractHud() {
        final Minecraft minecraft = Minecraft.getInstance();
        this.vanilla(graphics -> minecraft.gui.render(graphics, minecraft.getDeltaTracker()));
    }

    public void submit(final WLElement element) {
        final boolean glass = element instanceof LiquidGlassGuiElementRenderState;
        if (this.recording()) {
            if (!glass) {
                FRAMES.peek().ops.add(element);
            }
            return;
        }
        if (glass) {
            this.extractor.flush();
            this.flushDirect();
            WLRenderSystem.renderGlass();
            WLRenderSystem.glassUniforms().beginMainPass();
            return;
        }
        WLRenderSystem.contentStratum().addDirect(element);
        this.flushDirect();
    }

    public void immediate(final Consumer<GuiGraphics> body) {
        this.flushDirect();
        this.run(new Matrix3x2f(this.pose), this.scissor(), body);
    }

    public void immediateScreen(final Consumer<GuiGraphics> body) {
        this.flushDirect();
        this.run(new Matrix3x2f(), null, body);
    }

    private boolean recording() {
        final Frame frame = FRAMES.peek();
        return frame != null && frame.context == this;
    }

    public void vanilla(final Consumer<GuiGraphics> body) {
        if (this.recording()) {
            FRAMES.peek().ops.add(new Deferred(new Matrix3x2f(this.pose), this.scissor(), body));
            return;
        }
        this.immediate(body);
    }

    private void syncPose() {
        final Matrix4f matrix = this.extractor.pose().last().pose();
        this.pose.set(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m30(), matrix.m31());
    }

    private void flushDirect() {
        if (WLRenderSystem.contentStratum().hasDirect()) {
            this.extractor.flush();
            WLRenderSystem.contentStratum().flushDirect(WLRenderSystem.mainTarget());
        }
    }

    private void run(final Matrix3x2f matrix, @Nullable final WLRect scissor, final Consumer<GuiGraphics> body) {
        final PoseStack stack = this.extractor.pose();
        if (scissor != null) {
            stack.pushPose();
            stack.last().pose().identity();
            this.extractor.enableScissor(scissor.left(), scissor.top(), scissor.right(), scissor.bottom());
            stack.popPose();
        }
        stack.pushPose();
        stack.last()
            .pose()
            .m00(matrix.m00())
            .m01(matrix.m01())
            .m10(matrix.m10())
            .m11(matrix.m11())
            .m30(matrix.m20())
            .m31(matrix.m21());
        try {
            body.accept(this.extractor);
        } finally {
            stack.popPose();
            if (scissor != null) {
                this.extractor.disableScissor();
            }
        }
    }

    private record Deferred(Matrix3x2f pose, @Nullable WLRect scissor, Consumer<GuiGraphics> body) {
    }

    private static final class Frame {
        private final GuiContext context;
        private final List<Object> ops = new ArrayList<>();
        private boolean glass;

        private Frame(final GuiContext context) {
            this.context = context;
        }

        private void resolve() {
            this.context.extractor.flush();
            if (this.glass) {
                WLRenderSystem.renderGlass();
                WLRenderSystem.glassUniforms().beginMainPass();
            }
            for (final Object op : this.ops) {
                if (op instanceof final WLElement element) {
                    WLRenderSystem.contentStratum().addDirect(element);
                } else if (op instanceof final Deferred deferred) {
                    this.context.flushDirect();
                    this.context.run(deferred.pose(), deferred.scissor(), deferred.body());
                }
            }
            this.context.flushDirect();
        }
    }
}
