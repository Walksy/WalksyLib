package main.walksy.lib.core.render.preview;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public final class PreviewItems {
    private static final float NATIVE_SIZE = 16f;
    private static @Nullable TextureTarget target;
    private static long clearedFrame = -1L;
    private static boolean redirecting;

    private PreviewItems() {
    }

    public static @Nullable RenderTarget redirectedTarget() {
        return redirecting ? target : null;
    }

    public static boolean submit(final WLGraphics2D graphics, final String key, final ItemStack stack, final float x, final float y, final float size, final float alpha) {
        if (stack.isEmpty() || size <= 0f) {
            return false;
        }
        final Matrix3x2f flat = new Matrix3x2f(graphics.pose());
        final float x0 = flat.m00() * x + flat.m20();
        final float y0 = flat.m11() * y + flat.m21();
        final float x1 = flat.m00() * (x + size) + flat.m20();
        final float y1 = flat.m11() * (y + size) + flat.m21();
        if (x1 <= x0 || y1 <= y0) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget main = minecraft.getMainRenderTarget();
        final TextureTarget offscreen = offscreen(main.width, main.height);
        final GuiGraphics guiGraphics = graphics.context().extractor();
        guiGraphics.flush();
        if (clearedFrame != WLRenderSystem.frameId()) {
            clearedFrame = WLRenderSystem.frameId();
            offscreen.clear();
        }
        final PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.last().pose().identity();
        pose.translate(x0, y0, 0f);
        pose.scale((x1 - x0) / NATIVE_SIZE, (y1 - y0) / NATIVE_SIZE, 1f);
        redirecting = true;
        offscreen.bindWrite(false);
        try {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.flush();
        } finally {
            redirecting = false;
            main.bindWrite(false);
            pose.popPose();
        }
        final float guiScale = (float) minecraft.getWindow().getGuiScale();
        final PreviewEntityBlit blit = new PreviewEntityBlit(x0, y0, x1, y1, x0 * guiScale / offscreen.width, 1f - y0 * guiScale / offscreen.height, x1 * guiScale / offscreen.width, 1f - y1 * guiScale / offscreen.height, alpha, graphics.scissor());
        blit.texture(WLTexture.wrap(offscreen.getColorTextureId()));
        graphics.submit(blit);
        return true;
    }

    private static TextureTarget offscreen(final int width, final int height) {
        if (target == null) {
            target = new TextureTarget(width, height, true);
            target.setClearColor(0f, 0f, 0f, 0f);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
            clearedFrame = -1L;
        }
        return target;
    }
}
