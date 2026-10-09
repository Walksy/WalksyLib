package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.ListIcon;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.liquid.MorphProfile;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.texture.ListIcons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.ScrollGlide;
import net.minecraft.util.Mth;

public class StringListOptionWidget extends OptionRowWidget<List<String>> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x517157L);
    private static final float ENTRY_GAP = 4f;
    private static final float BUTTON_GAP = 6f;
    private static final float BOTTOM_PADDING = 4f;
    private static final float ICON_RATIO = 0.42f;
    private static final MorphProfile PICKER_MORPH = MorphProfile.apple().anchorResidue(0f).bridge(0f).closeSpeed(1.6f);
    private static final float EMPTY_TEXT_SIZE = Type.FOOTNOTE.size();
    private static final String EMPTY_TEXT = "No entries";
    private static final String ENTRY_PLACEHOLDER = "Empty";
    private static final float GRIP_WIDTH = 8f;
    private static final float GRIP_GAP = 5f;
    private static final int GRIP_BARS = 3;
    private static final float REORDER_SPEED = 20f;
    private static final float DRAG_FADE_SPEED = 16f;
    private static final float HOVER_SPEED = 14f;
    private static final float CHIP_CORNER_RATIO = 0.3f;
    private static final float CHIP_INSET = 5f;
    private static final float CHIP_ICON_GAP = 5f;
    private static final int DRAG_LAYER = ControlLayers.KNOB + 1;
    private final Runnable onHeightChanged;
    private final ListEntries catalogue;
    private final boolean reorderable;
    private final boolean allowDuplicates;
    private final List<Entry> entries = new ArrayList<>();
    private final WLButton addButton;
    private final GlassStyle addButtonStyle;
    private Entry dragEntry;
    private float dragGrabOffset;
    private float dragPointerY;
    private float dragFade;
    private boolean pickerActive;
    private float addScreenX;
    private float addScreenY;
    private float addScreenSize;
    private float addX;
    private float addY;
    private float addSize;

    public StringListOptionWidget(final Option<List<String>> option, final RowContext ctx, final Runnable onHeightChanged, final int x, final int y, final int width, final int rowHeight) {
        super(option, ctx, x, y, width, rowHeight);
        this.onHeightChanged = onHeightChanged;
        this.catalogue = option.getListEntries();
        this.reorderable = option.isReorderable();
        this.allowDuplicates = option.allowsDuplicates();
        this.addButton = WLButton.icon(0, 0, 1, 1, b -> this.onAdd());
        this.addButtonStyle = this.addButton.style();
        this.addButtonStyle.layer(ControlLayers.FILL).fadeKey(FADE_KEYS.incrementAndGet());
        if (this.isSelection()) {
            this.addButtonStyle.blurRadius(LiquidGlassConstants.BLUR_POPOVER);
        }
        this.rebuild(option.getValue());
        this.recomputeHeight();
    }

    private boolean isSelection() {
        return this.catalogue != null;
    }

    private float headerHeight() {
        return OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale;
    }

    private float entryHeight() {
        return this.headerHeight() * TextFieldControl.ROW_HEIGHT_RATIO;
    }

    private float entryStride() {
        return this.entryHeight() + ENTRY_GAP * this.uiScale;
    }

    private float contentLeft() {
        return this.getX() + PADDING * this.uiScale;
    }

    private float contentRight() {
        return this.getX() + this.getWidth() - PADDING * this.uiScale;
    }

    private float entryContentLeft() {
        return this.contentLeft() + (this.reorderable ? (GRIP_WIDTH + GRIP_GAP) * this.uiScale : 0f);
    }

    private float entryY(final int index) {
        return this.getY() + this.headerHeight() + index * this.entryStride();
    }

    private void recomputeHeight() {
        final int bands = Math.max(1, this.entries.size());
        final float total = this.headerHeight() + bands * this.entryHeight() + (bands - 1) * ENTRY_GAP * this.uiScale + BOTTOM_PADDING * this.uiScale;
        this.setHeight(Math.round(total));
    }

    private void onAdd() {
        if (this.isSelection()) {
            this.openPicker();
            return;
        }
        this.mutateEntries(() -> this.entries.add(new Entry("")));
        this.commit();
        this.afterStructureChanged();
    }

    private void removeEntry(final Entry entry) {
        if (!this.entries.contains(entry)) {
            return;
        }
        this.mutateEntries(() -> this.entries.remove(entry));
        this.commit();
        this.afterStructureChanged();
    }

    private void mutateEntries(final Runnable mutation) {
        final Map<Entry, Float> drawn = new IdentityHashMap<>();
        for (final Entry entry : this.entries) {
            drawn.put(entry, entry.drawnY);
        }
        mutation.run();
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            final Float was = drawn.get(entry);
            entry.slotOffset = was == null || Float.isNaN(was) ? 0f : was - this.entryY(i);
            entry.drawnY = this.entryY(i) + entry.slotOffset;
        }
    }

    private void toggleValue(final String value) {
        Entry existing = null;
        for (final Entry entry : this.entries) {
            if (entry.value().equals(value)) {
                existing = entry;
                break;
            }
        }
        if (existing != null && !this.allowDuplicates) {
            this.removeEntry(existing);
            return;
        }
        this.mutateEntries(() -> this.entries.add(new Entry(value)));
        this.commit();
        this.afterStructureChanged();
    }

    private boolean contains(final String value) {
        for (final Entry entry : this.entries) {
            if (entry.value().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void afterStructureChanged() {
        this.recomputeHeight();
        if (this.onHeightChanged != null) {
            this.onHeightChanged.run();
        }
    }

    private void commit() {
        final List<String> values = new ArrayList<>(this.entries.size());
        for (final Entry entry : this.entries) {
            final String raw = entry.value();
            values.add(entry.field != null ? this.option.cleanEntryName(raw) : raw);
        }
        this.option.setValue(values);
    }

    private void rebuild(final List<String> values) {
        this.entries.clear();
        if (values == null) {
            return;
        }
        for (final String value : values) {
            this.entries.add(new Entry(value));
        }
    }

    private void syncFromOption() {
        if (this.dragEntry != null || this.focusedEntry() != null) {
            return;
        }
        List<String> values = this.option.getValue();
        if (values == null) {
            values = List.of();
        }
        if (values.size() == this.entries.size()) {
            boolean same = true;
            for (int i = 0; i < values.size(); i++) {
                if (!this.entries.get(i).value().equals(values.get(i))) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return;
            }
        }
        this.rebuild(values);
        this.afterStructureChanged();
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.syncFromOption();
        final boolean hoverEnabled = available && !coveredByOverlay(mouseX, mouseY);
        final float size = this.entryHeight();
        this.addSize = size;
        this.addX = this.contentRight() - size;
        this.addY = this.getY() + (this.headerHeight() - size) / 2f;
        this.dragFade = MathUtils.approach(this.dragFade, this.dragEntry != null ? 1f : 0f, DRAG_FADE_SPEED, dt);
        final var pose = graphics.pose();
        final boolean posed = DevicePixels.isAxisAligned(pose);
        this.addScreenX = posed ? pose.m00() * Math.round(this.addX) + pose.m20() : Math.round(this.addX);
        this.addScreenY = posed ? pose.m11() * Math.round(this.addY) + pose.m21() : Math.round(this.addY);
        this.addScreenSize = posed ? pose.m11() * Math.round(size) : Math.round(size);
        if (!this.pickerActive) {
            this.extractIconButton(graphics, this.addButton, Icons.PLUS, this.addX, this.addY, size, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        } else {
            this.addButton.settleInteraction();
        }
        if (this.entries.isEmpty()) {
            final float textSize = EMPTY_TEXT_SIZE * this.uiScale;
            final float baseline = this.entryY(0) + size / 2f + textSize * 0.59043f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, EMPTY_TEXT, this.contentLeft(), Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
            return;
        }
        this.updateEntryPositions(dt);
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            if (entry != this.dragEntry) {
                entry.extract(graphics, mouseX, mouseY, dt, alpha, available, hoverEnabled);
            }
        }
        if (this.dragEntry != null) {
            this.dragEntry.extract(graphics, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        }
    }

    private void updateEntryPositions(final float dt) {
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            final float slot = this.entryY(i);
            if (entry == this.dragEntry) {
                final float pinned = Mth.clamp(this.dragPointerY - this.dragGrabOffset, this.entryY(0), this.entryY(this.entries.size() - 1));
                entry.slotOffset = pinned - slot;
            } else {
                entry.slotOffset = MathUtils.approach(entry.slotOffset, 0f, REORDER_SPEED, dt);
            }
            entry.drawnY = slot + entry.slotOffset;
        }
    }

    private void extractIconButton(final WLGraphics2D graphics, final WLButton button, final Icons.Icon icon, final float x, final float y, final float size, final int mouseX, final int mouseY, final float delta, final float alpha, final boolean available, final boolean hoverEnabled) {
        if (alpha <= 0.001f) {
            return;
        }
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.round(size));
        button.setHeight(Math.round(size));
        button.setCornerRadius(size / 2f);
        button.active = available;
        button.setAlpha(alpha);
        final int mx = hoverEnabled ? mouseX : TextFieldControl.CURSOR_AWAY;
        final int my = hoverEnabled ? mouseY : TextFieldControl.CURSOR_AWAY;
        graphics.widget(button, mx, my, delta);
        final float iconSize = size * ICON_RATIO;
        button.stretchContent(() -> icon.extract(graphics, button.getX() + (button.getWidth() - iconSize) / 2f, button.getY() + (button.getHeight() - iconSize) / 2f, iconSize, ColorUtils.withAlpha(Theme.get().label(), alpha)));
    }

    private void extractGrip(final WLGraphics2D graphics, final float x, final float y, final float height, final float alpha, final boolean hovered) {
        final float w = DevicePixels.snapSizeX(GRIP_WIDTH * this.uiScale);
        final float bar = DevicePixels.snapSizeY(this.uiScale);
        final float spacing = DevicePixels.snapSizeY(this.uiScale * 2f);
        final float total = bar + (GRIP_BARS - 1) * spacing;
        final float left = DevicePixels.snapX(x);
        final float top = DevicePixels.snapY(y + (height - total) / 2f);
        final int color = ColorUtils.withAlpha(hovered ? Theme.get().secondaryLabel() : Theme.get().tertiaryLabel(), alpha);
        for (int i = 0; i < GRIP_BARS; i++) {
            final float by = top + i * spacing;
            graphics.fill(left, by, left + w, by + bar, color, null);
        }
    }

    private void extractEntryIcon(final WLGraphics2D graphics, final ListIcon icon, final float x, final float y, final float size, final float alpha) {
        ListIcons.extract(graphics, icon, x, y, size, alpha);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive()) {
            return false;
        }
        if (!this.pickerActive && this.addButton.isMouseOver(event.x(), event.y())) {
            this.clearFieldFocus();
            return this.addButton.mouseClicked(event, doubleClick);
        }
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            if (this.reorderable && entry.isOverGrip(event.x(), event.y())) {
                this.clearFieldFocus();
                this.dragEntry = entry;
                this.dragPointerY = (float) event.y();
                this.dragGrabOffset = (float) event.y() - entry.drawnY;
                return true;
            }
            if (entry.isOverContent(event.x(), event.y())) {
                if (entry.field == null) {
                    this.clearFieldFocus();
                    return true;
                }
                this.focus(entry);
                entry.field.clicked(event, doubleClick);
                return true;
            }
            if (entry.removeButton.isMouseOver(event.x(), event.y())) {
                this.clearFieldFocus();
                return entry.removeButton.mouseClicked(event, doubleClick);
            }
        }
        this.clearFieldFocus();
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.dragEntry != null) {
            this.dragPointerY = (float) event.y();
            this.reorderToPointer();
            return true;
        }
        final Entry focused = this.focusedEntry();
        return focused != null && focused.field != null && focused.field.dragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.dragEntry != null) {
            this.dragEntry = null;
            this.commit();
            return true;
        }
        boolean handled = WLButton.release(this.addButton, event);
        for (final Entry entry : List.copyOf(this.entries)) {
            handled |= WLButton.release(entry.removeButton, event);
        }
        return handled;
    }

    private void reorderToPointer() {
        final int from = this.entries.indexOf(this.dragEntry);
        if (from < 0) {
            return;
        }
        final float centre = this.dragPointerY - this.dragGrabOffset + this.entryHeight() / 2f;
        int to = Math.round((centre - this.entryY(0) - this.entryHeight() / 2f) / this.entryStride());
        to = (int) Mth.clamp(to, 0f, this.entries.size() - 1f);
        if (to == from) {
            return;
        }
        final int target = to;
        this.mutateEntries(() -> {
            this.entries.remove(from);
            this.entries.add(target, this.dragEntry);
        });
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final Entry focused = this.focusedEntry();
        return focused != null && focused.field != null && focused.field.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        final Entry focused = this.focusedEntry();
        return focused != null && focused.field != null && focused.field.charTyped(event);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.clearFieldFocus();
        }
    }

    private void focus(final Entry target) {
        for (final Entry entry : this.entries) {
            if (entry.field != null) {
                entry.field.setFocused(entry == target);
            }
        }
    }

    private void clearFieldFocus() {
        this.focus(null);
    }

    private Entry focusedEntry() {
        for (final Entry entry : this.entries) {
            if (entry.field != null && entry.field.isFocused()) {
                return entry;
            }
        }
        return null;
    }

    private LiquidRect addScreenRect() {
        return new LiquidRect(this.addScreenX, this.addScreenY, this.addScreenSize, this.addScreenSize, this.addScreenSize / 2f);
    }

    private void openPicker() {
        if (this.ctx == null || this.ctx.onOpenCenteredPopover() == null) {
            return;
        }
        this.pickerActive = true;
        final LiquidRect anchor = this.addScreenRect();
        this.ctx.onOpenCenteredPopover().accept(anchor, new EntryPickerPopover());
    }

    private final class Entry {
        final TextFieldControl field;
        final String catalogueValue;
        final WLButton removeButton;
        private final GlassStyle style;
        private final int baseLayer;
        float slotOffset;
        float drawnY = Float.NaN;
        private float hoverAnim;
        private float chipX;
        private float chipW;

        Entry(final String initial) {
            this.style = SearchBar.defaultStyle().fadeKey(FADE_KEYS.incrementAndGet());
            this.baseLayer = this.style.getLayer();
            if (StringListOptionWidget.this.isSelection()) {
                this.field = null;
                this.catalogueValue = initial == null ? "" : initial;
            } else {
                this.catalogueValue = null;
                this.field = new TextFieldControl(initial, text -> StringListOptionWidget.this.commit(), ENTRY_PLACEHOLDER, this.style, StringListOptionWidget.this.uiScale);
            }
            this.removeButton = WLButton.icon(0, 0, 1, 1, b -> StringListOptionWidget.this.removeEntry(this));
            this.removeButton.style().layer(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
        }

        String value() {
            return this.field != null ? this.field.getText() : this.catalogueValue;
        }

        void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available, final boolean hoverEnabled) {
            final float h = StringListOptionWidget.this.entryHeight();
            final float y = this.drawnY;
            final float buttonX = StringListOptionWidget.this.contentRight() - h;
            this.chipX = StringListOptionWidget.this.entryContentLeft();
            this.chipW = Math.max(1f, buttonX - BUTTON_GAP * StringListOptionWidget.this.uiScale - this.chipX);
            final boolean dragged = this == StringListOptionWidget.this.dragEntry;
            if (StringListOptionWidget.this.reorderable) {
                final boolean gripHovered = hoverEnabled && this.isOverGrip(mouseX, mouseY);
                WLRenderSystem.cursors().requestIf(gripHovered || dragged, Cursors.Cursor.RESIZE_NS);
                StringListOptionWidget.this.extractGrip(graphics, StringListOptionWidget.this.contentLeft(), y, h, alpha, gripHovered || dragged);
            }
            if (this.field != null) {
                this.field.setLayer(dragged ? DRAG_LAYER : this.baseLayer);
                this.field.extract(graphics, this.chipX, y, this.chipW, h, mouseX, mouseY, dt, alpha, available, hoverEnabled && !dragged);
            } else {
                this.extractChip(graphics, y, h, mouseX, mouseY, dt, alpha, available, hoverEnabled, dragged);
            }
            final float buttonAlpha = alpha * (1f - StringListOptionWidget.this.dragFade);
            StringListOptionWidget.this.extractIconButton(graphics, this.removeButton, Icons.MINUS, buttonX, y, h, mouseX, mouseY, dt, buttonAlpha, available, hoverEnabled && StringListOptionWidget.this.dragEntry == null);
        }

        private void extractChip(final WLGraphics2D graphics, final float y, final float h, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available, final boolean hoverEnabled, final boolean dragged) {
            final boolean hovered = hoverEnabled && !dragged && this.isOverContent(mouseX, mouseY);
            WLRenderSystem.cursors().requestControl(hovered, available);
            this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
            final GlassStyle base = available ? this.style : GlassStyle.inactive(this.style);
            graphics.glass()
                .dimensions(this.chipX, y, this.chipW, h)
                .cornerRadius(h * CHIP_CORNER_RATIO)
                .style(base.copy().flat().hoverDarken(this.hoverAnim)
                    .scaled(StringListOptionWidget.this.uiScale)
                    .opacity(alpha)
                    .layer(dragged ? DRAG_LAYER : this.baseLayer))
                .extract();
            final float s = StringListOptionWidget.this.uiScale;
            float textX = this.chipX + CHIP_INSET * s;
            final ListIcon icon = StringListOptionWidget.this.catalogue.icon(this.value());
            if (icon != null) {
                final float iconSize = h * 0.7f;
                StringListOptionWidget.this.extractEntryIcon(graphics, icon, textX, y + (h - iconSize) / 2f, iconSize, alpha);
                textX += iconSize + CHIP_ICON_GAP * s;
            }
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * s, h * 0.55f);
            final float room = Math.max(4f, this.chipX + this.chipW - CHIP_INSET * s - textX);
            final String label = Fonts.SFPRO_DISPLAY.truncate(StringListOptionWidget.this.catalogue.label(this.value()), size, room);
            final float baseline = y + h / 2f + size * 0.7f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, label, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(Theme.get().label(), alpha));
        }

        boolean isOverGrip(final double mouseX, final double mouseY) {
            final float w = (GRIP_WIDTH + GRIP_GAP) * StringListOptionWidget.this.uiScale;
            return mouseX >= StringListOptionWidget.this.contentLeft() && mouseX < StringListOptionWidget.this.contentLeft() + w && mouseY >= this.drawnY && mouseY < this.drawnY + StringListOptionWidget.this.entryHeight();
        }

        boolean isOverContent(final double mouseX, final double mouseY) {
            return mouseX >= this.chipX && mouseX < this.chipX + this.chipW && mouseY >= this.drawnY && mouseY < this.drawnY + StringListOptionWidget.this.entryHeight();
        }
}

    private final class EntryPickerPopover implements PopoverHost.PopoverContent {
        private static final float PAD = 14f;
        private static final float SEARCH_H = OptionWidgetFactory.ROW_HEIGHT_BASE * TextFieldControl.ROW_HEIGHT_RATIO;
        private static final float ROW_H = 20f;
        private static final float WIDTH = 220f;
        private static final float HEIGHT = 260f;
        private static final float SCROLL_STEP = 34f;
        private static final float ROW_HOVER_SPEED = 18f;
        private static final float SCROLL_GLIDE_SPEED = 18f;
        private static final float SCROLLBAR_W = 4f;
        private static final float SCROLLBAR_GRAB = 4f;
        private static final float SCROLLBAR_GAP = 5f;
        private static final int SECTIONS = 2;
        private static final float TOP_FADE_CURVE = 0.55f;
        private final SearchBar search = new SearchBar(0, 0, 1, 1, q -> this.refilter()).deepLens();
        private List<String> filtered = List.of();
        private String appliedQuery;
        private float scrollTarget;
        private final ScrollGlide glide = new ScrollGlide();
        private boolean draggingThumb;
        private float thumbGrabOffset;
        private float lastListTop;
        private float lastListHeight;
        private float lastThumbX;
        private float scrollY;
        private float hoverAnim;

        {
            this.search.style().layer(PopoverHost.CONTENT_LAYER);
            this.search.setFocused(true);
        }

        private float s() {
            return StringListOptionWidget.this.uiScale;
        }

        @Override
        public int preferredWidth() {
            return Math.round(WIDTH * this.s());
        }

        @Override
        public int preferredHeight() {
            return Math.round(HEIGHT * this.s());
        }

        @Override
        public GlassStyle originStyle() {
            return StringListOptionWidget.this.addButtonStyle.scaled(this.s()).layer(PopoverHost.PANEL_LAYER);
        }

        @Override
        public void onClosed() {
            StringListOptionWidget.this.pickerActive = false;
        }

        @Override
        public LiquidRect liveAnchor() {
            return StringListOptionWidget.this.addScreenRect();
        }

        @Override
        public MorphProfile morphProfile() {
            return PICKER_MORPH;
        }

        @Override
        public boolean bridged() {
            return false;
        }

        @Override
        public int landingLayer() {
            return StringListOptionWidget.this.addButtonStyle.getLayer();
        }

        @Override
        public void extractLanding(final WLGraphics2D graphics, final LiquidRect current, final float landing) {
            if (landing <= 0.001f) {
                return;
            }
            final float iconSize = StringListOptionWidget.this.addSize * ICON_RATIO * (StringListOptionWidget.this.addScreenSize / Math.max(1f, Math.round(StringListOptionWidget.this.addSize)));
            Icons.PLUS.extract(graphics, current.centerX() - iconSize / 2f, current.centerY() - iconSize / 2f, iconSize, ColorUtils.withAlpha(Theme.get().label(), landing));
        }

        private void refilter() {
            final String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
            if (query.equals(this.appliedQuery)) {
                return;
            }
            this.appliedQuery = query;
            final List<String> all = StringListOptionWidget.this.catalogue.values();
            if (query.isEmpty()) {
                this.filtered = all;
            } else {
                final List<String> matches = new ArrayList<>();
                for (final String value : all) {
                    if (StringListOptionWidget.this.catalogue.label(value).toLowerCase(Locale.ROOT).contains(query) || value.toLowerCase(Locale.ROOT).contains(query)) {
                        matches.add(value);
                    }
                }
                final String typed = this.search.getValue().trim();
                if (StringListOptionWidget.this.catalogue.allowsCustom() && matches.stream().noneMatch(typed::equalsIgnoreCase)) {
                    matches.add(0, typed);
                }
                this.filtered = matches;
            }
            this.scrollY = 0f;
            this.scrollTarget = 0f;
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
            Fonts.SFPRO_DISPLAY.extract(graphics, this.option().getName(), x0, titleTop, titleSize, ColorUtils.withAlpha(OptionRowWidget.LABEL_COLOR(), a));
            Materialize.exit(pose);
            final float searchY = this.searchY(panelY);
            a = Materialize.enter(pose, Materialize.stagger(reveal, 1, SECTIONS), cx, searchY + SEARCH_H * s / 2f, s);
            this.extractSearch(graphics, x0, searchY, x1 - x0, SEARCH_H * s, mouseX, mouseY, delta, a);
            Materialize.exit(pose);
            this.extractList(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, dtSeconds, reveal);
        }

        private Option<List<String>> option() {
            return StringListOptionWidget.this.option;
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
            WLRenderSystem.contentStratum().beginPopover();
            try {
                if (this.filtered.isEmpty()) {
                    final float size = EMPTY_TEXT_SIZE * s;
                    final String text = "No matches";
                    final float w = Fonts.SFPRO_DISPLAY.width(text, size);
                    final float baseline = restY + (bottom - restY) / 2f + size * 0.59043f * 0.5f;
                    Fonts.SFPRO_DISPLAY.extract(graphics, text, panelX + (panelW - w) / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), reveal));
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
                    boolean anyHovered = false;
                    for (int i = first; i <= last; i++) {
                        final String value = this.filtered.get(i);
                        final float ry = restY + i * rowH - shown;
                        final float fade = this.dissolve((ry + rowH / 2f) - clipTop, fadeBand);
                        if (fade <= 0f) {
                            continue;
                        }
                        final boolean hovered = pointerInList && mouseY >= ry && mouseY < ry + rowH;
                        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
                        anyHovered |= hovered;
                        this.extractPickerRow(graphics, value, x0, ry, x1 - x0, rowH, hovered, reveal * fade, dtSeconds);
                    }
                    this.hoverAnim = MathUtils.approach(this.hoverAnim, anyHovered ? 1f : 0f, ROW_HOVER_SPEED, dtSeconds);
                } finally {
                    graphics.disableScissor();
                }
                this.extractScrollbar(graphics, x1, restY, bottom - restY, rowH, reveal);
            } finally {
                WLRenderSystem.contentStratum().endPopover();
                WLRenderSystem.markPopoverText(textMark);
            }
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

        private void extractScrollbar(final WLGraphics2D graphics, final float x1, final float top, final float viewH, final float rowH, final float reveal) {
            final float thumbH = this.thumbHeight(viewH);
            if (thumbH <= 0f) {
                return;
            }
            final float barW = SCROLLBAR_W * this.s();
            final float x0 = this.lastThumbX;
            final float y = this.thumbY();
            final int color = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().label(), this.draggingThumb ? 0.55f : 0.3f), reveal);
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

        private void extractPickerRow(final WLGraphics2D graphics, final String value, final float x, final float y, final float w, final float h, final boolean hovered, final float reveal, final float dtSeconds) {
            final float s = this.s();
            final boolean selected = !StringListOptionWidget.this.allowDuplicates && StringListOptionWidget.this.contains(value);
            final float radius = h * 0.28f;
            if (selected) {
                graphics.fillRoundedGradient(x, y, x + w, y + h, ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.22f), reveal), ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.22f), reveal), ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.22f), reveal), ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.22f), reveal), radius);
            } else if (hovered) {
                final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().shadowColor(), 0.14f), reveal * this.hoverAnim);
                graphics.fillRoundedGradient(x, y, x + w, y + h, fill, fill, fill, fill, radius);
            }
            float textX = x + 6f * s;
            final ListIcon icon = StringListOptionWidget.this.catalogue.icon(value);
            if (icon != null) {
                final float iconSize = h * 0.7f;
                StringListOptionWidget.this.extractEntryIcon(graphics, icon, textX, y + (h - iconSize) / 2f, iconSize, reveal);
                textX += iconSize + 5f * s;
            }
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * s, h * 0.55f);
            final float markSize = size * 0.75f;
            final float room = Math.max(4f, x + w - 6f * s - markSize - 5f * s - textX);
            final String label = Fonts.SFPRO_DISPLAY.truncate(StringListOptionWidget.this.catalogue.label(value), size, room);
            final float baseline = y + h / 2f + size * 0.7f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, label, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(selected ? Theme.get().accent() : Theme.get().label(), reveal));
            final Icons.Icon mark = selected ? Icons.MINUS : Icons.PLUS;
            mark.extract(graphics, x + w - 6f * s - markSize, y + (h - markSize) / 2f, markSize, ColorUtils.withAlpha(selected ? Theme.get().accent() : Theme.get().tertiaryLabel(), reveal));
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
            StringListOptionWidget.this.toggleValue(this.filtered.get(index));
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
}
