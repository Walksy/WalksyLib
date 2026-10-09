package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;

public class HiddenOptionsWidget extends WLWidget {
    private static final float TEXT_SIZE = Type.FOOTNOTE.size();
    private static final float ICON_RATIO = 1.05f;
    private static final float ICON_GAP = 5f;
    private static final float TOP_INSET = 4f;
    private static final float CAP_HEIGHT_RATIO = 0.59043f;
    private final String title;
    private final String detail;
    private final float uiScale = WLScreen.uiScaleFactor();

    public HiddenOptionsWidget(final String title, final String detail, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.title = title;
        this.detail = detail;
    }

    public static int preferredHeight() {
        final float scale = WLScreen.uiScaleFactor();
        return Math.round((TOP_INSET * 2f + TEXT_SIZE * ICON_RATIO) * scale);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float size = TEXT_SIZE * this.uiScale;
        final float iconSize = size * ICON_RATIO;
        final float cy = this.getY() + this.getHeight() / 2f;
        final float x = this.getX();
        Icons.INFO.extractCentered(graphics, x + iconSize / 2f, cy, iconSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha), 0f);
        final float textX = x + iconSize + ICON_GAP * this.uiScale;
        final float top = Fonts.SFPRO_DISPLAY.topForBaseline(cy + size * CAP_HEIGHT_RATIO * 0.5f, size);
        Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, this.title, textX, top, size, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
        if (this.detail == null || this.detail.isEmpty()) {
            return;
        }
        final float detailX = textX + Fonts.SFPRO_DISPLAY_MEDIUM.width(this.title, size) + ICON_GAP * this.uiScale;
        final float room = this.getX() + this.getWidth() - detailX;
        if (room <= 0f) {
            return;
        }
        Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(this.detail, size, room), detailX, top, size, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
    }
}
