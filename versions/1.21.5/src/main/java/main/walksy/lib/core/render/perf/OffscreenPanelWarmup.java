package main.walksy.lib.core.render.perf;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLGpu;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class OffscreenPanelWarmup {
    private static final String SNAPSHOT_LABEL = "walksylib warmup snapshot";

    private OffscreenPanelWarmup() {
    }

    public static void runIfPending() {
        if (!WLRenderSystem.supportsTextureCopy() || !WalksyLib.getInstance().getPanelWarmup().pending()) {
            return;
        }
        final long startedAt = System.nanoTime();
        final Minecraft client = Minecraft.getInstance();
        final WLTexture snapshot = WLRenderSystem.colorTexture(SNAPSHOT_LABEL, WLRenderSystem.targetWidth(), WLRenderSystem.targetHeight());
        WLRenderSystem.copyMainTo(snapshot);
        try {
            WLGpu.clearMainDepth();
            final GuiGraphics extractor = new GuiGraphics(client, client.renderBuffers().bufferSource());
            GuiContext.beginFrame(extractor);
            try {
                WalksyLib.getInstance().getPanelWarmup().onLoadingFinished(GuiContext.graphics(extractor));
            } finally {
                GuiContext.endFrame();
            }
            extractor.flush();
            GuiContext.resolveAll();
            WLRenderSystem.flushText();
            WLRenderSystem.renderGlassTop();
        } finally {
            WLGpu.copyToMain(snapshot);
            snapshot.close();
        }
        WalksyLib.getInstance().getPanelWarmup().deferDelay((System.nanoTime() - startedAt) / 1_000_000L);
    }
}
