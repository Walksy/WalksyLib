package main.walksy.lib.core.gui.widget.options.category;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;

final class CategoryListView {
    static final float ROW_HEIGHT = 22f;
    static final float TEXT_INSET = 10f;
    static final float PADDING_Y = 5f;
    private static final float HOVER_SPEED = 16f;
    private static final String WIDTH_REFERENCE = "Colors & Assets";
    private static final float MARQUEE_STEP = 0.22f;
    private static final int MARQUEE_DWELL = 5;
    private static final float SCROLL_BAR_W = 1.5f;
    private static final float SCROLL_BAR_INSET = 1.5f;
    private static final float SCROLL_BAR_END_INSET = 12f;
    private static final float DIM_ALPHA = 0.72f;
    private static final float ROW_INSET = 4f;
    private static final float ROW_CORNER_RATIO = 0.28f;
    private final List<Category> categories;
    private final Supplier<Category> selectedSupplier;
    private final Consumer<Category> onSelect;
    private final float uiScale;
    private final float[] hoverAnim;
    private final boolean scrollable;
    private float marqueeClock;

    CategoryListView(final List<Category> categories, final Supplier<Category> selectedSupplier, final Consumer<Category> onSelect, final float uiScale) {
        this(categories, selectedSupplier, onSelect, uiScale, false);
    }

    CategoryListView(final List<Category> categories, final Supplier<Category> selectedSupplier, final Consumer<Category> onSelect, final float uiScale, final boolean scrollable) {
        this.categories = categories;
        this.selectedSupplier = selectedSupplier;
        this.onSelect = onSelect;
        this.uiScale = uiScale;
        this.scrollable = scrollable;
        this.hoverAnim = new float[categories.size()];
    }

    float rowHeight() {
        return ROW_HEIGHT * this.uiScale;
    }

    float textSize() {
        return OptionRowWidget.LABEL_SIZE * this.uiScale;
    }

    float preferredWidth() {
        float widest = 0f;
        for (final Category category : this.categories) {
            widest = Math.max(widest, Fonts.SFPRO_DISPLAY.width(category.name(), this.textSize()));
        }
        return Math.min(widest, Fonts.SFPRO_DISPLAY.width(WIDTH_REFERENCE, this.textSize())) + TEXT_INSET * this.uiScale * 2f;
    }

    float preferredHeight() {
        return this.rowHeight() * this.categories.size() + PADDING_Y * this.uiScale * 2f;
    }

    float maxScroll(final float h) {
        return Math.max(0f, this.preferredHeight() - h);
    }

    void extract(final WLGraphics2D graphics, final float x, final float y, final float w, final int mouseX, final int mouseY, final float dtSeconds, final float reveal) {
        this.extract(graphics, x, y, w, this.preferredHeight(), mouseX, mouseY, dtSeconds, reveal, 0f);
    }

    void extract(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float dtSeconds, final float reveal) {
        this.extract(graphics, x, y, w, h, mouseX, mouseY, dtSeconds, reveal, 0f);
    }

    void extract(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float dtSeconds, final float reveal, final float scrollY) {
        if (dtSeconds > 0f && Float.isFinite(dtSeconds)) {
            this.marqueeClock += dtSeconds;
        }
        float limit = y + h - PADDING_Y * this.uiScale;
        final float rowH = this.rowHeight();
        final float size = this.textSize();
        final float capHeight = size * 0.59043f;
        final Category selected = this.selectedSupplier == null ? null : this.selectedSupplier.get();
        float top = y + PADDING_Y * this.uiScale;
        final float inset = ROW_INSET * this.uiScale;
        final int fits = (int) Math.floor((h - PADDING_Y * this.uiScale * 2f) / rowH);
        final boolean truncated = !this.scrollable && fits < this.categories.size();
        final int shown = truncated ? Math.max(0, fits - 1) : this.scrollable ? this.categories.size() : Math.min(fits, this.categories.size());
        top -= scrollY;
        if (this.scrollable) {
            limit = Float.MAX_VALUE;
        }
        for (int i = 0; i < shown; i++) {
            final Category category = this.categories.get(i);
            final float ry0 = top + i * rowH;
            if (ry0 + rowH > limit) {
                break;
            }
            if (this.scrollable && (ry0 + rowH < y || ry0 > y + h)) {
                continue;
            }
            final boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= ry0 && mouseY < ry0 + rowH;
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            this.hoverAnim[i] = MathUtils.approach(this.hoverAnim[i], hovered ? 1f : 0f, HOVER_SPEED, dtSeconds);
            final boolean current = category.equals(selected);
            final float fillStrength = current ? 1f : this.hoverAnim[i];
            if (fillStrength > 0.001f) {
                final int base = current ? Theme.get().selectedRowArgb() : Theme.get().hoverFillArgb();
                final int fill = ColorUtils.withAlpha(base, reveal * fillStrength);
                graphics.fillRoundedGradient(x + inset, ry0, x + w - inset, ry0 + rowH, fill, fill, fill, fill, rowH * ROW_CORNER_RATIO);
            }
            final float baseline = ry0 + rowH / 2f + capHeight * 0.5f;
            final float brightness = Math.max(this.hoverAnim[i], current ? 1f : 0f);
            final float colorAlpha = DIM_ALPHA + (1f - DIM_ALPHA) * brightness;
            final var font = current ? Fonts.SFPRO_DISPLAY_MEDIUM : Fonts.SFPRO_DISPLAY;
            final String label = this.marquee(category.name(), w - TEXT_INSET * this.uiScale * 2f, font, size);
            font.extract(graphics, label, x + TEXT_INSET * this.uiScale, font.topForBaseline(baseline, size), size, ColorUtils.withAlpha(Theme.get().label(), reveal * colorAlpha));
        }
        if (truncated) {
            final int hidden = this.categories.size() - shown;
            final float ry0 = top + shown * rowH;
            final float noticeSize = this.textSize() * 0.9f;
            final String text = hidden + " more";
            final float baseline = ry0 + rowH / 2f + noticeSize * 0.59043f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, text, x + TEXT_INSET * this.uiScale, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, noticeSize), noticeSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), reveal));
        }
    }

    private String marquee(final String text, final float maxWidth, final MsdfFont font, final float size) {
        if (text == null || text.isEmpty() || font.width(text, size) <= maxWidth) {
            return text;
        }
        int fit = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (font.width(text.substring(0, i), size) > maxWidth) {
                break;
            }
            fit = i;
        }
        if (fit <= 0) {
            return text.substring(0, 1);
        }
        final int steps = text.length() - fit + 1;
        if (steps <= 1) {
            return text;
        }
        final int span = steps - 1;
        final int cycle = (span + MARQUEE_DWELL) * 2;
        final int tick = (int) (this.marqueeClock / MARQUEE_STEP) % cycle;
        int pos;
        if (tick < MARQUEE_DWELL) {
            pos = 0;
        } else if (tick < MARQUEE_DWELL + span) {
            pos = tick - MARQUEE_DWELL;
        } else if (tick < MARQUEE_DWELL * 2 + span) {
            pos = span;
        } else {
            pos = span - (tick - (MARQUEE_DWELL * 2 + span));
        }
        pos = Math.max(0, Math.min(span, pos));
        return text.substring(pos, pos + fit);
    }

    void extractScrollIndicator(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float scrollY, final float alpha) {
        final float max = this.maxScroll(h);
        if (max <= 0f || alpha <= 0.01f) {
            return;
        }
        final float trackTop = y + SCROLL_BAR_END_INSET * this.uiScale;
        final float trackH = h - SCROLL_BAR_END_INSET * this.uiScale * 2f;
        if (trackH <= 0f) {
            return;
        }
        final float thumbH = Math.max(ROW_HEIGHT * this.uiScale * 0.5f, trackH * (trackH / (trackH + max)));
        final float t = Math.max(0f, Math.min(1f, scrollY / max));
        final float thumbY = trackTop + (trackH - thumbH) * t;
        final float barW = SCROLL_BAR_W * this.uiScale;
        final float x0 = x + w - SCROLL_BAR_INSET * this.uiScale - barW;
        final int color = ColorUtils.withAlpha(Theme.get().label(), 0.28f * alpha);
        graphics.fillRoundedGradient(x0, thumbY, x0 + barW, thumbY + thumbH, color, color, color, color, barW / 2f);
    }

    boolean click(final double mouseY, final float y) {
        return this.click(mouseY, y, this.preferredHeight());
    }

    boolean click(final double mouseY, final float y, final float h) {
        return this.click(mouseY, y, h, 0f);
    }

    boolean click(final double mouseY, final float y, final float h, final float scrollY) {
        final int row = (int) ((mouseY + scrollY - (y + PADDING_Y * this.uiScale)) / this.rowHeight());
        final int fits = (int) Math.floor((h - PADDING_Y * this.uiScale * 2f) / this.rowHeight());
        final int shown = this.scrollable ? this.categories.size() : fits < this.categories.size() ? Math.max(0, fits - 1) : this.categories.size();
        if (row < 0 || row >= shown) {
            return false;
        }
        if (this.scrollable && (mouseY < y || mouseY >= y + h)) {
            return false;
        }
        if (this.onSelect != null) {
            this.onSelect.accept(this.categories.get(row));
        }
        return true;
    }
}
