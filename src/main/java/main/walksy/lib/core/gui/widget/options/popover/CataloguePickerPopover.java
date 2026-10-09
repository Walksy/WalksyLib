package main.walksy.lib.core.gui.widget.options.popover;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.PopoverStyle;
import main.walksy.lib.api.config.value.ListIcon;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.texture.ItemModels;
import main.walksy.lib.core.render.texture.ListIcons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.ScrollGlide;

public final class CataloguePickerPopover implements PopoverHost.PopoverContent {
    private static final float PAD = 14f;
    private static final float SEARCH_H = WalksyLibPanelScreen.SEARCH_BAR_HEIGHT;
    private static final float ROW_H = 20f;
    public static final float DEFAULT_WIDTH = 220f;
    public static final float DEFAULT_HEIGHT = 260f;
    private static final float SCROLL_STEP = 34f;
    private static final float ROW_HOVER_SPEED = 18f;
    private static final float SCROLL_GLIDE_SPEED = 18f;
    private static final float SCROLLBAR_W = 4f;
    private static final float SCROLLBAR_GRAB = 4f;
    private static final float SCROLLBAR_GAP = 5f;
    private static final int SECTIONS = 2;
    private static final float TOP_FADE_CURVE = 0.55f;
    private static final float EMPTY_TEXT_SIZE = Type.FOOTNOTE.size();
    private final String title;
    private final Entries entries;
    private final Predicate<String> selected;
    private final Consumer<String> onPick;
    private final boolean closeOnPick;
    private final Runnable requestClose;
    private final Runnable onClosedCallback;
    private final Supplier<GlassStyle> originStyle;
    private final float uiScale;
    private final int panelLayer;
    private final float width;
    private final float height;
    private final SearchBar search = new SearchBar(0, 0, 1, 1, q -> { }).deepLens();
    private List<String> filtered = List.of();
    private String appliedQuery;
    private int appliedSourceSize = -1;
    private float scrollTarget;
    private final ScrollGlide glide = new ScrollGlide();
    private float scrollY;
    private float hoverAnim;
    private int hoverRow = -1;
    private boolean draggingThumb;
    private float thumbGrabOffset;
    private float lastListTop, lastListHeight, lastThumbX;
    private BiConsumer<WLGraphics2D, Float> triggerHandoff;
    private boolean handoffRides = true;

    public interface Entries {

        List<String> values();

        String label(String value);

        @Nullable
        default ListIcon icon(final String value) {
            return null;
        }

        @Nullable
        default String itemId(final String value) {
            return null;
        }

        @Nullable
        default String trailing(final String value) {
            return null;
        }

        default String emptyMessage() {
            return "Nothing to choose from";
        }
    }

    public CataloguePickerPopover(final String title, final Entries entries, final Predicate<String> selected, final Consumer<String> onPick, final boolean closeOnPick, final Runnable requestClose, final Runnable onClosedCallback, final Supplier<GlassStyle> originStyle, final float uiScale, final int panelLayer) {
        this(title, entries, selected, onPick, closeOnPick, requestClose, onClosedCallback, originStyle, uiScale, panelLayer, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public CataloguePickerPopover(final String title, final Entries entries, final Predicate<String> selected, final Consumer<String> onPick, final boolean closeOnPick, final Runnable requestClose, final Runnable onClosedCallback, final Supplier<GlassStyle> originStyle, final float uiScale, final int panelLayer, final float width, final float height) {
        this.title = title;
        this.entries = entries;
        this.selected = selected;
        this.onPick = onPick;
        this.closeOnPick = closeOnPick;
        this.requestClose = requestClose;
        this.onClosedCallback = onClosedCallback;
        this.originStyle = originStyle;
        this.uiScale = uiScale;
        this.panelLayer = panelLayer;
        this.width = width;
        this.height = height;
        this.search.style().layer(this.contentLayer());
        this.search.setFocused(true);
    }

    private int contentLayer() {
        return this.panelLayer + 1;
    }

    private float s() {
        return this.uiScale;
    }

    @Override
    public int preferredWidth() {
        return Math.round(this.width * this.s());
    }

    @Override
    public int preferredHeight() {
        return Math.round(this.height * this.s());
    }

    @Override
    public GlassStyle originStyle() {
        return this.originStyle == null ? null : this.originStyle.get();
    }

    @Override
    public boolean bridged() {
        return false;
    }

    @Override
    public void onClosed() {
        if (this.onClosedCallback != null) {
            this.onClosedCallback.run();
        }
    }

    public CataloguePickerPopover withTriggerHandoff(final BiConsumer<WLGraphics2D, Float> drawer) {
        this.triggerHandoff = drawer;
        return this;
    }

    public CataloguePickerPopover withFixedTriggerHandoff(final BiConsumer<WLGraphics2D, Float> drawer) {
        this.triggerHandoff = drawer;
        this.handoffRides = false;
        return this;
    }

    @Override
    public boolean handoffRides() {
        return this.handoffRides;
    }

    @Override
    public boolean hasTriggerHandoff() {
        return this.triggerHandoff != null;
    }

    @Override
    public void extractTriggerHandoff(final WLGraphics2D graphics, final float handoff) {
        if (this.triggerHandoff != null) {
            this.triggerHandoff.accept(graphics, handoff);
        }
    }

    private void refilter() {
        final String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
        final List<String> all = this.entries.values();
        final boolean queryChanged = !query.equals(this.appliedQuery);
        if (!queryChanged && all.size() == this.appliedSourceSize) {
            return;
        }
        this.appliedQuery = query;
        this.appliedSourceSize = all.size();
        this.hoverRow = -1;
        if (query.isEmpty()) {
            this.filtered = all;
        } else {
            final List<String> matches = new ArrayList<>();
            for (final String value : all) {
                if (this.entries.label(value).toLowerCase(Locale.ROOT).contains(query) || value.toLowerCase(Locale.ROOT).contains(query)) {
                    matches.add(value);
                }
            }
            this.filtered = matches;
        }
        if (queryChanged) {
            this.scrollY = 0f;
            this.scrollTarget = 0f;
        }
    }

    private PopoverStyle style;

    public CataloguePickerPopover style(final PopoverStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public PopoverStyle style() {
        return this.style == null ? PopoverStyle.getDefault() : this.style;
    }

    private float titleTop(final int panelY) {
        final float titleSize = OptionRowWidget.LABEL_SIZE * this.s();
        return Fonts.SFPRO_DISPLAY.topForBaseline(panelY + PAD * this.s() + titleSize * 0.59043f, titleSize);
    }

    private float searchY(final int panelY) {
        return this.titleTop(panelY) + OptionRowWidget.LABEL_SIZE * this.s() + 8f * this.s();
    }

    private float listTop(final int panelY) {
        return this.searchY(panelY) + SEARCH_H * this.s() + 8f * this.s();
    }

    private float listBottom(final int panelY, final int panelH) {
        return panelY + panelH - PAD * this.s();
    }

    private float clipBottom(final int panelY, final int panelH) {
        return panelY + panelH;
    }

    private float dissolveTop(final int panelY) {
        return this.titleTop(panelY) + OptionRowWidget.LABEL_SIZE * this.s();
    }

    private float fadeBand(final int panelY) {
        return Math.max(0f, this.listTop(panelY) - this.dissolveTop(panelY));
    }

    private float dissolve(final float leaving, final float fadeBand) {
        final float linear = fadeBand <= 0f ? 1f : Mth.clamp(leaving / fadeBand, 0f, 1f);
        return linear >= 1f ? 1f : (float) Math.pow(linear, TOP_FADE_CURVE);
    }

    private float maxScroll(final int panelY, final int panelH) {
        return Math.max(0f, this.filtered.size() * ROW_H * this.s() - (this.listBottom(panelY, panelH) - this.listTop(panelY)));
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
        this.refilter();
        final float s = this.s();
        final float cx = panelX + panelW / 2f;
        final float x0 = panelX + PAD * s;
        final float x1 = panelX + panelW - PAD * s;
        final float dtSeconds = delta / 20f;
        final var pose = graphics.pose();
        final float titleSize = OptionRowWidget.LABEL_SIZE * s;
        final float titleTop = this.titleTop(panelY);
        float a = Materialize.enter(pose, Materialize.stagger(reveal, 0, SECTIONS), cx, titleTop + titleSize / 2f, s);
        Fonts.SFPRO_DISPLAY.extract(graphics, this.title, x0, titleTop, titleSize, ColorUtils.withAlpha(this.style().title(), a));
        Materialize.exit(pose);
        final float searchY = this.searchY(panelY);
        a = Materialize.enter(pose, Materialize.stagger(reveal, 1, SECTIONS), cx, searchY + SEARCH_H * s / 2f, s);
        this.extractSearch(graphics, x0, searchY, x1 - x0, SEARCH_H * s, mouseX, mouseY, delta, a);
        Materialize.exit(pose);
        this.extractList(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, dtSeconds, reveal);
    }

    private void extractSearch(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float delta, final float alpha) {
        this.search.setX(Math.round(x));
        this.search.setY(Math.round(y));
        this.search.setWidth(Math.max(1, Math.round(w)));
        this.search.setHeight(Math.max(1, Math.round(h)));
        this.search.setCornerRadius(0.5f * Math.min(w, h));
        this.search.setTextSize(Mth.clamp(h * 0.50f, Type.CAPTION1.size() * this.s(), Type.TITLE3.size() * this.s()));
        this.search.active = true;
        this.search.setAlpha(alpha);
        graphics.widget(this.search, mouseX, mouseY, delta);
    }

    private void extractList(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float dtSeconds, final float reveal) {
        final float s = this.s();
        final float restY = this.listTop(panelY);
        final float clipTop = this.dissolveTop(panelY);
        final float bottom = this.listBottom(panelY, panelH);
        if (bottom - restY <= 1f) {
            return;
        }
        final float max = this.maxScroll(panelY, panelH);
        this.scrollTarget = Mth.clamp(this.scrollTarget, 0f, max);
        this.scrollY = this.draggingThumb ? this.scrollTarget : Mth.clamp(this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dtSeconds), 0f, max);
        this.lastListTop = restY;
        this.lastListHeight = bottom - restY;
        this.lastThumbX = panelX + panelW - PAD * s + SCROLLBAR_GAP * s;
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().beginPopover(this.panelLayer);
        try {
            if (this.filtered.isEmpty()) {
                this.extractEmptyState(graphics, panelX, panelW, restY, bottom, reveal);
                return;
            }
            final float rowH = ROW_H * s;
            final float shown = ScrollGlide.shown(this.scrollY);
            final float x0 = panelX + PAD * s;
            final float x1 = panelX + panelW - PAD * s;
            final float fadeBand = this.fadeBand(panelY);
            final float clip = this.clipBottom(panelY, panelH);
            graphics.enableScissor(Math.round(x0), Math.round(clipTop), Math.round(x1), Math.round(clip));
            try {
                final int first = Math.max(0, (int) Math.floor((shown - (restY - clipTop)) / rowH));
                final int last = Math.min(this.filtered.size() - 1, (int) Math.ceil((shown + (clip - restY)) / rowH));
                final boolean pointerInList = mouseX >= x0 && mouseX < x1 && mouseY >= restY && mouseY < clip;
                int hovered = -1;
                if (pointerInList) {
                    final int row = (int) Math.floor((mouseY - restY + shown) / rowH);
                    if (row >= first && row <= last) {
                        hovered = row;
                    }
                }
                if (hovered >= 0) {
                    this.hoverRow = hovered;
                }
                WLRenderSystem.cursors().requestIf(hovered >= 0, Cursors.Cursor.POINTER);
                this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered >= 0 ? 1f : 0f, ROW_HOVER_SPEED, dtSeconds);
                for (int i = first; i <= last; i++) {
                    final String value = this.filtered.get(i);
                    final float ry = restY + i * rowH - shown;
                    final float fade = this.dissolve((ry + rowH / 2f) - clipTop, fadeBand);
                    if (fade <= 0f) {
                        continue;
                    }
                    this.extractRow(graphics, value, x0, ry, x1 - x0, rowH, reveal * fade, i == this.hoverRow);
                }
            } finally {
                graphics.disableScissor();
            }
            this.extractScrollbar(graphics, restY, bottom - restY, reveal);
        } finally {
            WLRenderSystem.contentStratum().endPopover();
            WLRenderSystem.markPopoverText(textMark);
        }
    }

    private void extractEmptyState(final WLGraphics2D graphics, final int panelX, final int panelW, final float top, final float bottom, final float reveal) {
        final float size = EMPTY_TEXT_SIZE * this.s();
        final String text = this.entries.values().isEmpty() ? this.entries.emptyMessage() : "No matches";
        final float w = Fonts.SFPRO_DISPLAY.width(text, size);
        final float baseline = top + (bottom - top) / 2f + size * 0.59043f * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, text, panelX + (panelW - w) / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(this.style().emptyMessage(), reveal));
    }

    private void extractRow(final WLGraphics2D graphics, final String value, final float x, final float y, final float w, final float h, final float reveal, final boolean hovered) {
        final float s = this.s();
        final boolean isSelected = this.selected != null && this.selected.test(value);
        final float radius = h * 0.28f;
        if (isSelected) {
            final int fill = ColorUtils.withAlpha(this.style().selectedRowFill(), reveal);
            graphics.fillRoundedGradient(x, y, x + w, y + h, fill, fill, fill, fill, radius);
        } else if (hovered) {
            final int fill = ColorUtils.withAlpha(this.style().rowHoverFill(), reveal * this.hoverAnim);
            graphics.fillRoundedGradient(x, y, x + w, y + h, fill, fill, fill, fill, radius);
        }
        float textX = x + 6f * s;
        final String itemId = this.entries.itemId(value);
        final ListIcon icon = this.entries.icon(value);
        if (itemId != null || icon != null) {
            final float iconSize = h * 0.7f;
            final float iconY = y + (h - iconSize) / 2f;
            if (itemId == null || !ItemModels.extractPlain(graphics, itemId, textX, iconY, iconSize, this.panelLayer, reveal)) {
                ListIcons.extract(graphics, icon != null ? icon : ListIcons.forItem(itemId), textX, iconY, iconSize, reveal);
            }
            textX += iconSize + 5f * s;
        }
        final float size = Math.min(OptionRowWidget.LABEL_SIZE * s, h * 0.55f);
        final float markSize = size * 0.75f;
        float rightEdge = x + w - 6f * s - markSize - 5f * s;
        final String trailing = this.entries.trailing(value);
        if (trailing != null && !trailing.isEmpty()) {
            final float trailingSize = size * 0.85f;
            final float trailingW = Fonts.SFPRO_DISPLAY.width(trailing, trailingSize);
            final float baseline = y + h / 2f + trailingSize * 0.7f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, trailing, rightEdge - trailingW, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, trailingSize), trailingSize, ColorUtils.withAlpha(this.style().tertiaryLabel(), reveal));
            rightEdge -= trailingW + 6f * s;
        }
        final String label = Fonts.SFPRO_DISPLAY.truncate(this.entries.label(value), size, Math.max(4f, rightEdge - textX));
        final float baseline = y + h / 2f + size * 0.7f * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, label, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(isSelected ? this.style().selectedLabel() : this.style().label(), reveal));
        final Icons.Icon mark = isSelected && !this.closeOnPick ? Icons.MINUS : Icons.PLUS;
        mark.extract(graphics, x + w - 6f * s - markSize, y + (h - markSize) / 2f, markSize, ColorUtils.withAlpha(isSelected ? this.style().selectedLabel() : this.style().tertiaryLabel(), reveal));
    }

    private float thumbHeight(final float viewH) {
        final float contentH = this.filtered.size() * ROW_H * this.s();
        if (contentH <= viewH + 0.5f) {
            return 0f;
        }
        return Math.max(14f * this.s(), viewH * (viewH / contentH));
    }

    private float thumbTravel(final float viewH) {
        return Math.max(0f, viewH - this.thumbHeight(viewH));
    }

    private float lastMaxScroll() {
        return Math.max(0f, this.filtered.size() * ROW_H * this.s() - this.lastListHeight);
    }

    private float thumbY() {
        final float max = this.lastMaxScroll();
        final float t = max <= 0f ? 0f : Mth.clamp(this.scrollY / max, 0f, 1f);
        return this.lastListTop + this.thumbTravel(this.lastListHeight) * t;
    }

    private void extractScrollbar(final WLGraphics2D graphics, final float top, final float viewH, final float reveal) {
        final float thumbH = this.thumbHeight(viewH);
        if (thumbH <= 0f) {
            return;
        }
        final float barW = SCROLLBAR_W * this.s();
        final float x0 = this.lastThumbX;
        final float y = this.thumbY();
        final int color = ColorUtils.withAlpha(this.style().scrollbar(this.draggingThumb), reveal);
        graphics.fillRoundedGradient(x0, y, x0 + barW, y + thumbH, color, color, color, color, barW / 2f);
    }

    private boolean isOverThumb(final double mouseX, final double mouseY) {
        final float thumbH = this.thumbHeight(this.lastListHeight);
        if (thumbH <= 0f) {
            return false;
        }
        final float grab = SCROLLBAR_GRAB * this.s();
        final float y = this.thumbY();
        return mouseX >= this.lastThumbX - grab && mouseX <= this.lastThumbX + SCROLLBAR_W * this.s() + grab && mouseY >= y && mouseY < y + thumbH;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (this.isOverThumb(event.x(), event.y())) {
            this.draggingThumb = true;
            this.thumbGrabOffset = (float) event.y() - this.thumbY();
            return true;
        }
        if (this.search.isMouseOver(event.x(), event.y())) {
            this.search.setFocused(true);
            this.search.onClick(event, doubleClick);
            return true;
        }
        final float s = this.s();
        final float top = this.listTop(panelY);
        final float bottom = this.clipBottom(panelY, panelH);
        final float x0 = panelX + PAD * s;
        final float x1 = panelX + panelW - PAD * s;
        if (event.x() < x0 || event.x() >= x1 || event.y() < top || event.y() >= bottom) {
            return true;
        }
        final int index = (int) Math.floor((event.y() - top + this.scrollY) / (ROW_H * s));
        if (index < 0 || index >= this.filtered.size()) {
            return true;
        }
        this.onPick.accept(this.filtered.get(index));
        if (this.closeOnPick && this.requestClose != null) {
            this.requestClose.run();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (this.draggingThumb) {
            final float travel = this.thumbTravel(this.lastListHeight);
            final float max = this.lastMaxScroll();
            final float t = travel <= 0f ? 0f : ((float) event.y() - this.thumbGrabOffset - this.lastListTop) / travel;
            this.scrollTarget = Mth.clamp(t * max, 0f, max);
            this.scrollY = this.scrollTarget;
            return true;
        }
        return this.search.isFocused() && this.search.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        final boolean was = this.draggingThumb;
        this.draggingThumb = false;
        return was;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
        this.scrollTarget = Mth.clamp(this.scrollTarget - (float) scrollY * SCROLL_STEP * this.s(), 0f, this.maxScroll(panelY, panelH));
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.search.isFocused() && this.search.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.search.isFocused() && this.search.charTyped(event);
    }
}
