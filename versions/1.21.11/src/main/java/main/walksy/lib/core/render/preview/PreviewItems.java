package main.walksy.lib.core.render.preview;

import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PreviewItems {
    private static final Map<Object, PreviewEntityBlit> PENDING = new IdentityHashMap<>();
    private static final Map<String, TrackingItemStackRenderState> STATES = new HashMap<>();
    private static final Map<String, PreviewItemRenderer> RENDERERS = new HashMap<>();
    private static final int MAX_STATES = 512;

    private PreviewItems() {
    }

    public static boolean capture(final Object state, final WLTexture texture) {
        final PreviewEntityBlit blit = PENDING.remove(state);
        if (blit == null) {
            return false;
        }
        blit.texture(texture);
        return true;
    }

    public static boolean prepare(final PictureInPictureRenderState state, final GuiRenderState guiRenderState, final MultiBufferSource.BufferSource bufferSource, final int guiScale) {
        if (!(state instanceof PreviewItemRenderState item)) {
            return false;
        }
        final PreviewItemRenderer renderer = RENDERERS.computeIfAbsent(item.rendererKey(), key -> new PreviewItemRenderer(bufferSource));
        renderer.markUsed();
        renderer.prepare(item, guiRenderState, guiScale);
        return true;
    }

    public static void clearUnused() {
        PENDING.clear();
        RENDERERS.values().removeIf(renderer -> {
            if (renderer.usedOnThisFrame()) {
                renderer.resetUsedOnThisFrame();
                return false;
            }
            renderer.close();
            return true;
        });
    }

    public static boolean submit(final WLGraphics2D graphics, final String key, final ItemStack stack, final float x, final float y, final float size, final float alpha) {
        if (stack.isEmpty() || size <= 0f) {
            return false;
        }
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final float screenX = pose.m00() * x + pose.m20();
        final float screenY = pose.m11() * y + pose.m21();
        final int x0 = Math.round(screenX);
        final int y0 = Math.round(screenY);
        final int x1 = x0 + Math.round(pose.m00() * size);
        final int y1 = y0 + Math.round(pose.m11() * size);
        if (x1 <= x0 || y1 <= y0) {
            return false;
        }
        final TrackingItemStackRenderState item = stateFor(key, stack);
        if (item == null) {
            return false;
        }
        final var extractor = graphics.context().extractor();
        final PreviewItemRenderState state = PreviewItemRenderState.of(key, item, x0, y0, x1, y1, extractor.scissorStack.peek());
        final PreviewEntityBlit blit = new PreviewEntityBlit(screenX, screenY, screenX + (x1 - x0), screenY + (y1 - y0), alpha, graphics.scissor());
        PENDING.put(state, blit);
        graphics.submit(blit);
        extractor.guiRenderState.submitPicturesInPictureState(state);
        return true;
    }

    private static TrackingItemStackRenderState stateFor(final String key, final ItemStack stack) {
        final TrackingItemStackRenderState cached = STATES.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            final TrackingItemStackRenderState built = new TrackingItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(built, stack, ItemDisplayContext.GUI, null, null, 0);
            if (STATES.size() > MAX_STATES) {
                STATES.clear();
            }
            STATES.put(key, built);
            return built;
        } catch (final RuntimeException unusable) {
            return null;
        }
    }
}
