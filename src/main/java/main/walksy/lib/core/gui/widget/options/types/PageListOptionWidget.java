package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.ListIcon;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.gui.widget.options.popover.CataloguePickerPopover;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;
import net.minecraft.util.Mth;

public class PageListOptionWidget extends OptionRowWidget<List<String>> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x9A6E51L);
    private static final float ENTRY_GAP = 4f;
    private static final float BUTTON_GAP = 6f;
    private static final float BOTTOM_PADDING = 4f;
    private static final float ICON_RATIO = 0.42f;
    private static final float EMPTY_TEXT_SIZE = Type.FOOTNOTE.size();
    private static final String EMPTY_TEXT = "No entries";
    private static final String NAME_PLACEHOLDER = "Name";
    private static final String EDIT_GLOBAL_LABEL = "Edit Teams";
    private final Runnable onHeightChanged;
    private final Runnable onPageChanged;
    private final List<Entry> entries = new ArrayList<>();
    private final boolean readOnly;
    private final boolean namesOnly;
    private final ListEntries catalogue;
    private final WLButton addButton;
    private final WLButton editGlobalButton;
    private Entry renaming;

    public PageListOptionWidget(final Option<List<String>> option, final RowContext ctx, final Runnable onHeightChanged, final Runnable onPageChanged, final int x, final int y, final int width, final int rowHeight) {
        super(option, ctx, x, y, width, rowHeight);
        this.onHeightChanged = onHeightChanged;
        this.onPageChanged = onPageChanged;
        this.readOnly = option.hasReadOnlyNames();
        this.namesOnly = option.isNamesOnly();
        this.catalogue = option.getListEntries();
        this.addButton = this.readOnly ? null : WLButton.icon(0, 0, 1, 1, b -> this.onAdd());
        if (this.addButton != null) {
            this.addButton.style().layer(ControlLayers.FILL).fadeKey(FADE_KEYS.incrementAndGet());
        }
        this.editGlobalButton = this.readOnly ? new WLButton(0, 0, 1, 1, EDIT_GLOBAL_LABEL, b -> WalksyLibPanelScreen.openTeamsEditor()) : null;
        if (this.editGlobalButton != null) {
            this.editGlobalButton.style().layer(ControlLayers.FILL).fadeKey(FADE_KEYS.incrementAndGet());
        }
        this.rebuildEntries(option.getValue());
        this.recomputeHeight();
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

    private float entryY(final int index) {
        return this.getY() + this.headerHeight() + index * this.entryStride();
    }

    private void recomputeHeight() {
        final int bands = Math.max(1, this.entries.size());
        final float total = this.headerHeight() + bands * this.entryHeight() + (bands - 1) * ENTRY_GAP * this.uiScale + BOTTOM_PADDING * this.uiScale;
        setHeight(Math.round(total));
    }

    private void onAdd() {
        this.commitPendingRename();
        if (this.catalogue != null) {
            this.openPicker();
            return;
        }
        String stem = this.option.getNewEntryName();
        if (stem == null || stem.isBlank()) {
            stem = "Entry";
        }
        String base = this.option.cleanEntryName(stem);
        if (base.isEmpty()) {
            base = "Entry";
        }
        String name;
        int n = 0;
        do {
            n++;
            name = this.option.cleanEntryName(base + " " + n);
            if (name.isEmpty()) {
                name = base + n;
            }
        } while (this.nameTaken(name, null) && n < 100_000);
        if (this.nameTaken(name, null)) {
            name = base + Long.toString(System.nanoTime(), 36);
        }
        this.entries.add(new Entry(name));
        this.commit();
        this.afterStructureChanged();
    }

    private void openPicker() {
        if (this.ctx == null || this.ctx.onOpenCenteredPopover() == null) {
            return;
        }
        final float size = this.addButton.getWidth();
        final LiquidRect anchor = new LiquidRect(this.addButton.getX(), this.addButton.getY(), size, size, size / 2f);
        this.ctx.onOpenCenteredPopover().accept(anchor, new CataloguePickerPopover(this.option.getName(), this.pickerEntries(), value -> this.nameTaken(value, null), this::addCatalogueEntry, true, this.ctx.onClosePopover(), null, () -> this.addButton.style().scaled(this.uiScale).layer(PopoverHost.PANEL_LAYER), this.uiScale, PopoverHost.PANEL_LAYER));
    }

    private CataloguePickerPopover.Entries pickerEntries() {
        final ListEntries source = this.catalogue;
        return new CataloguePickerPopover.Entries() {
            @Override
            public List<String> values() {
                return source.values();
            }

            @Override
            public String label(final String value) {
                return source.label(value);
            }

            @Override
            public ListIcon icon(final String value) {
                return source.icon(value);
            }
        };
    }

    private void addCatalogueEntry(final String value) {
        if (value == null || value.isEmpty() || this.nameTaken(value, null)) {
            return;
        }
        this.entries.add(new Entry(value));
        this.commit();
        this.afterStructureChanged();
    }

    private void removeEntry(final Entry entry) {
        if (!this.entries.contains(entry)) {
            return;
        }
        final String id = entry.id;
        final boolean wasOpen = id.equals(this.option.getActivePageId());
        if (this.renaming == entry) {
            this.renaming = null;
        }
        this.entries.remove(entry);
        this.commit();
        this.option.removePage(id);
        if (wasOpen) {
            this.requestRebuild();
            return;
        }
        this.afterStructureChanged();
    }

    private void toggleEdit(final Entry entry) {
        this.commitPendingRename();
        final String id = entry.id;
        this.option.setActivePageId(id.equals(this.option.getActivePageId()) ? null : id);
        this.requestRebuild();
    }

    private void commitRename(final Entry entry) {
        final String typed = this.option.cleanEntryName(entry.field.getText().trim());
        if (typed.isEmpty() || typed.equals(entry.id) || this.nameTaken(typed, entry)) {
            entry.field.syncText(entry.id);
            return;
        }
        final String from = entry.id;
        final boolean wasOpen = from.equals(this.option.getActivePageId());
        entry.id = typed;
        entry.field.syncText(typed);
        this.commit();
        this.option.renamePage(from, typed);
        if (wasOpen) {
            this.requestRebuild();
        }
    }

    private void commitPendingRename() {
        final Entry entry = this.renaming;
        if (entry == null || entry.field == null) {
            this.renaming = null;
            return;
        }
        this.renaming = null;
        entry.field.setFocused(false);
        this.commitRename(entry);
    }

    private boolean nameTaken(final String name, final Entry exclude) {
        for (final Entry entry : this.entries) {
            if (entry != exclude && entry.id.equals(name)) {
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
            values.add(entry.id);
        }
        this.option.setValue(values);
    }

    private void rebuildEntries(final List<String> values) {
        this.entries.clear();
        this.renaming = null;
        if (values == null) {
            return;
        }
        for (final String value : values) {
            if (value != null && !value.isEmpty() && !this.nameTaken(value, null)) {
                this.entries.add(new Entry(value));
            }
        }
    }

    private void syncFromOption() {
        if (this.renaming != null) {
            return;
        }
        List<String> values = this.option.getValue();
        if (values == null) {
            values = List.of();
        }
        if (values.size() == this.entries.size()) {
            boolean same = true;
            for (int i = 0; i < values.size(); i++) {
                if (!this.entries.get(i).id.equals(values.get(i))) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return;
            }
        }
        this.rebuildEntries(values);
        this.afterStructureChanged();
    }

    private void requestRebuild() {
        if (this.onPageChanged != null) {
            this.onPageChanged.run();
        }
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.syncFromOption();
        final boolean hoverEnabled = available && !coveredByOverlay(mouseX, mouseY);
        final float size = this.entryHeight();
        final float addY = this.getY() + (this.headerHeight() - size) / 2f;
        if (this.readOnly) {
            final float labelSize = Mth.clamp(size * 0.5f, Type.CAPTION1.size() * this.uiScale, Type.TITLE2.size() * this.uiScale);
            final float buttonW = Fonts.SFPRO_DISPLAY.width(EDIT_GLOBAL_LABEL, labelSize) + size;
            this.layoutTextButton(graphics, this.editGlobalButton, this.contentRight() - buttonW, addY, buttonW, size, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        } else {
            this.extractIconButton(graphics, this.addButton, Icons.PLUS, 0f, this.contentRight() - size, addY, size, mouseX, mouseY, dt, alpha, available, hoverEnabled, Theme.get().label());
        }
        if (this.entries.isEmpty()) {
            final float textSize = EMPTY_TEXT_SIZE * this.uiScale;
            final float baseline = this.entryY(0) + size / 2f + textSize * 0.59043f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, EMPTY_TEXT, this.contentLeft(), Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
            return;
        }
        final String openId = this.option.getActivePageId();
        for (int i = 0; i < this.entries.size(); i++) {
            this.entries.get(i).extract(graphics, i, openId, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        }
    }

    private void extractIconButton(final WLGraphics2D graphics, final WLButton button, final Icons.Icon icon, final float rotationDegrees, final float x, final float y, final float size, final int mouseX, final int mouseY, final float delta, final float alpha, final boolean available, final boolean hoverEnabled, final int iconColor) {
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
        final float ix = button.getX() + (button.getWidth() - iconSize) / 2f;
        final float iy = button.getY() + (button.getHeight() - iconSize) / 2f;
        final int tint = ColorUtils.withAlpha(iconColor, alpha);
        button.stretchContent(() -> {
            if (rotationDegrees == 0f) {
                icon.extract(graphics, ix, iy, iconSize, tint);
            } else {
                icon.extractRotated(graphics, ix, iy, iconSize, tint, rotationDegrees);
            }
        });
    }

    private void layoutTextButton(final WLGraphics2D graphics, final WLButton button, final float x, final float y, final float w, final float h, final int mouseX, final int mouseY, final float delta, final float alpha, final boolean available, final boolean hoverEnabled) {
        if (button == null || alpha <= 0.001f) {
            return;
        }
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.round(w));
        button.setHeight(Math.round(h));
        button.setCornerRadius(h / 2f);
        button.active = available;
        button.setAlpha(alpha);
        final int mx = hoverEnabled ? mouseX : TextFieldControl.CURSOR_AWAY;
        final int my = hoverEnabled ? mouseY : TextFieldControl.CURSOR_AWAY;
        graphics.widget(button, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive()) {
            return false;
        }
        return this.handleClick(event, doubleClick);
    }

    private boolean handleClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.addButton != null && this.addButton.isMouseOver(event.x(), event.y())) {
            focus(null);
            return this.addButton.mouseClicked(event, doubleClick);
        }
        if (this.editGlobalButton != null && this.editGlobalButton.isMouseOver(event.x(), event.y())) {
            return this.editGlobalButton.mouseClicked(event, doubleClick);
        }
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            if (entry.field != null && entry.field.isOver(event.x(), event.y())) {
                focus(entry);
                entry.field.clicked(event, doubleClick);
                return true;
            }
            if (entry.editButton != null && entry.editButton.isMouseOver(event.x(), event.y())) {
                focus(null);
                return entry.editButton.mouseClicked(event, doubleClick);
            }
            if (entry.removeButton != null && entry.removeButton.isMouseOver(event.x(), event.y())) {
                focus(null);
                return entry.removeButton.mouseClicked(event, doubleClick);
            }
        }
        focus(null);
        return false;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        return this.renaming != null && this.renaming.field != null && this.renaming.field.dragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        boolean handled = WLButton.release(this.addButton, event);
        handled |= WLButton.release(this.editGlobalButton, event);
        for (final Entry entry : List.copyOf(this.entries)) {
            handled |= WLButton.release(entry.editButton, event);
            handled |= WLButton.release(entry.removeButton, event);
        }
        return handled;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final Entry entry = this.renaming;
        if (entry == null || entry.field == null) {
            return false;
        }
        final boolean handled = entry.field.keyPressed(event);
        if (!entry.field.isFocused()) {
            this.renaming = null;
            this.commitRename(entry);
        }
        return handled;
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.renaming != null && this.renaming.field != null && this.renaming.field.charTyped(event);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            focus(null);
        }
    }

    private void focus(final Entry target) {
        if (this.renaming != null && this.renaming != target) {
            this.commitPendingRename();
        }
        for (final Entry entry : this.entries) {
            if (entry.field != null) {
                entry.field.setFocused(entry == target);
            }
        }
        this.renaming = this.readOnly || this.catalogue != null ? null : target;
    }

    private final class Entry {
        String id;
        final TextFieldControl field;
        final WLButton editButton;
        final WLButton removeButton;
        private final GlassStyle chipStyle;

        Entry(final String id) {
            this.id = id;
            final GlassStyle style = SearchBar.defaultStyle().fadeKey(FADE_KEYS.incrementAndGet());
            if (PageListOptionWidget.this.readOnly || PageListOptionWidget.this.catalogue != null) {
                this.field = null;
                this.chipStyle = style;
            } else {
                this.chipStyle = null;
                this.field = new TextFieldControl(id, text -> { }, NAME_PLACEHOLDER, style, PageListOptionWidget.this.uiScale);
            }
            if (PageListOptionWidget.this.namesOnly) {
                this.editButton = null;
            } else {
                this.editButton = WLButton.icon(0, 0, 1, 1, b -> PageListOptionWidget.this.toggleEdit(this));
                this.editButton.style().layer(ControlLayers.FILL).fadeKey(FADE_KEYS.incrementAndGet());
            }
            if (PageListOptionWidget.this.readOnly) {
                this.removeButton = null;
            } else {
                this.removeButton = WLButton.icon(0, 0, 1, 1, b -> PageListOptionWidget.this.removeEntry(this));
                this.removeButton.style().layer(ControlLayers.KNOB).fadeKey(FADE_KEYS.incrementAndGet());
            }
        }

        void extract(final WLGraphics2D graphics, final int index, final String openId, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available, final boolean hoverEnabled) {
            final float h = PageListOptionWidget.this.entryHeight();
            final float y = PageListOptionWidget.this.entryY(index);
            final float gap = BUTTON_GAP * PageListOptionWidget.this.uiScale;
            float right = PageListOptionWidget.this.contentRight();
            final float removeX = right - h;
            if (this.removeButton != null) {
                right = removeX - gap;
            }
            final float editX = right - h;
            if (this.editButton != null) {
                right = editX - gap;
            }
            final float nameX = PageListOptionWidget.this.contentLeft();
            final float nameW = Math.max(1f, right - nameX);
            if (this.field != null) {
                this.field.extract(graphics, nameX, y, nameW, h, mouseX, mouseY, dt, alpha, available, hoverEnabled);
            } else {
                this.extractNamePlate(graphics, nameX, y, nameW, h, alpha, available);
            }
            final boolean open = this.id.equals(openId);
            if (this.editButton != null) {
                PageListOptionWidget.this.extractIconButton(graphics, this.editButton, Icons.CHEVRON, open ? 90f : 0f, editX, y, h, mouseX, mouseY, dt, alpha, available, hoverEnabled, open ? Theme.get().accent() : Theme.get().label());
            }
            if (this.removeButton != null) {
                PageListOptionWidget.this.extractIconButton(graphics, this.removeButton, Icons.MINUS, 0f, removeX, y, h, mouseX, mouseY, dt, alpha, available, hoverEnabled, Theme.get().label());
            }
        }

        private void extractNamePlate(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final float alpha, final boolean available) {
            final float s = PageListOptionWidget.this.uiScale;
            final GlassStyle base = available ? this.chipStyle : GlassStyle.inactive(this.chipStyle);
            graphics.glass().dimensions(x, y, w, h).cornerRadius(h * 0.3f).style(base.scaled(s).opacity(alpha)).extract();
            final float size = Math.min(OptionRowWidget.LABEL_SIZE * s, h * 0.55f);
            final float inset = 5f * s;
            final float baseline = y + h / 2f + size * 0.7f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, PageListOptionWidget.this.option.pageLabel(this.id), x + inset, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(available ? Theme.get().label() : Theme.get().tertiaryLabel(), alpha));
        }
}
}
