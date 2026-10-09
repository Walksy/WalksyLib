package main.walksy.lib.core.toast;

import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;

import java.util.List;

public final class ToastRenderer {
    private static final float MARGIN = 9f;
    private static final float GAP = 6f;
    private static final float PANEL_DROP = 6f;

    private ToastRenderer() {
    }

    public static void render(final WLGraphics2D graphics, final float topInset) {
        if (Fonts.SFPRO_DISPLAY == null || Fonts.SFPRO_DISPLAY_BOLD == null) {
            return;
        }
        if (WLClient.overlay() != null) {
            return;
        }
        final WLToastManager toasts = WalksyLib.getInstance().getToastManager();
        final List<ToastCard> cards = toasts.beginFrame();
        if (!cards.isEmpty()) {
            extractOnTop(graphics, cards, topInset);
        }
        toasts.endFrame();
    }

    private static void extractOnTop(final WLGraphics2D graphics, final List<ToastCard> cards, final float topInset) {
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().beginTop();
        try {
            extract(graphics, cards, topInset);
        } finally {
            WLRenderSystem.contentStratum().endTop();
            WLRenderSystem.markTopText(textMark);
        }
    }

    private static void extract(final WLGraphics2D graphics, final List<ToastCard> cards, final float topInset) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final float scale = WLScreen.uiScaleFactor();
        final float margin = MARGIN * scale;
        final float gap = GAP * scale;
        final float windowWidth = graphics.guiWidth();
        final float available = Math.max(1f, windowWidth - margin * 2f);
        final ToastCorner corner = WalksyLib.getInstance().getAppearancePage().toastCorner();
        final float panelDrop = WLClient.screen() instanceof WalksyLibPanelScreen ? PANEL_DROP * scale : 0f;
        float y = (corner.isRight() ? topInset : 0f) + margin + panelDrop;
        for (final ToastCard card : cards) {
            card.measure(scale, available);
            card.update(dt, y);
            y += (card.height() + gap) * card.occupancy();
        }
        for (final ToastCard card : cards) {
            card.extract(graphics, windowWidth, margin, corner);
        }
    }
}
