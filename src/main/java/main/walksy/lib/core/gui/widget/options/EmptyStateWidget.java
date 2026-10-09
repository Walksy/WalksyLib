package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;

public class EmptyStateWidget extends WLWidget {
    private static final float TITLE_SIZE = Type.HEADLINE.size();
    private static final float BODY_SIZE = Type.SUBHEADLINE.size();
    private static final float LINE_GAP = 6f;
    private static final float TOP_INSET = 28f;
    private final String title;
    private final String detail;
    private final float uiScale = WLScreen.uiScaleFactor();

    public EmptyStateWidget(final String title, final String detail, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.title = title;
        this.detail = detail;
    }

    public static int preferredHeight() {
        final float scale = WLScreen.uiScaleFactor();
        return Math.round((TOP_INSET + TITLE_SIZE + LINE_GAP + BODY_SIZE + TOP_INSET) * scale);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float titleSize = TITLE_SIZE * this.uiScale;
        final float bodySize = BODY_SIZE * this.uiScale;
        final float cx = this.getX() + this.getWidth() / 2f;
        final float top = this.getY() + TOP_INSET * this.uiScale;
        final float titleWidth = Fonts.SFPRO_DISPLAY_BOLD.width(this.title, titleSize);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, this.title, cx - titleWidth / 2f, top, titleSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        if (this.detail == null || this.detail.isEmpty()) {
            return;
        }
        final float detailTop = top + titleSize + LINE_GAP * this.uiScale;
        final String detailText = Fonts.SFPRO_DISPLAY.truncate(this.detail, bodySize, this.getWidth());
        final float detailWidth = Fonts.SFPRO_DISPLAY.width(detailText, bodySize);
        Fonts.SFPRO_DISPLAY.extract(graphics, detailText, cx - detailWidth / 2f, detailTop, bodySize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
    }
}
