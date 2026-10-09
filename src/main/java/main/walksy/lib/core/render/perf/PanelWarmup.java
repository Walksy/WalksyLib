package main.walksy.lib.core.render.perf;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import net.minecraft.client.Minecraft;

public final class PanelWarmup {
    private static final String TITLE = "Warmup";
    private boolean done;
    private boolean requested;
    private long deferredDelayMs;

    public void request() {
        if (!this.done) {
            this.requested = true;
        }
    }

    public boolean pending() {
        return this.requested && !this.done && this.ready();
    }

    public void deferDelay(final long ms) {
        this.deferredDelayMs += ms;
    }

    public long takeDeferredDelay() {
        final long delay = this.deferredDelayMs;
        this.deferredDelayMs = 0L;
        return delay;
    }

    public long onLoadingFinished(final WLGraphics2D graphics) {
        if (this.done || graphics == null || !this.ready()) {
            return 0L;
        }
        this.done = true;
        final long startedAt = System.nanoTime();
        try {
            this.warm(graphics);
        } catch (final Throwable broken) {
            WalksyLib.getLogger().err("Panel warmup failed, be prepared for the smoothest opening experience!: " + broken);
        }
        final long tookMs = (System.nanoTime() - startedAt) / 1_000_000L;
        if (WalksyLibConstants.DEV_BUILD) {
            WalksyLib.getLogger().info("Panel warmup took " + tookMs + " ms");
        }
        return tookMs;
    }

    private boolean ready() {
        final Minecraft client = Minecraft.getInstance();
        return client != null && client.level == null && Fonts.SFPRO_DISPLAY != null && client.getWindow().getGuiScaledWidth() > 0 && client.getWindow().getGuiScaledHeight() > 0;
    }

    private void warm(final WLGraphics2D graphics) {
        final Minecraft client = Minecraft.getInstance();
        final int width = client.getWindow().getGuiScaledWidth();
        final int height = client.getWindow().getGuiScaledHeight();
        final WalksyLibPanelScreen screen = new WalksyLibPanelScreen(null, TITLE);
        try {
            screen.init(width, height);
            screen.warmFrame(graphics);
        } finally {
            screen.removed();
        }
    }
}
