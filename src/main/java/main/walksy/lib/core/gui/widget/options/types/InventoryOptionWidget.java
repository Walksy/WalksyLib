package main.walksy.lib.core.gui.widget.options.types;
import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ItemSpec;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.ListIcon;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.texture.ItemModels;
import main.walksy.lib.core.render.texture.ListIcons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.control.SlotCursor;
import main.walksy.lib.core.gui.widget.options.control.TrashSlot;
import main.walksy.lib.core.gui.widget.options.popover.CataloguePickerPopover;
import main.walksy.lib.core.gui.widget.options.popover.ItemEditorPopover;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class InventoryOptionWidget extends OptionRowWidget<ItemInventory> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x1E7A11L);
    static final CataloguePickerPopover.Entries ITEM_ENTRIES = new CataloguePickerPopover.Entries() {

        private final ListEntries catalogue = ListEntries.items();
        private List<String> cached;

        @Override
        public List<String> values() {
            if (this.cached == null) {
                final List<String> all = new ArrayList<>(this.catalogue.values());
                all.remove("minecraft:air");
                this.cached = List.copyOf(all);
            }
            return this.cached;
        }

        @Override
        public String label(final String value) {
            return this.catalogue.label(value);
        }

        @Override
        public ListIcon icon(final String value) {
            return this.catalogue.icon(value);
        }

        @Override
        public String itemId(final String value) {
            return value;
        }
    };
    private static final int COLUMNS = ItemInventory.HOTBAR_SIZE;
    private static final float SLOT_SIZE = 18f;
    private static final float SLOT_GAP = 2f;
    private static final float BAND_GAP = 6f;
    private static final float TRASH_GAP = 6f;
    private static final float GRID_TOP_GAP = 4f;
    private static final float CARD_PADDING = 5f;
    private static final float CARD_RADIUS = 8f;
    private static final float READOUT_HEIGHT = 12f;
    private static final float BOTTOM_PADDING = 4f;
    private static final float ACTION_PILL_H = 14f;
    private static final float ACTION_PILL_PADDING = 7f;
    private static final float SLOT_RADIUS_RATIO = 0.22f;
    private static final float READOUT_SIZE = Type.FOOTNOTE.size();
    private static final int PILL_LAYER = ControlLayers.BASE;
    private static final int[] SLOT_LAYERS = {7, 8, 9};
    private static final float REJECT_TINT_ALPHA = 0.35f;
    private static final float SLOT_LENS = 0.35f;
    private static final int NESTED_PICKER_LAYER = 40;
    private static final int NESTED_NBT_LAYER = 46;
    private static final String[] EQUIPMENT_NAMES = {"Boots", "Leggings", "Chestplate", "Helmet"};
    private static final SlotCursor.Rules RULES = new SlotCursor.Rules() {
        @Override
        public boolean accepts(final int slot, final ItemSpec spec) {
            return ItemInventory.accepts(slot, spec.getItemId());
        }

        @Override
        public int limit(final int slot, final ItemSpec spec) {
            return slot >= ItemInventory.ARMOR_START && slot < ItemInventory.ARMOR_START + ItemInventory.ARMOR_SIZE ? 1 : spec.maxStackSize();
        }
    };
    private final boolean container;
    private final WLButton clearButton;
    private final long slotFadeKeys;
    private float gridX0;
    private float gridY0;
    private float slotSize;
    private float slotPitch;
    private int activeSlot = -1;
    private float activeSlotFade = 1f;
    private int hoveredSlot = -1;
    private int pressedSlot = -1;
    private double pressX;
    private double pressY;
    private boolean dragging;
    private double dragX;
    private double dragY;
    private final SlotCursor cursor = new SlotCursor();
    private final TrashSlot trash = new TrashSlot();
    private boolean trashHovered;
    private float cursorX;
    private float cursorY;

    public InventoryOptionWidget(final Option<ItemInventory> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.container = option.isContainer();
        this.clearButton = new WLButton(0, 0, 1, 1, "Clear All", b -> this.mutate(ItemInventory::clearAll));
        this.clearButton.style().layer(PILL_LAYER).fadeKey(FADE_KEYS.incrementAndGet());
        this.slotFadeKeys = FADE_KEYS.addAndGet(ItemInventory.SIZE + 1) - ItemInventory.SIZE;
        this.setHeight(Math.round(this.preferredHeight()));
        WalksyLib.getInstance().getOfflineRegistries().warm();
    }

    private void mutate(final Consumer<ItemInventory> mutation) {
        final ItemInventory updated = this.value().copy();
        mutation.accept(updated);
        this.option.setValue(updated);
    }

    private ItemInventory value() {
        final ItemInventory live = this.option.getValue();
        return live == null ? new ItemInventory() : live;
    }

    private List<ItemSpec> slotList() {
        final ItemInventory inventory = this.value();
        final List<ItemSpec> slots = new ArrayList<>(ItemInventory.SIZE);
        for (int slot = 0; slot < ItemInventory.SIZE; slot++) {
            slots.add(inventory.get(slot));
        }
        return slots;
    }

    private void commit(final List<ItemSpec> slots) {
        this.mutate(inventory -> {
            for (int slot = 0; slot < slots.size(); slot++) {
                inventory.set(slot, slots.get(slot));
            }
        });
    }

    public static String slotName(final int slot) {
        if (slot == ItemInventory.OFFHAND) {
            return "Off-hand";
        }
        if (slot >= ItemInventory.ARMOR_START && slot < ItemInventory.ARMOR_START + ItemInventory.ARMOR_SIZE) {
            return EQUIPMENT_NAMES[slot - ItemInventory.ARMOR_START];
        }
        if (slot < ItemInventory.HOTBAR_SIZE) {
            return "Hotbar " + (slot + 1);
        }
        return "Slot " + (slot - ItemInventory.STORAGE_START + 1);
    }

    private static ListIcon emptySlotIcon(final int slot) {
        if (slot == ItemInventory.OFFHAND) {
            return ListIcon.sprite(Sheets.GUI_SHEET, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
        return switch (slot - ItemInventory.ARMOR_START) {
            case 0 -> ListIcon.sprite(Sheets.GUI_SHEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS);
            case 1 -> ListIcon.sprite(Sheets.GUI_SHEET, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS);
            case 2 -> ListIcon.sprite(Sheets.GUI_SHEET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE);
            case 3 -> ListIcon.sprite(Sheets.GUI_SHEET, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET);
            default -> null;
        };
    }

    private int firstSlot() {
        return this.container ? ItemInventory.STORAGE_START : 0;
    }

    private int endSlot() {
        return this.container ? ItemInventory.STORAGE_START + ItemInventory.STORAGE_SIZE : ItemInventory.SIZE;
    }

    private int[] cell(final int slot) {
        if (this.container) {
            final int index = slot - ItemInventory.STORAGE_START;
            return new int[]{index % COLUMNS, index / COLUMNS};
        }
        if (slot == ItemInventory.OFFHAND) {
            return new int[]{COLUMNS - 1, 0};
        }
        if (slot >= ItemInventory.ARMOR_START) {
            return new int[]{ItemInventory.ARMOR_START + ItemInventory.ARMOR_SIZE - 1 - slot, 0};
        }
        if (slot < ItemInventory.HOTBAR_SIZE) {
            return new int[]{slot, 4};
        }
        final int index = slot - ItemInventory.STORAGE_START;
        return new int[]{index % COLUMNS, 1 + index / COLUMNS};
    }

    private float bandOffset(final int bandRow) {
        if (this.container) {
            return 0f;
        }
        final float gap = BAND_GAP * this.uiScale;
        return (bandRow >= 1 ? gap : 0f) + (bandRow >= 4 ? gap : 0f);
    }

    private float slotX(final int slot) {
        return this.gridX0 + cell(slot)[0] * this.slotPitch;
    }

    private float slotY(final int slot) {
        final int band = cell(slot)[1];
        return this.gridY0 + band * this.slotPitch + this.bandOffset(band);
    }

    private float gridWidth(final float slotSize) {
        return COLUMNS * slotSize + (COLUMNS - 1) * SLOT_GAP * this.uiScale;
    }

    private float gridHeight(final float slotSize) {
        if (this.container) {
            return ItemInventory.STORAGE_ROWS * slotSize + (ItemInventory.STORAGE_ROWS - 1) * SLOT_GAP * this.uiScale;
        }
        return 5 * slotSize + 4 * SLOT_GAP * this.uiScale + 2 * BAND_GAP * this.uiScale;
    }

    private float fittedSlotSize() {
        final float available = this.getWidth() - (PADDING + CARD_PADDING) * 2f * this.uiScale;
        final float fromWidth = (available - (COLUMNS - 1) * SLOT_GAP * this.uiScale - TRASH_GAP * this.uiScale) / (COLUMNS + 1);
        return Math.max(6f * this.uiScale, Math.min(SLOT_SIZE * this.uiScale, fromWidth));
    }

    private float preferredHeight() {
        final float slot = this.fittedSlotSize();
        return OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale + GRID_TOP_GAP * this.uiScale + this.gridHeight(slot) + CARD_PADDING * 2f * this.uiScale + READOUT_HEIGHT * this.uiScale + BOTTOM_PADDING * this.uiScale;
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float s = this.uiScale;
        this.slotSize = this.fittedSlotSize();
        this.slotPitch = this.slotSize + SLOT_GAP * s;
        final float cardW = this.gridWidth(this.slotSize) + CARD_PADDING * 2f * s;
        final float cardH = this.gridHeight(this.slotSize) + CARD_PADDING * 2f * s;
        final float cardX0 = this.getX() + PADDING * s;
        final float cardY0 = this.getY() + this.rowLabelHeight() + GRID_TOP_GAP * s;
        this.gridX0 = cardX0 + CARD_PADDING * s;
        this.gridY0 = cardY0 + CARD_PADDING * s;
        this.extractHeaderPills(graphics, mouseX, mouseY, dt, alpha, available);
        final int card = ColorUtils.withAlpha(Theme.get().cardFillArgb(), alpha);
        graphics.fillRoundedGradient(cardX0, cardY0, cardX0 + cardW, cardY0 + cardH, card, card, card, card, CARD_RADIUS * s);
        final boolean covered = coveredByOverlay(mouseX, mouseY);
        this.hoveredSlot = available && !covered ? this.slotAt(mouseX, mouseY) : -1;
        WLRenderSystem.cursors().requestIf(this.hoveredSlot >= 0, Cursors.Cursor.POINTER);
        this.cursorX = mouseX;
        this.cursorY = mouseY;
        this.cursor.validate(this.slotList());
        final ItemInventory inventory = this.value();
        for (int slot = this.firstSlot(); slot < this.endSlot(); slot++) {
            this.extractSlot(graphics, inventory, slot, available, alpha);
        }
        this.trash.layout(cardX0 + cardW + TRASH_GAP * s, this.gridY0 + this.gridHeight(this.slotSize) - this.slotSize, this.slotSize);
        this.trashHovered = available && !covered && this.trash.contains(mouseX, mouseY);
        final int trashLayer = SLOT_LAYERS[(this.container ? ItemInventory.STORAGE_ROWS - 1 : 4) % SLOT_LAYERS.length];
        this.trash.extractGlass(graphics, this.slotStyle(trashLayer, this.slotFadeKeys + ItemInventory.SIZE, available, alpha), this.slotSize * SLOT_RADIUS_RATIO, this.trashHovered, this.trashArmed(), alpha, dt);
        this.extractReadout(graphics, inventory, cardY0 + cardH, alpha);
        this.extractDragGhost(graphics, inventory, alpha);
        this.extractCarried(graphics, alpha);
    }

    private void extractCarried(final WLGraphics2D graphics, final float alpha) {
        final ItemSpec carried = this.cursor.carried(this.slotList());
        if (carried == null) {
            return;
        }
        final float size = this.slotSize;
        this.extractItem(graphics, carried, this.cursorX - size / 2f, this.cursorY - size / 2f, size, alpha, 0);
    }

    private void extractHeaderPills(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float s = this.uiScale;
        final float h = ACTION_PILL_H * s;
        final float y = this.getY() + (this.rowLabelHeight() - h) / 2f;
        final float textSize = Type.CAPTION2.size() * s;
        final float clearW = Fonts.SFPRO_DISPLAY.width(this.clearButton.getLabel(), textSize) + ACTION_PILL_PADDING * 2f * s;
        final float clearX = this.getX() + this.getWidth() - PADDING * s - clearW;
        this.layoutPill(this.clearButton, clearX, y, clearW, h, alpha, textSize);
        this.clearButton.active = available && this.filledCount(this.value()) > 0;
        graphics.widget(this.clearButton, mouseX, mouseY, dt);
    }

    private void layoutPill(final WLButton button, final float x, final float y, final float w, final float h, final float alpha, final float textSize) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.max(1, Math.round(w)));
        button.setHeight(Math.max(1, Math.round(h)));
        button.setAlpha(alpha);
        button.setCornerRadius(h * 0.35f);
        button.setTextSize(textSize);
    }

    private void extractSlot(final WLGraphics2D graphics, final ItemInventory inventory, final int slot, final boolean available, final float alpha) {
        final float x = this.slotX(slot);
        final float y = this.slotY(slot);
        final float size = this.slotSize;
        final float radius = size * SLOT_RADIUS_RATIO;
        final boolean hovered = slot == this.hoveredSlot || this.cursor.spreadsOver(slot);
        final boolean source = this.dragging && slot == this.pressedSlot;
        final float showing = slot == this.activeSlot ? alpha * Mth.clamp(this.activeSlotFade, 0f, 1f) : alpha;
        if (showing <= 0.004f) {
            return;
        }
        this.extractSlotWell(graphics, x, y, size, radius, slot, hovered, available, showing);
        final ItemSpec spec = this.cursor.shown(slot, inventory.get(slot));
        if (spec == null || source) {
            if (spec == null) {
                this.extractEmptyGhost(graphics, slot, x, y, size, showing);
            }
            return;
        }
        this.extractItem(graphics, spec, x, y, size, showing, 0);
    }

    private void extractSlotWell(final WLGraphics2D graphics, final float x, final float y, final float size, final float radius, final int slot, final boolean hovered, final boolean available, final float alpha) {
        final GlassStyle style = this.slotStyle(SLOT_LAYERS[cell(slot)[1] % SLOT_LAYERS.length], this.slotFadeKeys + slot, available, alpha);
        if (hovered && this.hoveringRejectedDrop()) {
            style.tint(Theme.get().destructive(), REJECT_TINT_ALPHA);
        }
        graphics.glass()
            .dimensions(x, y, size, size)
            .cornerRadius(radius)
            .style(style)
            .hover(hovered && available ? 1f : 0f)
            .extract();
    }

    private GlassStyle slotStyle(final int layer, final long fadeKey, final boolean available, final float alpha) {
        final GlassStyle knob = ResetButton.knobStyle(layer).fadeKey(fadeKey);
        knob.refractionThickness(knob.getRefThickness() * SLOT_LENS)
            .refractionDispersion(knob.getRefDispersion() * SLOT_LENS)
            .refractionFactor(1f + (knob.getRefFactor() - 1f) * SLOT_LENS);
        final GlassStyle style = (available ? knob : GlassStyle.inactive(knob)).scaled(this.uiScale);
        return alpha < 1f ? style.opacity(style.getOpacity() * alpha) : style;
    }

    private void extractEmptyGhost(final WLGraphics2D graphics, final int slot, final float x, final float y, final float size, final float alpha) {
        final ListIcon ghost = emptySlotIcon(slot);
        if (ghost != null) {
            final float inset = size * 0.15f;
            ListIcons.extract(graphics, ghost, x + inset, y + inset, size - inset * 2f, alpha * 0.35f);
        }
    }

    private void extractItem(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final float alpha, final int layer) {
        final float inset = size * 0.08f;
        ItemModels.extractOrFallback(graphics, spec, x + inset, y + inset, size - inset * 2f, layer, alpha);
    }

    private void extractReadout(final WLGraphics2D graphics, final ItemInventory inventory, final float cardBottom, final float alpha) {
        final float s = this.uiScale;
        final float size = READOUT_SIZE * s;
        final float y = cardBottom + (READOUT_HEIGHT * s - size) / 2f;
        final String text;
        final int color;
        final ItemSpec carried = this.cursor.carried(this.slotList());
        final ItemSpec dragged = this.dragging && this.pressedSlot >= 0 ? inventory.get(this.pressedSlot) : null;
        if (this.trashHovered && (carried != null || dragged != null)) {
            text = (carried != null ? "Click" : "Release") + " to delete " + describe(carried != null ? carried : dragged);
            color = Theme.get().destructive();
        } else if (carried != null) {
            text = "Holding " + describe(carried);
            color = Theme.get().secondaryLabel();
        } else if (this.trashHovered) {
            text = "Drag an item here to delete it";
            color = Theme.get().tertiaryLabel();
        } else if (this.hoveringRejectedDrop()) {
            final ItemSpec moving = inventory.get(this.pressedSlot);
            text = (moving == null ? "That" : moving.displayName()) + " doesn't go in " + slotName(this.hoveredSlot);
            color = Theme.get().destructive();
        } else if (this.hoveredSlot >= 0) {
            final ItemSpec spec = inventory.get(this.hoveredSlot);
            text = spec == null ? slotName(this.hoveredSlot) + " — empty" : slotName(this.hoveredSlot) + " — " + describe(spec);
            color = spec == null ? Theme.get().tertiaryLabel() : Theme.get().secondaryLabel();
        } else {
            final int filled = this.filledCount(inventory);
            text = filled == 0 ? "Empty — click a slot to add an item" : filled + (filled == 1 ? " item" : " items");
            color = Theme.get().tertiaryLabel();
        }
        final float maxWidth = this.getWidth() - PADDING * 2f * s;
        Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(text, size, maxWidth), this.getX() + PADDING * s, Fonts.SFPRO_DISPLAY.topForBaseline(y + size, size), size, ColorUtils.withAlpha(color, alpha));
    }

    private static String describe(final ItemSpec spec) {
        final StringBuilder out = new StringBuilder();
        if (spec.getCount() > 1) {
            out.append(spec.getCount()).append("x ");
        }
        out.append(spec.displayName());
        if (!spec.getEnchantments().isEmpty()) {
            out.append(" · ");
            boolean first = true;
            for (final Map.Entry<String, Integer> entry : spec.getEnchantments().entrySet()) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append(ItemSpec.enchantmentDisplayName(entry.getKey())).append(' ').append(ItemSpec.romanLevel(entry.getValue()));
            }
        }
        return out.toString();
    }

    private void extractDragGhost(final WLGraphics2D graphics, final ItemInventory inventory, final float alpha) {
        if (!this.dragging || this.pressedSlot < 0) {
            return;
        }
        final ItemSpec spec = inventory.get(this.pressedSlot);
        if (spec == null) {
            return;
        }
        final float size = this.slotSize;
        this.extractItem(graphics, spec, (float) this.dragX - size / 2f, (float) this.dragY - size / 2f, size, alpha, 0);
    }

    private boolean trashArmed() {
        return this.cursor.holding() || (this.dragging && this.pressedSlot >= 0);
    }

    private int filledCount(final ItemInventory inventory) {
        int filled = 0;
        for (int slot = this.firstSlot(); slot < this.endSlot(); slot++) {
            if (inventory.get(slot) != null) {
                filled++;
            }
        }
        return filled;
    }

    private int slotAt(final double mouseX, final double mouseY) {
        if (this.slotSize <= 0f) {
            return -1;
        }
        for (int slot = this.firstSlot(); slot < this.endSlot(); slot++) {
            final float x = this.slotX(slot);
            final float y = this.slotY(slot);
            if (mouseX >= x && mouseX < x + this.slotSize && mouseY >= y && mouseY < y + this.slotSize) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isInteractive() || coveredByOverlay(event.x(), event.y())) {
            return false;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (this.clearButton.active && this.clearButton.isMouseOver(event.x(), event.y())) {
                this.clearButton.onClick(event, doubleClick);
                return true;
            }
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT && event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return false;
        }
        final int slot = this.slotAt(event.x(), event.y());
        if (this.cursor.holding() && this.trash.contains(event.x(), event.y())) {
            final List<ItemSpec> changed = this.cursor.discard(this.slotList());
            if (changed != null) {
                this.commit(changed);
            }
            return true;
        }
        if (this.cursor.holding()) {
            if (slot < 0) {
                this.cursor.cancel();
            } else {
                this.cursor.press(event.button(), slot);
            }
            return true;
        }
        if (slot < 0) {
            return false;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.cursor.pickHalf(this.slotList(), slot);
            return true;
        }
        this.pressedSlot = slot;
        this.pressX = event.x();
        this.pressY = event.y();
        this.dragX = event.x();
        this.dragY = event.y();
        this.dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double deltaX, final double deltaY) {
        if (this.cursor.pressing()) {
            this.cursor.dragOver(this.slotList(), this.slotAt(event.x(), event.y()), RULES);
            return true;
        }
        if (this.pressedSlot < 0) {
            return false;
        }
        this.dragX = event.x();
        this.dragY = event.y();
        if (!this.dragging) {
            final double slop = 3f * this.uiScale;
            if (Math.abs(event.x() - this.pressX) < slop && Math.abs(event.y() - this.pressY) < slop) {
                return true;
            }
            if (this.value().get(this.pressedSlot) == null) {
                return true;
            }
            this.dragging = true;
            WLRenderSystem.cursors().request(Cursors.Cursor.MOVE);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.cursor.pressing()) {
            final List<ItemSpec> changed = this.cursor.release(this.slotList(), RULES);
            if (changed != null && this.isInteractive()) {
                this.commit(changed);
            }
            return true;
        }
        if (this.pressedSlot < 0) {
            return false;
        }
        final int from = this.pressedSlot;
        final boolean wasDragging = this.dragging;
        this.pressedSlot = -1;
        this.dragging = false;
        this.clearButton.onRelease(event);
        if (!this.isInteractive()) {
            return true;
        }
        final int target = this.slotAt(event.x(), event.y());
        if (wasDragging) {
            if (this.trash.contains(event.x(), event.y())) {
                this.mutate(inventory -> inventory.clear(from));
                return true;
            }
            if (target >= 0 && target != from && canDrop(this.value(), from, target)) {
                this.mutate(inventory -> inventory.drop(from, target));
            }
            return true;
        }
        if (target == from) {
            this.openSlot(from);
        }
        return true;
    }

    private static boolean canDrop(final ItemInventory inventory, final int from, final int to) {
        final ItemSpec moving = inventory.get(from);
        if (moving != null && !ItemInventory.accepts(to, moving.getItemId())) {
            return false;
        }
        final ItemSpec displaced = inventory.get(to);
        return displaced == null || ItemInventory.accepts(from, displaced.getItemId());
    }

    private boolean hoveringRejectedDrop() {
        return this.dragging && this.pressedSlot >= 0 && this.hoveredSlot >= 0 && this.hoveredSlot != this.pressedSlot && !canDrop(this.value(), this.pressedSlot, this.hoveredSlot);
    }

    private void openSlot(final int slot) {
        if (this.ctx == null || this.ctx.onOpenCenteredPopover() == null) {
            return;
        }
        if (this.activeSlot == slot) {
            return;
        }
        this.activeSlot = slot;
        this.activeSlotFade = 1f;
        final LiquidRect anchor = new LiquidRect(this.slotX(slot), this.slotY(slot), this.slotSize, this.slotSize, this.slotSize * SLOT_RADIUS_RATIO);
        final PopoverHost.PopoverContent content = this.value().get(slot) == null ? this.emptySlotPicker(slot) : this.slotEditor(slot);
        this.ctx.onOpenCenteredPopover().accept(anchor, content);
    }

    private PopoverHost.PopoverContent emptySlotPicker(final int slot) {
        return new CataloguePickerPopover("Add to " + slotName(slot), entriesFor(slot), value -> false, itemId -> this.mutate(inventory -> inventory.set(slot, new ItemSpec(itemId))), true, this.ctx.onClosePopover(), () -> this.releaseSlot(slot), () -> this.slotOriginStyle(slot), this.uiScale, PopoverHost.PANEL_LAYER).withFixedTriggerHandoff((graphics, handoff) -> this.trackSlotHandoff(slot, handoff));
    }

    public static CataloguePickerPopover.Entries entriesFor(final int slot) {
        if (ItemInventory.equipmentSlotFor(slot) == null || ItemInventory.equipmentSlotFor(slot) == EquipmentSlot.OFFHAND) {
            return ITEM_ENTRIES;
        }
        return new CataloguePickerPopover.Entries() {

            private List<String> cached;

            @Override
            public List<String> values() {
                if (this.cached == null) {
                    final List<String> fitting = new ArrayList<>();
                    for (final String value : ITEM_ENTRIES.values()) {
                        if (ItemInventory.accepts(slot, value)) {
                            fitting.add(value);
                        }
                    }
                    this.cached = List.copyOf(fitting);
                }
                return this.cached;
            }

            @Override
            public String label(final String value) {
                return ITEM_ENTRIES.label(value);
            }

            @Override
            public ListIcon icon(final String value) {
                return ITEM_ENTRIES.icon(value);
            }

            @Override
            public String itemId(final String value) {
                return value;
            }
        };
    }

    private PopoverHost.PopoverContent slotEditor(final int slot) {
        return new ItemEditorPopover(() -> this.value().get(slot), mutation -> this.mutate(inventory -> {
            final ItemSpec spec = inventory.get(slot);
            if (spec != null) {
                mutation.accept(spec);
            }
        }), () -> this.mutate(inventory -> inventory.clear(slot)), this.ctx.onClosePopover(), () -> this.releaseSlot(slot), () -> this.slotOriginStyle(slot), slot, this.uiScale, FADE_KEYS.addAndGet(64) - 63, PopoverHost.PANEL_LAYER, NESTED_PICKER_LAYER, NESTED_NBT_LAYER).withFixedTriggerHandoff((graphics, handoff) -> this.trackSlotHandoff(slot, handoff));
    }

    private void releaseSlot(final int slot) {
        if (this.activeSlot == slot) {
            this.activeSlot = -1;
            this.activeSlotFade = 1f;
        }
    }

    private void trackSlotHandoff(final int slot, final float handoff) {
        if (this.activeSlot == slot) {
            this.activeSlotFade = Mth.clamp(handoff, 0f, 1f);
        }
    }

    private GlassStyle slotOriginStyle(final int slot) {
        return clearOriginStyle(PopoverHost.PANEL_LAYER, this.uiScale);
    }

    public static GlassStyle clearOriginStyle(final int layer, final float uiScale) {
        return new GlassStyle()
            .layer(layer)
            .tint(Theme.get().shadowColor(), 0f)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .refractionReach(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .edgeDarken(0f)
            .brightEdge(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f)
            .scaled(uiScale);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.cursor.cancel();
        }
    }

    @Override
    protected float rowLabelHeight() {
        return Math.min(this.getHeight(), OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale);
    }

    @Override
    public void setWidth(final int width) {
        super.setWidth(width);
        this.setHeight(Math.round(this.preferredHeight()));
    }
}
