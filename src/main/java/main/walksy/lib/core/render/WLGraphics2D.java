package main.walksy.lib.core.render;

import main.walksy.lib.core.render.element.WLBlitRenderState;
import main.walksy.lib.core.render.element.CheckerCircleRenderState;
import main.walksy.lib.core.render.element.WLColoredRectangleRenderState;
import main.walksy.lib.core.render.element.RadialGlowRenderState;
import main.walksy.lib.core.render.element.RoundedBlitRenderState;
import main.walksy.lib.core.render.element.RoundedGradientRectangleState;
import main.walksy.lib.core.render.element.StretchedBlitRenderState;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.gui.LiquidGlassGuiElementRenderState;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import main.walksy.lib.core.gui.style.MinecraftSurface;
import main.walksy.lib.core.gui.style.UiStyle;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WLGraphics2D {
    private final GuiContext context;
    private static List<WLElement> captureSink;
    private static final boolean SCALE_GUARD = FabricLoader.getInstance().isDevelopmentEnvironment();
    private static final Set<String> REPORTED_UNSCALED = ConcurrentHashMap.newKeySet();

    public WLGraphics2D(final GuiContext context) {
        this.context = context;
    }

    public GuiContext context() {
        return this.context;
    }

    public Matrix3x2fStack pose() {
        return this.context.pose();
    }

    public @Nullable WLRect scissor() {
        return this.context.scissor();
    }

    public void enableScissor(final int x0, final int y0, final int x1, final int y1) {
        final Matrix3x2fStack pose = this.pose();
        if (!DevicePixels.isAxisAligned(pose)) {
            this.context.enableScissor(x0, y0, x1, y1);
            return;
        }
        final float ax = pose.m00() * x0 + pose.m20();
        final float bx = pose.m00() * x1 + pose.m20();
        final float ay = pose.m11() * y0 + pose.m21();
        final float by = pose.m11() * y1 + pose.m21();
        pose.pushMatrix();
        pose.identity();
        try {
            this.context.enableScissor((int) Math.floor(Math.min(ax, bx)), (int) Math.floor(Math.min(ay, by)), (int) Math.floor(Math.max(ax, bx)), (int) Math.floor(Math.max(ay, by)));
        } finally {
            pose.popMatrix();
        }
    }

    public void enableScissorWithin(final float x0, final float y0, final float x1, final float y1) {
        final Matrix3x2fStack pose = this.pose();
        if (!DevicePixels.isAxisAligned(pose)) {
            this.context.enableScissor((int) Math.ceil(x0), (int) Math.ceil(y0), (int) Math.floor(x1), (int) Math.floor(y1));
            return;
        }
        final float ax = pose.m00() * x0 + pose.m20();
        final float bx = pose.m00() * x1 + pose.m20();
        final float ay = pose.m11() * y0 + pose.m21();
        final float by = pose.m11() * y1 + pose.m21();
        pose.pushMatrix();
        pose.identity();
        try {
            this.context.enableScissor((int) Math.ceil(Math.min(ax, bx)), (int) Math.ceil(Math.min(ay, by)), (int) Math.floor(Math.max(ax, bx)), (int) Math.floor(Math.max(ay, by)));
        } finally {
            pose.popMatrix();
        }
    }

    public void disableScissor() {
        this.context.disableScissor();
    }

    public boolean containsPointInScissor(final int x, final int y) {
        return this.context.containsPointInScissor(x, y);
    }

    public int guiWidth() {
        return this.context.guiWidth();
    }

    public int guiHeight() {
        return this.context.guiHeight();
    }

    public void nextStratum() {
        this.context.nextStratum();
    }

    public void pointingHandCursor() {
        this.context.pointingHandCursor();
    }

    public void blurBeforeThisStratum() {
        this.context.blurBeforeThisStratum();
    }

    public TextureAtlasSprite getSprite(final Identifier atlas, final Identifier texture) {
        return this.context.sprite(atlas, texture);
    }

    public void item(final ItemStack stack, final int x, final int y) {
        this.context.item(stack, x, y);
    }

    public void itemDecorations(final Font font, final ItemStack stack, final int x, final int y) {
        this.context.itemDecorations(font, stack, x, y);
    }

    public void text(final Font font, final String text, final int x, final int y, final int color, final boolean shadow) {
        this.context.text(font, text, x, y, color, shadow);
    }

    public void text(final Font font, final String text, final int x, final int y, final int color) {
        this.context.text(font, text, x, y, color, true);
    }

    public void blitSprite(final Identifier sprite, final int x, final int y, final int width, final int height) {
        this.context.blitSprite(sprite, x, y, width, height);
    }

    public void widget(final AbstractWidget widget, final int mouseX, final int mouseY, final float delta) {
        this.context.widget(widget, mouseX, mouseY, delta);
    }

    public void fill(final float x0, final float y0, final float x1, final float y1, final int color1, @Nullable final Integer color2) {
        final ContentDefocus defocus = WLRenderSystem.contentDefocus();
        if (defocus.isActive()) {
            for (int i = 0; i < defocus.tapCount(); i++) {
                final int c1 = defocus.tapColor(color1, i);
                final Integer c2 = color2 != null ? defocus.tapColor(color2, i) : null;
                if ((c1 >>> 24) == 0 && (c2 == null || (c2 >>> 24) == 0)) {
                    continue;
                }
                final float dx = defocus.tapDx(i);
                final float dy = defocus.tapDy(i);
                this.fillCrisp(x0 + dx, y0 + dy, x1 + dx, y1 + dy, c1, c2);
            }
            return;
        }
        this.fillCrisp(x0, y0, x1, y1, color1, color2);
    }

    private void fillCrisp(float x0, float y0, float x1, float y1, final int color1, @Nullable final Integer color2) {
        if (x0 < x1) {
            final float tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        if (y0 < y1) {
            final float tmp = y0;
            y0 = y1;
            y1 = tmp;
        }
        this.submit(new WLColoredRectangleRenderState(new Matrix3x2f(this.pose()), x0, y0, x1, y1, color1, color2 != null ? color2 : color1, this.scissor()));
    }

    public static void capture(final List<WLElement> sink, final Runnable body) {
        final List<WLElement> previous = captureSink;
        captureSink = sink;
        try {
            body.run();
        } finally {
            captureSink = previous;
        }
    }

    public void submit(final WLElement element) {
        final ContentStratum stratum = WLRenderSystem.contentStratum();
        if (captureSink != null) {
            captureSink.add(element);
        } else if (stratum.isDevActive()) {
            stratum.addDev(element);
        } else if (stratum.isTopActive()) {
            stratum.addTop(element);
        } else if (stratum.isPopoverActive()) {
            stratum.addPopover(element);
        } else if (stratum.isActive()) {
            stratum.add(element);
        } else {
            this.context.submit(element);
        }
    }

    public void radialGlow(final float cx, final float cy, final float radius, final int color) {
        if (UiStyle.minecraft()) {
            return;
        }
        this.submit(new RadialGlowRenderState(new Matrix3x2f(this.pose()), cx, cy, radius, color, this.scissor()));
    }

    public void fillRoundedGradient(final float x0, final float y0, final float x1, final float y1, final int colTL, final int colTR, final int colBR, final int colBL, final float cornerRadius) {
        final float radius = UiStyle.minecraft() ? 0f : cornerRadius;
        if (WLRenderSystem.contentDefocus().isActive()) {
            for (int i = 0; i < WLRenderSystem.contentDefocus().tapCount(); i++) {
                final int tl = WLRenderSystem.contentDefocus().tapColor(colTL, i);
                final int tr = WLRenderSystem.contentDefocus().tapColor(colTR, i);
                final int br = WLRenderSystem.contentDefocus().tapColor(colBR, i);
                final int bl = WLRenderSystem.contentDefocus().tapColor(colBL, i);
                if ((tl >>> 24) == 0 && (tr >>> 24) == 0 && (br >>> 24) == 0 && (bl >>> 24) == 0) {
                    continue;
                }
                final float dx = WLRenderSystem.contentDefocus().tapDx(i);
                final float dy = WLRenderSystem.contentDefocus().tapDy(i);
                this.fillRoundedGradientCrisp(x0 + dx, y0 + dy, x1 + dx, y1 + dy, tl, tr, br, bl, radius);
            }
            return;
        }
        this.fillRoundedGradientCrisp(x0, y0, x1, y1, colTL, colTR, colBR, colBL, radius);
    }

    private void fillRoundedGradientCrisp(final float x0, final float y0, final float x1, final float y1, final int colTL, final int colTR, final int colBR, final int colBL, final float radius) {
        this.submit(new RoundedGradientRectangleState(new Matrix3x2f(this.pose()), x0, y0, x1, y1, colTL, colTR, colBR, colBL, radius, this.scissor()));
    }

    public void fillCheckerCircle(final float cx, final float cy, final float radius, final float alpha, final boolean flip) {
        this.submit(new CheckerCircleRenderState(new Matrix3x2f(this.pose()), cx, cy, radius, alpha, flip, this.scissor()));
    }

    public void blit(final Identifier texture, final float x, final float y, final float u, final float v, final float width, final float height, final int srcWidth, final int srcHeight, final int textureWidth, final int textureHeight, final int color) {
        this.innerBlit(texture, x, x + width, y, y + height, (u + 0.0F) / (float) textureWidth, (u + (float) srcWidth) / (float) textureWidth, (v + 0.0F) / (float) textureHeight, (v + (float) srcHeight) / (float) textureHeight, color);
    }

    private void innerBlit(final Identifier location, final float x0, final float x1, final float y0, final float y1, final float u0, final float u1, final float v0, final float v1, final int color) {
        this.innerBlit(this.context.texture(location), x0, y0, x1, y1, u0, u1, v0, v1, color);
    }

    private void innerBlit(final WLTexture requested, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color) {
        final WLTexture texture = stretchSafe(requested);
        if (WLRenderSystem.contentDefocus().isActive()) {
            for (int i = 0; i < WLRenderSystem.contentDefocus().tapCount(); i++) {
                final int c = WLRenderSystem.contentDefocus().tapColor(color, i);
                if ((c >>> 24) == 0) {
                    continue;
                }
                final float dx = WLRenderSystem.contentDefocus().tapDx(i);
                final float dy = WLRenderSystem.contentDefocus().tapDy(i);
                this.submit(this.blitState(texture, x0 + dx, y0 + dy, x1 + dx, y1 + dy, u0, u1, v0, v1, c));
            }
            return;
        }
        this.submit(this.blitState(texture, x0, y0, x1, y1, u0, u1, v0, v1, color));
    }

    private static WLTexture stretchSafe(final WLTexture requested) {
        return ContentStretch.current() == null ? requested : WLRenderSystem.withFilter(requested, WLFilter.LINEAR);
    }

    private WLElement blitState(final WLTexture texture, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color) {
        final Matrix3x2f pose = new Matrix3x2f(this.pose());
        final WLRect scissor = this.scissor();
        final ContentStretch stretch = ContentStretch.current();
        if (stretch != null) {
            return new StretchedBlitRenderState(WLElementKind.TEXTURED, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, stretch, scissor);
        }
        return new WLBlitRenderState(WLElementKind.TEXTURED, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, scissor);
    }

    public void blitUv(final Identifier texture, final float x, final float y, final float width, final float height, final float u0, final float v0, final float u1, final float v1, final int color) {
        this.innerBlit(texture, x, x + width, y, y + height, u0, u1, v0, v1, color);
    }

    public void blitRounded(final Identifier texture, final float x, final float y, final float u, final float v, final float width, final float height, final int srcWidth, final int srcHeight, final int textureWidth, final int textureHeight, final int color, final float cornerRadius) {
        final boolean shrunk = width * DevicePixels.perLogicalX() * poseScaleX(this.pose()) < srcWidth || height * DevicePixels.perLogicalY() * poseScaleY(this.pose()) < srcHeight;
        this.innerBlitRounded(texture, x, x + width, y, y + height, (u + 0.0F) / (float) textureWidth, (u + (float) srcWidth) / (float) textureWidth, (v + 0.0F) / (float) textureHeight, (v + (float) srcHeight) / (float) textureHeight, color, cornerRadius, shrunk);
    }

    private void innerBlitRounded(final Identifier location, final float x0, final float x1, final float y0, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final float cornerRadius, final boolean shrunk) {
        this.innerBlitRounded(this.context.texture(location), x0, y0, x1, y1, u0, u1, v0, v1, color, cornerRadius, shrunk);
    }

    private static float poseScaleX(final Matrix3x2f pose) {
        return (float) Math.hypot(pose.m00(), pose.m01());
    }

    private static float poseScaleY(final Matrix3x2f pose) {
        return (float) Math.hypot(pose.m10(), pose.m11());
    }

    private boolean submitsToStratum() {
        final ContentStratum stratum = WLRenderSystem.contentStratum();
        return captureSink == null && (stratum.isDevActive() || stratum.isTopActive() || stratum.isPopoverActive() || stratum.isActive());
    }

    private void innerBlitRounded(final WLTexture requested, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final float roundedRadius, final boolean shrunk) {
        final float cornerRadius = UiStyle.minecraft() ? 0f : roundedRadius;
        final WLTexture texture = shrunk ? WLRenderSystem.withFilter(requested, WLFilter.LINEAR) : stretchSafe(requested);
        final WLElementKind kind = this.submitsToStratum() && (color & 0x00FFFFFF) == 0x00FFFFFF ? WLElementKind.ROUNDED_TEXTURED : WLElementKind.TEXTURED;
        if (WLRenderSystem.contentDefocus().isActive()) {
            for (int i = 0; i < WLRenderSystem.contentDefocus().tapCount(); i++) {
                final int c = WLRenderSystem.contentDefocus().tapColor(color, i);
                if ((c >>> 24) == 0) {
                    continue;
                }
                final float dx = WLRenderSystem.contentDefocus().tapDx(i);
                final float dy = WLRenderSystem.contentDefocus().tapDy(i);
                this.submit(new RoundedBlitRenderState(kind, texture, new Matrix3x2f(this.pose()), x0 + dx, y0 + dy, x1 + dx, y1 + dy, u0, u1, v0, v1, c, cornerRadius, ContentStretch.current(), this.scissor()));
            }
            return;
        }
        this.submit(new RoundedBlitRenderState(kind, texture, new Matrix3x2f(this.pose()), x0, y0, x1, y1, u0, u1, v0, v1, color, cornerRadius, ContentStretch.current(), this.scissor()));
    }

    private static void warnIfUnscaled(final GlassStyle style) {
        if (style == null || style.isScaleResolved()) {
            return;
        }
        final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String site = "unknown";
        for (final StackTraceElement e : trace) {
            final String cn = e.getClassName();
            if (cn.startsWith("main.walksy.lib") && !cn.contains("WLGraphics2D")) {
                site = cn + "." + e.getMethodName() + ":" + e.getLineNumber();
                break;
            }
        }
        REPORTED_UNSCALED.add(site);
    }

    public GlassBuilder glass() {
        return new GlassBuilder(this);
    }

    public static final class GlassBuilder {
        private final WLGraphics2D graphics;
        private float x, y, width, height;
        private float cornerRadius = -1f;
        @Nullable private Component text = null;
        private GlassStyle style = new GlassStyle();
        private float hoverAmount = 0f;
        private float focusAmount = 0f;
        private float stretchX = 0f;
        private float stretchY = 0f;
        private boolean screenSpace = false;
        private float minecraftAmount = -1f;

        private GlassBuilder(final WLGraphics2D graphics) {
            this.graphics = graphics;
        }

        public GlassBuilder fromWidget(final AbstractWidget widget) {
            this.position(widget.getX(), widget.getY());
            this.size(widget.getWidth(), widget.getHeight());
            this.text(widget.getMessage());
            return this;
        }

        public GlassBuilder position(final float x, final float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public GlassBuilder size(final float width, final float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public GlassBuilder dimensions(final float x, final float y, final float width, final float height) {
            return this.position(x, y).size(width, height);
        }

        public GlassBuilder cornerRadius(final float radius) {
            this.cornerRadius = radius;
            return this;
        }

        public GlassBuilder text(final Component text) {
            this.text = text;
            return this;
        }

        public GlassBuilder style(final GlassStyle style) {
            this.style = style;
            return this;
        }

        public GlassBuilder hover(float amount) {
            if (Float.isNaN(amount)) {
                amount = 0f;
            }
            this.hoverAmount = Math.max(0f, Math.min(1f, amount));
            return this;
        }

        public GlassBuilder focus(float amount) {
            if (Float.isNaN(amount)) {
                amount = 0f;
            }
            this.focusAmount = Math.max(0f, Math.min(1f, amount));
            return this;
        }

        public GlassBuilder selected(final float amount) {
            return this.focus(amount);
        }

        public GlassBuilder screenSpace() {
            this.screenSpace = true;
            return this;
        }

        public GlassBuilder stretch(final float pullX, final float pullY) {
            this.stretchX = Float.isFinite(pullX) ? pullX : 0f;
            this.stretchY = Float.isFinite(pullY) ? pullY : 0f;
            return this;
        }

        public GlassBuilder minecraftAmount(final float amount) {
            this.minecraftAmount = Float.isFinite(amount) && amount >= 0f ? Math.min(1f, amount) : -1f;
            return this;
        }

        public void extract() {
            final float minecraft = this.minecraftAmount < 0f ? (UiStyle.minecraft() ? 1f : 0f) : this.minecraftAmount;
            if (minecraft >= 1f) {
                this.style = MinecraftSurface.of(this.style, Math.max(this.hoverAmount, this.focusAmount) > 0.5f);
                this.cornerRadius = 0f;
                this.hoverAmount = 0f;
                this.focusAmount = 0f;
                this.stretchX = 0f;
                this.stretchY = 0f;
            } else if (minecraft > 0f) {
                final float keep = 1f - minecraft;
                this.style = MinecraftSurface.blend(this.style, minecraft);
                this.cornerRadius = (this.cornerRadius < 0 ? 0.5f * Math.min(this.width, this.height) : this.cornerRadius) * keep;
                this.hoverAmount *= keep;
                this.focusAmount *= keep;
                this.stretchX *= keep;
                this.stretchY *= keep;
            }
            final float finalCornerRadius = this.cornerRadius < 0 ? 0.5f * Math.min(this.width, this.height) : this.cornerRadius;
            final Matrix3x2f pose = this.screenSpace ? new Matrix3x2f() : new Matrix3x2f(this.graphics.pose());
            final WLRect scissorRect = this.graphics.scissor();
            if (SCALE_GUARD) {
                warnIfUnscaled(this.style);
            }
            final LiquidGlassGuiElementRenderState element = new LiquidGlassGuiElementRenderState(this.x, this.y, this.x + this.width, this.y + this.height, finalCornerRadius, this.text, this.style, pose, scissorRect, this.hoverAmount, this.focusAmount, this.stretchX, this.stretchY);
            if (WLRenderSystem.contentStratum().isTopActive()) {
                WLRenderSystem.glassUniforms().addTopWidget(element);
                return;
            }
            WLRenderSystem.glassUniforms().requestGlassPass(this.graphics);
            WLRenderSystem.glassUniforms().addWidget(element);
            this.graphics.context().submit(element);
        }
    }
}
