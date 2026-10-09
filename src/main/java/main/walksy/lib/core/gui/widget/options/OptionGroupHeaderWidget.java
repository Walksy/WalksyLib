package main.walksy.lib.core.gui.widget.options;
import main.walksy.lib.core.gui.style.Type;

import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

public class OptionGroupHeaderWidget extends WLWidget implements ScrollPanel.ExtendedPaint {
    private static final float TEXT_SIZE = Type.HEADLINE.size();
    private static final float CARD_RADIUS = 6f;
    private static final float TITLE_GAP = 3f;
    private final String title;
    private final float uiScale;
    private final List<AbstractWidget> rows = new ArrayList<>();
    private float headerAlpha = 1f;
    private float bandTop;
    private float bandHeight;

    private void extractCard(final WLGraphics2D graphics, final float top, final float bottom, final int fill, final float r) {
        final float x0 = this.getX();
        final float x1 = this.getX() + this.getWidth();
        final float bandBottom = this.bandTop + this.bandHeight;
        if (this.bandHeight <= 0f || top >= bandBottom) {
            graphics.fillRoundedGradient(x0, top, x1, bottom, fill, fill, fill, fill, r);
            return;
        }
        final int bandBottomEdge = screenEdge(bandBottom);
        if (bottom > bandBottom) {
            graphics.enableScissor(Math.round(x0), bandBottomEdge, Math.round(x1), (int) Math.ceil(bottom) + 2);
            graphics.fillRoundedGradient(x0, top, x1, bottom, fill, fill, fill, fill, r);
            graphics.disableScissor();
        }
        final float rampTop = Math.max(top, this.bandTop);
        final float rampBottom = Math.min(bottom, bandBottom + r);
        if (rampBottom <= rampTop) {
            return;
        }
        final int topFill = scaleAlpha(fill, this.bandRamp(rampTop));
        final int botFill = scaleAlpha(fill, this.bandRamp(rampBottom));
        final float rampRadius = top >= this.bandTop ? r : 0f;
        final int rampTopEdge = top >= this.bandTop ? (int) Math.floor(top) - 2 : screenEdge(this.bandTop);
        graphics.enableScissor(Math.round(x0), rampTopEdge, Math.round(x1), bandBottomEdge);
        graphics.fillRoundedGradient(x0, rampTop, x1, rampBottom, topFill, topFill, botFill, botFill, rampRadius);
        graphics.disableScissor();
    }

    private static int screenEdge(final float localY) {
        return (int) Math.ceil(localY);
    }

    public OptionGroupHeaderWidget(final OptionGroup group, final int x, final int y, final int width, final int height) {
        this(group.getName(), x, y, width, height);
    }

    public OptionGroupHeaderWidget(final String title, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.title = title;
        this.uiScale = WLScreen.uiScaleFactor();
    }

    public void addRow(final AbstractWidget row) {
        this.rows.add(row);
    }

    @Override
    public float paintBottom() {
        float bottom = this.getY() + this.getHeight();
        for (final AbstractWidget row : this.rows) {
            bottom = Math.max(bottom, row.getY() + row.getHeight());
        }
        return bottom;
    }

    @Override
    public void setHeaderAlpha(final float alpha) {
        this.headerAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    @Override
    public void setDissolveBand(final float top, final float height) {
        this.bandTop = top;
        this.bandHeight = Math.max(0f, height);
    }

    private float bandRamp(final float y) {
        if (this.bandHeight <= 0f) {
            return 1f;
        }
        return Math.max(0f, (y - this.bandTop) / this.bandHeight);
    }

    private static int scaleAlpha(final int argb, final float factor) {
        final int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, factor));
        return (Math.min(255, a) << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        if (!this.rows.isEmpty()) {
            float top = Float.MAX_VALUE;
            float bottom = -Float.MAX_VALUE;
            for (final AbstractWidget row : this.rows) {
                top = Math.min(top, row.getY());
                bottom = Math.max(bottom, row.getY() + row.getHeight());
            }
            if (bottom > top) {
                final int fill = ColorUtils.withAlpha(Theme.get().cardFillArgb(), alpha);
                final float r = CARD_RADIUS * this.uiScale;
                this.extractCard(graphics, top, bottom, fill, r);
                final int hairBase = ColorUtils.withAlpha(Theme.get().separator(), alpha);
                final float hair = WLScreen.hairlinePx();
                for (int i = 0; i < this.rows.size() - 1; i++) {
                    final AbstractWidget above = this.rows.get(i);
                    final float sy = above.getY() + above.getHeight();
                    final int hairColor = scaleAlpha(hairBase, Math.min(1f, this.bandRamp(sy)));
                    graphics.fill(this.getX(), sy, this.getX() + this.getWidth(), sy + hair, hairColor, null);
                }
            }
        }
        if (this.headerAlpha <= 0.001f) {
            return;
        }
        final float size = TEXT_SIZE * this.uiScale;
        final float capHeight = size * 0.59043f;
        final float baseline = this.getY() + (this.getHeight() - TITLE_GAP * this.uiScale) / 2f + capHeight * 0.5f;
        final float top = Fonts.SFPRO_DISPLAY_BOLD.topForBaseline(baseline, size);
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, this.title, this.getX(), top, size, ColorUtils.withAlpha(Theme.get().label(), this.headerAlpha));
    }
}
