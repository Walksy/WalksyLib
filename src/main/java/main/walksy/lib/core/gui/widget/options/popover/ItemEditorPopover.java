package main.walksy.lib.core.gui.widget.options.popover;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.render.element.WLRect;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.PopoverStyle;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ItemSpec;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.SearchBar;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.texture.ItemModels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.LegacyText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import main.walksy.lib.core.gui.widget.options.types.InventoryOptionWidget;
import main.walksy.lib.core.gui.widget.options.control.SliderControl;
import main.walksy.lib.core.gui.widget.options.control.SlotCursor;
import main.walksy.lib.core.gui.widget.options.control.TrashSlot;
import main.walksy.lib.core.gui.widget.options.control.ToggleControl;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.ScrollGlide;
import net.minecraft.util.Mth;

public final class ItemEditorPopover implements PopoverHost.PopoverContent {
    private static final float WIDTH = 280f;
    private static final float PAD = 14f;
    private static final float MAX_BODY_HEIGHT = 250f;
    private static final float ICON_SIZE = 28f;
    private static final float ROW_H = 22f;
    private static final float SECTION_GAP = 12f;
    private static final float ENTRY_H = 16f;
    private static final float ENTRY_GAP = 4f;
    private static final float ENTRY_STRIDE = ENTRY_H + ENTRY_GAP;
    private static final float BUTTON_SIZE = 14f;
    private static final float BUTTON_GAP = 6f;
    private static final float SECTION_HEADER_GAP = 5f;
    private static final float SWATCH_SIZE = 11f;
    private static final float SWATCH_GAP = 2f;
    private static final float CARD_RADIUS = 8f;
    private static final float SCROLL_STEP = 34f;
    private static final float SCROLL_GLIDE_SPEED = 18f;
    private static final float SCROLLBAR_W = 4f;
    private static final float SCROLLBAR_GRAB = 4f;
    private static final float SCROLLBAR_GAP = 5f;
    private static final float BODY_CLIP_LIFT = 5f;
    private static final float SECTION_LABEL_SIZE = Type.FOOTNOTE.size();
    private static final float CAPTION_SIZE = Type.FOOTNOTE.size();
    private static final float ICON_RATIO = 0.42f;
    private static final int SECTIONS = 3;
    static final float SMALL_BUTTON_REFRACTION = 4f;
    private static final SliderControl.Metrics CARD_SLIDER = SliderControl.Metrics.row().withFieldCapHeight(0.59043f);
    private final int panelLayer;
    private final int cardLayer;
    private final int controlLayer;
    private final int fieldLayer;
    private final int buttonLayer;
    private final int headPillLayer;
    private float contentsGridX;
    private float contentsGridY;
    private final PopoverHost pickerHost;
    private final PopoverHost nbtHost;
    private final Supplier<ItemSpec> spec;
    private final Consumer<Consumer<ItemSpec>> edit;
    private final Runnable onRemove;
    private final Runnable requestClose;
    private final Runnable onClosedCallback;
    private final Supplier<GlassStyle> originStyle;
    private final int slot;
    private final String slotName;
    private final float uiScale;
    private final long fadeKeyBase;
    private final ToggleControl unbreakableToggle;
    private final TextFieldControl nameField;
    private final WLButton changeItemButton;
    private final WLButton removeItemButton;
    private final WLButton addLoreButton;
    private final WLButton addEnchantButton;
    private final WLButton editNbtButton;
    private final GlassStyle cardStyle;
    private SliderControl durabilitySlider;
    private SliderControl countSlider;
    private String slidersItemId;
    private final List<TextFieldControl> loreFields = new ArrayList<>();
    private final List<WLButton> loreRemoveButtons = new ArrayList<>();
    private final Map<String, TextFieldControl> enchantLevelFields = new HashMap<>();
    private final Map<String, WLButton> enchantRemoveButtons = new HashMap<>();
    private float scrollY;
    private float scrollTarget;
    private final ScrollGlide glide = new ScrollGlide();
    private boolean draggingThumb;
    private float thumbGrabOffset;
    private SliderControl draggingSlider;
    private int lastPanelX;
    private int lastPanelY;
    private int lastPanelW;
    private int lastPanelH;
    private float lastBodyTop;
    private float lastBodyHeight;
    private float lastThumbX;
    private float lastBodyClipTop;
    private float lastBodyClipBottom;
    private WLButton activeTrigger;
    private int activeContentsSlot = -1;
    private float activeContentsFade = 1f;
    private static final SlotCursor.Rules CONTENTS_RULES = new SlotCursor.Rules() {
        @Override
        public boolean accepts(final int slot, final ItemSpec spec) {
            return true;
        }

        @Override
        public int limit(final int slot, final ItemSpec spec) {
            return spec.maxStackSize();
        }
    };
    private final SlotCursor contentsCursor = new SlotCursor();
    private final TrashSlot contentsTrash = new TrashSlot();
    private float contentsMouseX;
    private float contentsMouseY;
    private int pressedContentsSlot = -1;
    private boolean draggingContents;
    private double contentsPressX;
    private double contentsPressY;
    private double contentsDragX;
    private double contentsDragY;
    private TextFieldControl paletteTarget;
    private float paletteX;
    private float paletteY;
    private BiConsumer<WLGraphics2D, Float> triggerHandoff;
    private boolean handoffRides = true;
    private final CataloguePickerPopover.Entries enchantEntries = new CataloguePickerPopover.Entries() {

        private String key;
        private List<String> cached = List.of();

        @Override
        public List<String> values() {
            final ItemSpec item = ItemEditorPopover.this.current();
            final String signature = item.getItemId() + item.getEnchantments();
            if (signature.equals(this.key)) {
                return this.cached;
            }
            final List<String> applicable = new ArrayList<>(item.applicableEnchantments());
            applicable.sort(Comparator.comparing(id -> ItemSpec.enchantmentDisplayName(id).toLowerCase(Locale.ROOT)));
            final List<String> rows = new ArrayList<>();
            for (final String id : applicable) {
                final int max = Math.max(1, ItemSpec.enchantmentMaxLevel(id));
                final int applied = item.getEnchantmentLevel(id);
                final int ceiling = Math.max(max, applied);
                for (int level = 1; level <= ceiling; level++) {
                    rows.add(id + LEVEL_SEP + level);
                }
            }
            this.key = signature;
            this.cached = List.copyOf(rows);
            return this.cached;
        }

        @Override
        public String label(final String value) {
            return ItemSpec.enchantmentDisplayName(enchantIdOf(value)) + " " + ItemSpec.romanLevel(enchantLevelOf(value));
        }

        @Override
        public String trailing(final String value) {
            final String id = enchantIdOf(value);
            return enchantLevelOf(value) > ItemSpec.enchantmentMaxLevel(id) ? "beyond max" : null;
        }

        @Override
        public String emptyMessage() {
            return ItemEditorPopover.this.current().enchantmentUnavailableReason();
        }
    };
    private static final char LEVEL_SEP = '#';
    private static final int CONTAINER_COLUMNS = 9;
    private static final float SLOT_SIZE = 18f;
    private static final float SLOT_GAP = 2f;
    private static final float SLOT_RADIUS = 3f;
    private static final float TRASH_GAP = 6f;

    public ItemEditorPopover(final Supplier<ItemSpec> spec, final Consumer<Consumer<ItemSpec>> edit, final Runnable onRemove, final Runnable requestClose, final Runnable onClosedCallback, final Supplier<GlassStyle> originStyle, final int slot, final float uiScale, final long fadeKeyBase, final int panelLayer, final int pickerPanelLayer, final int nbtPanelLayer) {
        this.spec = spec;
        this.edit = edit;
        this.onRemove = onRemove;
        this.requestClose = requestClose;
        this.onClosedCallback = onClosedCallback;
        this.originStyle = originStyle;
        this.slot = slot;
        this.slotName = InventoryOptionWidget.slotName(slot);
        this.uiScale = uiScale;
        this.fadeKeyBase = fadeKeyBase;
        final int content = panelLayer + 1;
        this.panelLayer = panelLayer;
        this.cardLayer = content;
        this.controlLayer = content + 1;
        this.fieldLayer = content + 5;
        this.buttonLayer = content + 6;
        this.headPillLayer = content + 7;
        this.pickerHost = new PopoverHost(pickerPanelLayer);
        this.nbtHost = new PopoverHost(nbtPanelLayer);
        this.cardStyle = cardStyle().layer(this.cardLayer).fadeKey(fadeKeyBase);
        this.unbreakableToggle = new ToggleControl(() -> this.current().isUnbreakable(), uiScale, fadeKeyBase + 20, v -> this.edit(s -> s.setUnbreakable(v)), this.controlLayer);
        this.nameField = new TextFieldControl(this.current().getCustomName(), text -> this.edit(s -> s.setCustomName(text)), (Supplier<String>) () -> this.current().itemDisplayName(), SearchBar.defaultStyle().layer(this.fieldLayer).fadeKey(fadeKeyBase + 30), uiScale);
        this.nameField.setMaxLength(128);
        this.changeItemButton = new WLButton(0, 0, 1, 1, "Change Item", b -> this.openItemPicker());
        this.removeItemButton = new WLButton(0, 0, 1, 1, "Remove", b -> {
            if (this.onRemove != null) {
                this.onRemove.run();
            }
            if (this.requestClose != null) {
                this.requestClose.run();
            }
        });
        for (final WLButton button : new WLButton[]{this.changeItemButton, this.removeItemButton}) {
            button.style().layer(this.headPillLayer).fadeKey(FadeKeys.next()).refractionThickness(SMALL_BUTTON_REFRACTION);
        }
        this.addLoreButton = WLButton.icon(0, 0, 1, 1, b -> this.edit(s -> s.addLoreLine("")));
        this.addEnchantButton = WLButton.icon(0, 0, 1, 1, b -> this.openEnchantPicker());
        for (final WLButton button : new WLButton[]{this.addLoreButton, this.addEnchantButton}) {
            button.style().layer(this.buttonLayer).fadeKey(FadeKeys.next());
        }
        this.editNbtButton = new WLButton(0, 0, 1, 1, "Edit NBT", b -> this.openNbtEditor());
        this.editNbtButton.style()
            .layer(this.headPillLayer)
            .fadeKey(FadeKeys.next())
            .refractionThickness(SMALL_BUTTON_REFRACTION * 0.5f);
    }

    private static final class FadeKeys {
        private static final AtomicLong COUNTER = new AtomicLong(0x17E33DL);

        static long next() {
            return COUNTER.incrementAndGet();
        }
}

    private ItemSpec current() {
        final ItemSpec live = this.spec.get();
        return live == null ? new ItemSpec("minecraft:air") : live;
    }

    private void edit(final Consumer<ItemSpec> mutation) {
        if (this.edit != null) {
            this.edit.accept(mutation);
        }
    }

    private float s() {
        return this.uiScale;
    }

    private boolean showsDurability() {
        return this.current().isDamageable();
    }

    private boolean showsUnbreakable() {
        return this.current().isDamageable();
    }

    private boolean showsCount() {
        return this.current().maxStackSize() > 1;
    }

    private int propertyRowCount() {
        return (this.showsCount() ? 1 : 0) + (this.showsDurability() ? 1 : 0) + (this.showsUnbreakable() ? 1 : 0);
    }

    private float loreCount() {
        return this.current().getLore().size();
    }

    private float enchantCount() {
        return this.current().getEnchantments().size();
    }

    private boolean showsContents() {
        return this.current().holdsContainer();
    }

    private int containerRows() {
        return (ItemSpec.CONTAINER_SIZE + CONTAINER_COLUMNS - 1) / CONTAINER_COLUMNS;
    }

    private float containerGridHeight() {
        final float s = this.s();
        return this.containerRows() * SLOT_SIZE * s + (this.containerRows() - 1) * SLOT_GAP * s;
    }

    private float containerSectionHeight() {
        return this.sectionLabelHeight() + this.containerGridHeight();
    }

    private float extractContentsSection(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float dtSeconds, final float alpha) {
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().beginPopover(this.panelLayer);
        try {
            return this.extractContentsGrid(graphics, x0, y, mouseX, mouseY, dtSeconds, alpha);
        } finally {
            WLRenderSystem.contentStratum().endPopover();
            WLRenderSystem.markPopoverText(textMark);
        }
    }

    private float extractContentsGrid(final WLGraphics2D graphics, final float x0, final float y, final int mouseX, final int mouseY, final float dtSeconds, final float alpha) {
        final float s = this.s();
        float cursorY = this.extractSectionLabel(graphics, "Contents", x0, y, alpha);
        final List<ItemSpec> contents = this.current().containerContents();
        this.contentsCursor.validate(contents);
        final float slot = SLOT_SIZE * s;
        final float gap = SLOT_GAP * s;
        for (int index = 0; index < ItemSpec.CONTAINER_SIZE; index++) {
            final float sx = x0 + (index % CONTAINER_COLUMNS) * (slot + gap);
            final float sy = cursorY + (index / CONTAINER_COLUMNS) * (slot + gap);
            final boolean active = index == this.activeContentsSlot;
            final float showing = active ? alpha * Mth.clamp(this.activeContentsFade, 0f, 1f) : alpha;
            if (showing <= 0.004f) {
                continue;
            }
            final boolean hovered = !active && !this.nbtHost.isOpen() && !this.pickerHost.isOpen() && (mouseX >= sx && mouseX < sx + slot && mouseY >= sy && mouseY < sy + slot || this.contentsCursor.spreadsOver(index));
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            final int fill = ColorUtils.withAlpha(hovered ? this.style().accent() : this.style().separator(), showing * (hovered ? 0.35f : 0.6f));
            graphics.fillRoundedGradient(sx, sy, sx + slot, sy + slot, fill, fill, fill, fill, SLOT_RADIUS * s);
            final ItemSpec inner = this.contentsCursor.shown(index, contents.get(index));
            if (inner != null && !(this.draggingContents && index == this.pressedContentsSlot)) {
                ItemModels.extractFaded(graphics, inner, sx + 1f * s, sy + 1f * s, slot - 2f * s, this.cardLayer, showing);
            }
        }
        this.contentsTrash.layout(x0 + CONTAINER_COLUMNS * (slot + gap) - gap + TRASH_GAP * s, cursorY + (this.containerRows() - 1) * (slot + gap), slot);
        final boolean armed = this.contentsTrashArmed();
        final boolean trashHovered = !this.nbtHost.isOpen() && !this.pickerHost.isOpen() && this.overContentsTrash(mouseX, mouseY);
        this.contentsTrash.extract(graphics, SLOT_RADIUS * s, Math.max(1f, (trashHovered && armed ? 1.5f : 1f) * s), trashHovered, armed, alpha, dtSeconds);
        return cursorY + this.containerGridHeight();
    }

    private void extractContentsDragGhost(final WLGraphics2D graphics, final float alpha) {
        if (!this.draggingContents || this.pressedContentsSlot < 0 || !this.showsContents()) {
            return;
        }
        final ItemSpec spec = this.current().containerContents().get(this.pressedContentsSlot);
        if (spec == null) {
            return;
        }
        final float size = (SLOT_SIZE - 2f) * this.s();
        ItemModels.extractFaded(graphics, spec, (float) this.contentsDragX - size / 2f, (float) this.contentsDragY - size / 2f, size, this.cardLayer, alpha * 0.85f);
    }

    private void extractContentsCarried(final WLGraphics2D graphics, final float alpha) {
        if (!this.showsContents()) {
            return;
        }
        final ItemSpec carried = this.contentsCursor.carried(this.current().containerContents());
        if (carried == null) {
            return;
        }
        final float size = (SLOT_SIZE - 2f) * this.s();
        ItemModels.extractFaded(graphics, carried, this.contentsMouseX - size / 2f, this.contentsMouseY - size / 2f, size, this.cardLayer, alpha);
    }

    private boolean contentsTrashArmed() {
        return this.contentsCursor.holding() || (this.draggingContents && this.pressedContentsSlot >= 0);
    }

    private boolean overContentsTrash(final double x, final double y) {
        return this.showsContents() && y >= this.lastBodyTop && y < this.lastBodyClipBottom && this.contentsTrash.contains(x, y);
    }

    private void commitContents(final List<ItemSpec> contents) {
        this.edit(owner -> owner.setContainerContents(contents));
    }

    private void dropContents(final int from, final int to) {
        this.edit(owner -> {
            final List<ItemSpec> contents = owner.containerContents();
            ItemSpec.drop(contents, from, to);
            owner.setContainerContents(contents);
        });
    }

    private int contentsSlotAt(final double x, final double y) {
        if (!this.showsContents()) {
            return -1;
        }
        final float s = this.s();
        final float stride = (SLOT_SIZE + SLOT_GAP) * s;
        final float slot = SLOT_SIZE * s;
        for (int index = 0; index < ItemSpec.CONTAINER_SIZE; index++) {
            final float sx = this.contentsGridX + (index % CONTAINER_COLUMNS) * stride;
            final float sy = this.contentsGridY + (index / CONTAINER_COLUMNS) * stride;
            if (x >= sx && x < sx + slot && y >= sy && y < sy + slot) {
                return index;
            }
        }
        return -1;
    }

    private void openContentsSlot(final int index) {
        if (this.activeContentsSlot == index) {
            return;
        }
        final ItemSpec existing = this.current().containerContents().get(index);
        final PopoverHost.PopoverContent sheet = existing == null ? this.contentsSlotPicker(index) : this.contentsSlotEditor(index, existing);
        final float s = this.s();
        final float stride = (SLOT_SIZE + SLOT_GAP) * s;
        final float slot = SLOT_SIZE * s;
        final LiquidRect anchor = new LiquidRect(this.contentsGridX + (index % CONTAINER_COLUMNS) * stride, this.contentsGridY + (index / CONTAINER_COLUMNS) * stride, slot, slot, SLOT_RADIUS * s);
        this.activeTrigger = null;
        this.activeContentsSlot = index;
        this.activeContentsFade = 1f;
        this.openBeside(this.nbtHost, sheet, anchor, true);
    }

    private PopoverHost.PopoverContent contentsSlotPicker(final int index) {
        return new CataloguePickerPopover("Add to Slot " + (index + 1), InventoryOptionWidget.entriesFor(ItemInventory.STORAGE_START), value -> false, itemId -> this.writeContentsSlot(index, new ItemSpec(itemId)), true, this.nbtHost::close, () -> this.releaseContentsSlot(index), () -> InventoryOptionWidget.clearOriginStyle(this.nbtHost.panelLayer(), this.uiScale), this.uiScale, this.nbtHost.panelLayer())
            .withFixedTriggerHandoff((graphics, handoff) -> this.trackContentsHandoff(index, handoff));
    }

    private PopoverHost.PopoverContent contentsSlotEditor(final int index, final ItemSpec working) {
        return new ItemEditorPopover(() -> working, mutation -> {
            mutation.accept(working);
            this.writeContentsSlot(index, working);
        }, () -> {
            this.writeContentsSlot(index, null);
            this.nbtHost.close();
        }, this.nbtHost::close, () -> this.releaseContentsSlot(index), () -> InventoryOptionWidget.clearOriginStyle(this.nbtHost.panelLayer(), this.uiScale), index, this.uiScale, FadeKeys.next(), this.nbtHost.panelLayer(), this.nbtHost.panelLayer() + 10, this.nbtHost.panelLayer() + 20)
            .withFixedTriggerHandoff((graphics, handoff) -> this.trackContentsHandoff(index, handoff));
    }

    private void releaseContentsSlot(final int index) {
        if (this.activeContentsSlot == index) {
            this.activeContentsSlot = -1;
            this.activeContentsFade = 1f;
        }
    }

    private void trackContentsHandoff(final int index, final float handoff) {
        if (this.activeContentsSlot == index) {
            this.activeContentsFade = Mth.clamp(handoff, 0f, 1f);
        }
    }

    private void writeContentsSlot(final int index, final ItemSpec value) {
        this.edit(owner -> {
            final List<ItemSpec> contents = owner.containerContents();
            contents.set(index, value);
            owner.setContainerContents(contents);
        });
    }

    private float bodyContentHeight() {
        final float s = this.s();
        final int rows = this.propertyRowCount();
        float h = rows == 0 ? 0f : rows * ROW_H * s + SECTION_GAP * s;
        h += this.sectionLabelHeight() + ENTRY_H * s + ENTRY_GAP * s + (SWATCH_SIZE + 2f) * s;
        h += SECTION_GAP * s + this.sectionHeaderHeight() + (this.loreCount() == 0 ? this.captionHeight() : this.loreCount() * ENTRY_STRIDE * s);
        h += SECTION_GAP * s + this.sectionHeaderHeight() + (this.enchantCount() == 0 ? this.captionHeight() : this.enchantCount() * ENTRY_STRIDE * s);
        if (this.showsContents()) {
            h += SECTION_GAP * s + this.containerSectionHeight();
        }
        h += SECTION_GAP * s + ENTRY_H * s;
        return h + 4f * s;
    }

    private float sectionLabelHeight() {
        return (SECTION_LABEL_SIZE + 6f) * this.s();
    }

    private float sectionHeaderHeight() {
        return Math.max(this.sectionLabelHeight(), BUTTON_SIZE * this.s()) + SECTION_HEADER_GAP * this.s();
    }

    private float captionHeight() {
        return (CAPTION_SIZE + 6f) * this.s();
    }

    private float headHeight() {
        return (ICON_SIZE + 6f + ENTRY_H + 8f) * this.s();
    }

    @Override
    public int preferredWidth() {
        return Math.round(WIDTH * this.s());
    }

    @Override
    public int preferredHeight() {
        final float body = Math.min(MAX_BODY_HEIGHT * this.s(), this.bodyContentHeight());
        return (int) Math.ceil(PAD * this.s() + this.headHeight() + body + PAD * this.s());
    }

    @Override
    public boolean growFromTop() {
        return true;
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
    public float lensScale() {
        return 0.6f;
    }

    @Override
    public void onClosed() {
        if (this.onClosedCallback != null) {
            this.onClosedCallback.run();
        }
    }

    public ItemEditorPopover withTriggerHandoff(final BiConsumer<WLGraphics2D, Float> drawer) {
        this.triggerHandoff = drawer;
        return this;
    }

    public ItemEditorPopover withFixedTriggerHandoff(final BiConsumer<WLGraphics2D, Float> drawer) {
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

    @Override
    public void onCloseBegan() {
        this.pickerHost.close();
        this.nbtHost.close();
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
        this.lastPanelX = panelX;
        this.lastPanelY = panelY;
        this.contentsMouseX = mouseX;
        this.contentsMouseY = mouseY;
        this.lastPanelW = panelW;
        this.lastPanelH = panelH;
        final float s = this.s();
        final var pose = graphics.pose();
        final float cx = panelX + panelW / 2f;
        final float dtSeconds = delta / 20f;
        this.syncControls();
        this.extractNestedHost(graphics, this.pickerHost, mouseX, mouseY, delta);
        this.extractNestedHost(graphics, this.nbtHost, mouseX, mouseY, delta);
        if (!this.pickerHost.isOpen() && !this.nbtHost.isOpen()) {
            this.activeTrigger = null;
            this.activeContentsSlot = -1;
            this.activeContentsFade = 1f;
        }
        final float headTop = panelY + PAD * s;
        float a = Materialize.enter(pose, Materialize.stagger(reveal, 0, SECTIONS), cx, headTop + ICON_SIZE * s / 2f, s);
        this.extractHead(graphics, panelX, headTop, panelW, a);
        Materialize.exit(pose);
        final float actionsY = headTop + (ICON_SIZE + 6f) * s;
        a = Materialize.enter(pose, Materialize.stagger(reveal, 1, SECTIONS), cx, actionsY + ENTRY_H * s / 2f, s);
        this.extractActions(graphics, panelX, actionsY, panelW, mouseX, mouseY, delta, a);
        Materialize.exit(pose);
        this.extractBody(graphics, panelX, panelY, panelW, panelH, mouseX, mouseY, delta, dtSeconds, Materialize.stagger(reveal, 2, SECTIONS));
    }

    private void extractNestedHost(final WLGraphics2D graphics, final PopoverHost host, final int mouseX, final int mouseY, final float delta) {
        final WLRect outer = graphics.scissor();
        graphics.disableScissor();
        host.extract(graphics, mouseX, mouseY, delta);
        if (outer != null) {
            graphics.enableScissor(outer.left(), outer.top(), outer.left() + outer.width(), outer.top() + outer.height());
        }
    }

    private void extractHead(final WLGraphics2D graphics, final int panelX, final float top, final int panelW, final float alpha) {
        final float s = this.s();
        final ItemSpec item = this.current();
        final float iconSize = ICON_SIZE * s;
        final float x0 = panelX + PAD * s;
        final int well = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().shadowColor(), 0.22f), alpha);
        graphics.fillRoundedGradient(x0, top, x0 + iconSize, top + iconSize, well, well, well, well, 6f * s);
        ItemModels.extractOrFallback(graphics, item, x0 + 3f * s, top + 3f * s, iconSize - 6f * s, this.panelLayer, alpha);
        final float textX = x0 + iconSize + 8f * s;
        final float textRight = panelX + panelW - PAD * s;
        final float titleSize = OptionRowWidget.LABEL_SIZE * s;
        final float subSize = CAPTION_SIZE * s;
        final String title = Fonts.SFPRO_DISPLAY.truncate(item.displayName(), titleSize, textRight - textX);
        final int titleColor = item.hasCustomName() ? LegacyText.colorOf(item.getCustomName(), OptionRowWidget.LABEL_COLOR()) : OptionRowWidget.LABEL_COLOR();
        final float titleBaseline = top + iconSize * 0.42f;
        Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, title, textX, Fonts.SFPRO_DISPLAY_BOLD.topForBaseline(titleBaseline, titleSize), titleSize, ColorUtils.withAlpha(titleColor, alpha));
        final String subtitle = this.slotName == null || this.slotName.isEmpty() ? item.getItemId() : this.slotName + " · " + item.getItemId();
        Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(subtitle, subSize, textRight - textX), textX, Fonts.SFPRO_DISPLAY.topForBaseline(top + iconSize * 0.82f, subSize), subSize, ColorUtils.withAlpha(this.style().tertiaryLabel(), alpha));
    }

    private void extractActions(final WLGraphics2D graphics, final int panelX, final float y, final int panelW, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float s = this.s();
        final float h = ENTRY_H * s;
        final float x0 = panelX + PAD * s;
        final float x1 = panelX + panelW - PAD * s;
        final float gap = BUTTON_GAP * s;
        final float half = (x1 - x0 - gap) / 2f;
        this.layoutPill(this.changeItemButton, x0, y, half, h, alpha);
        this.layoutPill(this.removeItemButton, x0 + half + gap, y, half, h, alpha);
        this.extractTrigger(graphics, this.changeItemButton, mouseX, mouseY, delta);
        this.extractTrigger(graphics, this.removeItemButton, mouseX, mouseY, delta);
    }

    private void extractBody(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float dtSeconds, final float reveal) {
        final float s = this.s();
        final float top = panelY + PAD * s + this.headHeight();
        final float bottom = panelY + panelH - PAD * s;
        if (bottom - top <= 1f) {
            return;
        }
        this.lastBodyTop = top;
        this.lastBodyClipTop = top - BODY_CLIP_LIFT * s;
        this.lastBodyHeight = bottom - top;
        this.lastBodyClipBottom = panelY + panelH;
        this.lastThumbX = panelX + panelW - PAD * s + SCROLLBAR_GAP * s;
        final float max = this.maxScroll();
        this.scrollTarget = Mth.clamp(this.scrollTarget, 0f, max);
        this.scrollY = this.draggingThumb ? this.scrollTarget : Mth.clamp(this.glide.step(this.scrollY, this.scrollTarget, SCROLL_GLIDE_SPEED, dtSeconds), 0f, max);
        graphics.enableScissor(panelX, Math.round(this.lastBodyClipTop), panelX + panelW, Math.round(this.lastBodyClipBottom));
        try {
            this.extractSections(graphics, panelX, panelW, top - ScrollGlide.shown(this.scrollY), mouseX, mouseY, delta, dtSeconds, reveal);
        } finally {
            graphics.disableScissor();
        }
        this.extractContentsDragGhost(graphics, reveal);
        this.extractContentsCarried(graphics, reveal);
        this.extractScrollbar(graphics, reveal);
    }

    private void extractSections(final WLGraphics2D graphics, final int panelX, final int panelW, final float y, final int mouseX, final int mouseY, final float delta, final float dtSeconds, final float alpha) {
        final float s = this.s();
        final float x0 = panelX + PAD * s;
        final float x1 = panelX + panelW - PAD * s;
        float cursorY = y;
        final float afterCard = this.extractPropertiesCard(graphics, x0, x1, cursorY, mouseX, mouseY, delta, dtSeconds, alpha);
        cursorY = afterCard > cursorY ? afterCard + SECTION_GAP * s : afterCard;
        cursorY = this.extractNameSection(graphics, x0, x1, cursorY, mouseX, mouseY, delta, alpha);
        cursorY += SECTION_GAP * s;
        cursorY = this.extractLoreSection(graphics, x0, x1, cursorY, mouseX, mouseY, delta, alpha);
        cursorY += SECTION_GAP * s;
        cursorY = this.extractEnchantSection(graphics, x0, x1, cursorY, mouseX, mouseY, delta, dtSeconds, alpha);
        if (this.showsContents()) {
            cursorY += SECTION_GAP * s;
            cursorY = this.extractContentsSection(graphics, x0, x1, cursorY, mouseX, mouseY, dtSeconds, alpha);
        }
        this.contentsGridX = x0;
        this.contentsGridY = cursorY - this.containerGridHeight();
        cursorY += SECTION_GAP * s;
        this.extractNbtButton(graphics, x0, x1, cursorY, mouseX, mouseY, delta, alpha);
    }

    private void extractNbtButton(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float s = this.s();
        final float h = ENTRY_H * s;
        final float textSize = Math.max(Type.CAPTION2.size() * s, h * 0.46f);
        final float w = Fonts.SFPRO_DISPLAY.width(this.editNbtButton.getLabel(), textSize) + 14f * s;
        this.layoutPill(this.editNbtButton, x0, y, w, h, alpha);
        this.extractTrigger(graphics, this.editNbtButton, mouseX, mouseY, delta);
    }

    private float extractPropertiesCard(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float dtSeconds, final float alpha) {
        final float s = this.s();
        final float rowH = ROW_H * s;
        final int rows = this.propertyRowCount();
        if (rows == 0) {
            return y;
        }
        final float cardH = rows * rowH;
        graphics.glass()
            .dimensions(x0, y, x1 - x0, cardH)
            .cornerRadius(CARD_RADIUS * s)
            .style(this.cardStyle.scaled(s).opacity(alpha))
            .extract();
        final float hair = WLScreen.hairlinePx();
        final float rowInset = 10f * s;
        for (int i = 1; i < rows; i++) {
            final float sy = y + i * rowH;
            graphics.fill(x0 + rowInset, sy, x1 - rowInset, sy + hair, ColorUtils.withAlpha(this.style().separator(), alpha), null);
        }
        final float labelSize = Type.SUBHEADLINE.size() * s;
        final float textX = x0 + rowInset;
        final float ctrlX1 = x1 - rowInset;
        final float ctrlX0 = x0 + (x1 - x0) * 0.42f;
        float rowY = y;
        if (this.showsCount()) {
            this.extractRowLabel(graphics, "Count", textX, rowY, rowH, labelSize, alpha);
            this.countSlider().extractInRow(graphics, dtSeconds, ctrlX0, ctrlX1, rowY, rowH, alpha, mouseX, mouseY, delta);
            rowY += rowH;
        }
        if (this.showsDurability()) {
            this.extractRowLabel(graphics, "Durability", textX, rowY, rowH, labelSize, alpha);
            this.durabilitySlider().extractInRow(graphics, dtSeconds, ctrlX0, ctrlX1, rowY, rowH, alpha, mouseX, mouseY, delta);
            rowY += rowH;
        }
        if (this.showsUnbreakable()) {
            this.extractRowLabel(graphics, "Unbreakable", textX, rowY, rowH, labelSize, alpha);
            this.unbreakableToggle.extract(graphics, dtSeconds, alpha, ctrlX1, rowY, rowH);
            rowY += rowH;
        }
        return y + cardH;
    }

    private float extractNameSection(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float s = this.s();
        float cursorY = this.extractSectionLabel(graphics, "Display Name", x0, y, alpha);
        this.nameField.syncText(this.current().getCustomName());
        this.nameField.extract(graphics, x0, cursorY, x1 - x0, ENTRY_H * s, mouseX, mouseY, delta, alpha, true, true);
        WLRenderSystem.cursors().requestIf(this.nameField.isOver(mouseX, mouseY), Cursors.Cursor.TEXT);
        cursorY += (ENTRY_H + ENTRY_GAP) * s;
        this.extractColorPalette(graphics, x0, cursorY, mouseX, mouseY, alpha);
        return cursorY + (SWATCH_SIZE + 2f) * s;
    }

    private void extractColorPalette(final WLGraphics2D graphics, final float x0, final float y, final int mouseX, final int mouseY, final float alpha) {
        final float s = this.s();
        final float size = SWATCH_SIZE * s;
        this.paletteX = x0;
        this.paletteY = y;
        final float stride = size + SWATCH_GAP * s;
        final List<LegacyText.Swatch> swatches = LegacyText.colorSwatches();
        for (int i = 0; i < swatches.size(); i++) {
            final float sx = x0 + i * stride;
            final boolean hovered = mouseX >= sx && mouseX < sx + size && mouseY >= y && mouseY < y + size;
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            final int color = ColorUtils.withAlpha(swatches.get(i).argb(), alpha);
            final float grow = hovered ? 1f * s : 0f;
            graphics.fillRoundedGradient(sx - grow, y - grow, sx + size + grow, y + size + grow, color, color, color, color, (size + grow * 2f) * 0.3f);
        }
        final float rx = x0 + swatches.size() * stride;
        final boolean resetHovered = mouseX >= rx && mouseX < rx + size && mouseY >= y && mouseY < y + size;
        WLRenderSystem.cursors().requestIf(resetHovered, Cursors.Cursor.POINTER);
        final int resetColor = ColorUtils.withAlpha(ColorUtils.withAlpha(this.style().label(), resetHovered ? 0.5f : 0.28f), alpha);
        graphics.fillRoundedGradient(rx, y, rx + size, y + size, resetColor, resetColor, resetColor, resetColor, size * 0.3f);
        final float markSize = size * 0.5f;
        Icons.PLUS.extractCentered(graphics, rx + size / 2f, y + size / 2f, markSize, ColorUtils.withAlpha(this.style().label(), alpha), 45f);
    }

    private float extractLoreSection(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float s = this.s();
        final List<String> lore = this.current().getLore();
        this.syncLoreControls(lore.size());
        final float buttonSize = BUTTON_SIZE * s;
        this.extractSectionHeader(graphics, "Lore", x0, x1, y, this.addLoreButton, Icons.PLUS, 0f, lore.size() < ItemSpec.MAX_LORE_LINES, mouseX, mouseY, delta, alpha);
        float cursorY = y + this.sectionHeaderHeight();
        if (lore.isEmpty()) {
            this.extractCaption(graphics, "No lore lines", x0, cursorY, alpha);
            return cursorY + this.captionHeight();
        }
        final float fieldX1 = x1 - buttonSize - BUTTON_GAP * s;
        for (int i = 0; i < lore.size(); i++) {
            final TextFieldControl field = this.loreFields.get(i);
            field.syncText(lore.get(i));
            field.extract(graphics, x0, cursorY, fieldX1 - x0, ENTRY_H * s, mouseX, mouseY, delta, alpha, true, true);
            WLRenderSystem.cursors().requestIf(field.isOver(mouseX, mouseY), Cursors.Cursor.TEXT);
            final WLButton remove = this.loreRemoveButtons.get(i);
            this.layoutRoundButton(remove, fieldX1 + BUTTON_GAP * s, cursorY + (ENTRY_H * s - buttonSize) / 2f, buttonSize, alpha);
            graphics.widget(remove, mouseX, mouseY, delta);
            this.extractButtonIcon(graphics, Icons.MINUS, remove, alpha, 0f);
            cursorY += ENTRY_STRIDE * s;
        }
        return cursorY - ENTRY_GAP * s;
    }

    private float extractEnchantSection(final WLGraphics2D graphics, final float x0, final float x1, final float y, final int mouseX, final int mouseY, final float delta, final float dtSeconds, final float alpha) {
        final float s = this.s();
        final Map<String, Integer> enchantments = new LinkedHashMap<>(this.current().getEnchantments());
        this.syncEnchantControls(enchantments);
        this.extractSectionHeader(graphics, "Enchantments", x0, x1, y, this.addEnchantButton, Icons.PLUS, 0f, true, mouseX, mouseY, delta, alpha);
        float cursorY = y + this.sectionHeaderHeight();
        if (enchantments.isEmpty()) {
            this.extractCaption(graphics, ItemSpec.enchantmentRegistry() == null ? "Enchantments are unavailable right now" : "No enchantments", x0, cursorY, alpha);
            return cursorY + this.captionHeight();
        }
        final float buttonSize = BUTTON_SIZE * s;
        final float rowH = ENTRY_H * s;
        final float labelSize = Type.SUBHEADLINE.size() * s;
        for (final Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            final String id = entry.getKey();
            final float removeX = x1 - buttonSize;
            final float levelW = 34f * s;
            final float levelX = removeX - BUTTON_GAP * s - levelW;
            final int vanillaMax = ItemSpec.enchantmentMaxLevel(id);
            final String name = ItemSpec.enchantmentDisplayName(id);
            final String label = entry.getValue() > vanillaMax ? name + "  (max " + vanillaMax + ")" : name;
            Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(label, labelSize, levelX - BUTTON_GAP * s - x0), x0, Fonts.SFPRO_DISPLAY.topForBaseline(cursorY + rowH / 2f + labelSize * 0.59043f * 0.5f, labelSize), labelSize, ColorUtils.withAlpha(entry.getValue() > vanillaMax ? this.style().accent() : this.style().label(), alpha));
            final TextFieldControl level = this.enchantLevelFields.get(id);
            level.syncText(String.valueOf(entry.getValue()));
            level.extract(graphics, levelX, cursorY, levelW, rowH, mouseX, mouseY, delta, alpha, true, true);
            WLRenderSystem.cursors().requestIf(level.isOver(mouseX, mouseY), Cursors.Cursor.TEXT);
            final WLButton remove = this.enchantRemoveButtons.get(id);
            this.layoutRoundButton(remove, removeX, cursorY + (rowH - buttonSize) / 2f, buttonSize, alpha);
            graphics.widget(remove, mouseX, mouseY, delta);
            this.extractButtonIcon(graphics, Icons.MINUS, remove, alpha, 0f);
            cursorY += ENTRY_STRIDE * s;
        }
        return cursorY - ENTRY_GAP * s;
    }

    private float extractSectionLabel(final WLGraphics2D graphics, final String text, final float x, final float y, final float alpha) {
        final float size = SECTION_LABEL_SIZE * this.s();
        Fonts.SFPRO_DISPLAY.extract(graphics, text.toUpperCase(Locale.ROOT), x, Fonts.SFPRO_DISPLAY.topForBaseline(y + size, size), size, ColorUtils.withAlpha(this.style().label(), alpha));
        return y + this.sectionLabelHeight();
    }

    private void extractSectionHeader(final WLGraphics2D graphics, final String text, final float x0, final float x1, final float y, final WLButton button, final Icons.Icon icon, final float rotation, final boolean enabled, final int mouseX, final int mouseY, final float delta, final float alpha) {
        this.extractSectionLabel(graphics, text, x0, y, alpha);
        final float size = BUTTON_SIZE * this.s();
        this.layoutRoundButton(button, x1 - size, y + (this.sectionLabelHeight() - size) / 2f, size, enabled ? alpha : alpha * 0.4f);
        button.active = enabled;
        if (this.extractTrigger(graphics, button, mouseX, mouseY, delta)) {
            this.extractButtonIcon(graphics, icon, button, enabled ? alpha : alpha * 0.4f, rotation);
        }
    }

    private void extractCaption(final WLGraphics2D graphics, final String text, final float x, final float y, final float alpha) {
        final float size = CAPTION_SIZE * this.s();
        Fonts.SFPRO_DISPLAY.extract(graphics, text, x, Fonts.SFPRO_DISPLAY.topForBaseline(y + size, size), size, ColorUtils.withAlpha(this.style().tertiaryLabel(), alpha));
    }

    private void extractRowLabel(final WLGraphics2D graphics, final String text, final float x, final float rowY, final float rowH, final float size, final float alpha) {
        final float cap = size * 0.59043f;
        Fonts.SFPRO_DISPLAY.extract(graphics, text, x, Fonts.SFPRO_DISPLAY.topForBaseline(rowY + rowH / 2f + cap * 0.5f, size), size, ColorUtils.withAlpha(this.style().label(), alpha));
    }

    private void extractTriggerLabel(final WLGraphics2D graphics, final WLButton button, final float alpha) {
        final String label = button.getLabel();
        if (label == null || label.isEmpty()) {
            return;
        }
        final float size = Math.max(Type.CAPTION2.size() * this.s(), button.getHeight() * 0.46f);
        final float width = Fonts.SFPRO_DISPLAY.width(label, size);
        final float cap = size * 0.59043f;
        final float centerX = button.getX() + button.getWidth() / 2f;
        final float centerY = button.getY() + button.getHeight() / 2f;
        final boolean pixelated = Fonts.SFPRO_DISPLAY.pixelated(size);
        final float x = pixelated ? Fonts.SFPRO_DISPLAY.leftForPixelCenter(centerX, label, size) : centerX - width / 2f;
        final float y = pixelated ? Fonts.SFPRO_DISPLAY.topForPixelCenter(centerY, size) : Fonts.SFPRO_DISPLAY.topForBaseline(centerY + cap * 0.5f, size);
        button.stretchContent(() -> Fonts.SFPRO_DISPLAY.extract(graphics, label, x, y, size, ColorUtils.withAlpha(this.style().label(), alpha)));
    }

    private boolean extractTrigger(final WLGraphics2D graphics, final WLButton button, final int mouseX, final int mouseY, final float delta) {
        if (button == this.activeTrigger) {
            button.settleInteraction();
            return false;
        }
        graphics.widget(button, mouseX, mouseY, delta);
        return true;
    }

    private void layoutPill(final WLButton button, final float x, final float y, final float w, final float h, final float alpha) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.max(1, Math.round(w)));
        button.setHeight(Math.max(1, Math.round(h)));
        button.setAlpha(alpha);
        button.setCornerRadius(Math.min(w, h) * 0.35f);
        button.setTextSize(Math.max(Type.CAPTION2.size() * this.s(), h * 0.46f));
    }

    private void layoutRoundButton(final WLButton button, final float x, final float y, final float size, final float alpha) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.max(1, Math.round(size)));
        button.setHeight(Math.max(1, Math.round(size)));
        button.setAlpha(alpha);
        button.setCornerRadius(size / 2f);
    }

    private PopoverStyle style;

    public ItemEditorPopover style(final PopoverStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public PopoverStyle style() {
        return this.style == null ? PopoverStyle.getDefault() : this.style;
    }

    private void extractButtonIcon(final WLGraphics2D graphics, final Icons.Icon icon, final WLButton button, final float alpha, final float rotation) {
        final float size = Math.min(button.getWidth(), button.getHeight()) * ICON_RATIO;
        final float cx = button.getX() + button.getWidth() / 2f;
        final float cy = button.getY() + button.getHeight() / 2f;
        button.stretchContent(() -> icon.extractCentered(graphics, cx, cy, size, ColorUtils.withAlpha(this.style().label(), alpha), rotation));
    }

    private void extractScrollbar(final WLGraphics2D graphics, final float reveal) {
        final float thumbH = this.thumbHeight();
        if (thumbH <= 0f) {
            return;
        }
        final float barW = SCROLLBAR_W * this.s();
        final float y = this.thumbY();
        final int color = ColorUtils.withAlpha(this.style().scrollbar(this.draggingThumb), reveal);
        graphics.fillRoundedGradient(this.lastThumbX, y, this.lastThumbX + barW, y + thumbH, color, color, color, color, barW / 2f);
    }

    private float maxScroll() {
        return Math.max(0f, this.bodyContentHeight() - this.lastBodyHeight);
    }

    private float thumbHeight() {
        final float contentH = this.bodyContentHeight();
        if (this.lastBodyHeight <= 0f || contentH <= this.lastBodyHeight + 0.5f) {
            return 0f;
        }
        return Math.max(14f * this.s(), this.lastBodyHeight * (this.lastBodyHeight / contentH));
    }

    private float thumbY() {
        final float max = this.maxScroll();
        final float t = max <= 0f ? 0f : Mth.clamp(this.scrollY / max, 0f, 1f);
        return this.lastBodyTop + Math.max(0f, this.lastBodyHeight - this.thumbHeight()) * t;
    }

    private boolean isOverThumb(final double mouseX, final double mouseY) {
        final float thumbH = this.thumbHeight();
        if (thumbH <= 0f) {
            return false;
        }
        final float grab = SCROLLBAR_GRAB * this.s();
        final float y = this.thumbY();
        return mouseX >= this.lastThumbX - grab && mouseX <= this.lastThumbX + SCROLLBAR_W * this.s() + grab && mouseY >= y && mouseY < y + thumbH;
    }

    private void syncControls() {
        final String itemId = this.current().getItemId();
        if (!itemId.equals(this.slidersItemId)) {
            this.slidersItemId = itemId;
            this.durabilitySlider = null;
            this.countSlider = null;
        }
    }

    private SliderControl countSlider() {
        if (this.countSlider == null) {
            final int max = Math.max(1, this.current().maxStackSize());
            this.countSlider = new SliderControl(() -> this.current().getCount(), 1, max, 1, this.fadeKeyBase + 10, v -> this.edit(s -> s.setCount((int) Math.round(v))), true, v -> String.valueOf(Math.round(v)), CARD_SLIDER, this.uiScale, this.controlLayer);
        }
        return this.countSlider;
    }

    private SliderControl durabilitySlider() {
        if (this.durabilitySlider == null) {
            final int max = Math.max(1, this.current().maxDamage());
            this.durabilitySlider = new SliderControl(() -> Math.max(1, max - this.current().getDamage()), 1, max, 1, this.fadeKeyBase + 15, v -> this.edit(s -> s.setDamage(max - (int) Math.round(v))), true, v -> String.valueOf(Math.round(v)), CARD_SLIDER, this.uiScale, this.controlLayer);
        }
        return this.durabilitySlider;
    }

    private void syncLoreControls(final int lines) {
        while (this.loreFields.size() < lines) {
            final int index = this.loreFields.size();
            final TextFieldControl field = new TextFieldControl("", text -> this.edit(s -> s.setLoreLine(index, text)), "Empty line", SearchBar.defaultStyle().layer(this.fieldLayer).fadeKey(FadeKeys.next()), this.uiScale);
            field.setMaxLength(128);
            this.loreFields.add(field);
            final WLButton remove = WLButton.icon(0, 0, 1, 1, b -> this.edit(s -> s.removeLoreLine(index)));
            remove.style().layer(this.buttonLayer).fadeKey(FadeKeys.next());
            this.loreRemoveButtons.add(remove);
        }
        while (this.loreFields.size() > lines) {
            this.loreFields.remove(this.loreFields.size() - 1);
            this.loreRemoveButtons.remove(this.loreRemoveButtons.size() - 1);
        }
    }

    private void syncEnchantControls(final Map<String, Integer> enchantments) {
        this.enchantLevelFields.keySet().removeIf(id -> !enchantments.containsKey(id));
        this.enchantRemoveButtons.keySet().removeIf(id -> !enchantments.containsKey(id));
        for (final String id : enchantments.keySet()) {
            this.enchantLevelFields.computeIfAbsent(id, key -> {
                final TextFieldControl field = new TextFieldControl(String.valueOf(this.current().getEnchantmentLevel(key)), text -> this.edit(s -> s.setEnchantment(key, parseLevel(text, s.getEnchantmentLevel(key)))), "1", SearchBar.defaultStyle().layer(this.fieldLayer).fadeKey(FadeKeys.next()), this.uiScale);
                field.setMaxLength(3);
                return field;
            });
            this.enchantRemoveButtons.computeIfAbsent(id, key -> {
                final WLButton remove = WLButton.icon(0, 0, 1, 1, b -> this.edit(s -> s.removeEnchantment(key)));
                remove.style().layer(this.buttonLayer).fadeKey(FadeKeys.next());
                return remove;
            });
        }
    }

    private static int parseLevel(final String text, final int fallback) {
        try {
            final int parsed = Integer.parseInt(text.trim());
            return parsed <= 0 ? Math.max(1, fallback) : Math.min(ItemSpec.MAX_ENCHANT_LEVEL, parsed);
        } catch (final NumberFormatException stillTyping) {
            return Math.max(1, fallback);
        }
    }

    private void openItemPicker() {
        this.openPicker(new CataloguePickerPopover("Change Item", InventoryOptionWidget.entriesFor(this.slot), value -> value.equals(this.current().getItemId()), value -> this.edit(s -> s.setItemId(value)), true, this.pickerHost::close, null, () -> this.pickerOriginStyle(this.changeItemButton), this.uiScale, this.pickerHost.panelLayer()).withTriggerHandoff((g, handoff) -> this.extractTriggerLabel(g, this.changeItemButton, handoff)), this.changeItemButton);
    }

    private void openEnchantPicker() {
        this.openPicker(new CataloguePickerPopover("Enchantments", this.enchantEntries, value -> this.current().getEnchantmentLevel(enchantIdOf(value)) == enchantLevelOf(value), value -> this.edit(s -> {
            final String id = enchantIdOf(value);
            final int level = enchantLevelOf(value);
            s.setEnchantment(id, s.getEnchantmentLevel(id) == level ? 0 : level);
        }), false, this.pickerHost::close, null, () -> this.pickerOriginStyle(this.addEnchantButton), this.uiScale, this.pickerHost.panelLayer()).withTriggerHandoff((g, handoff) -> this.extractButtonIcon(g, Icons.PLUS, this.addEnchantButton, handoff, 0f)), this.addEnchantButton);
    }

    private void openNbtEditor() {
        final NbtEditorPopover sheet = new NbtEditorPopover(this::currentOrNull, this::edit, this.nbtHost::close, () -> this.editNbtButton.style().scaled(this.uiScale).layer(this.nbtHost.panelLayer()), this.uiScale, this.nbtHost.panelLayer(), FadeKeys.next()).withTriggerHandoff((g, handoff) -> this.extractTriggerLabel(g, this.editNbtButton, handoff));
        final int widestSide = Math.max(this.sideRoom(true), this.sideRoom(false));
        if (widestSide < sheet.preferredWidth()) {
            sheet.limitWidth(widestSide);
        }
        this.openBeside(this.nbtHost, sheet, this.editNbtButton, true);
    }

    private int sideRoom(final boolean left) {
        final int gap = Math.round(10f * this.s());
        final int margin = Math.round(PopoverHost.screenMargin());
        return left ? this.lastPanelX - gap - margin : Minecraft.getInstance().getWindow().getGuiScaledWidth() - (this.lastPanelX + this.lastPanelW) - gap - margin;
    }

    private ItemSpec currentOrNull() {
        return this.spec.get();
    }

    private GlassStyle pickerOriginStyle(final WLButton trigger) {
        return trigger.style().scaled(this.uiScale).layer(this.pickerHost.panelLayer());
    }

    private void openPicker(final CataloguePickerPopover picker, final WLButton trigger) {
        this.openBeside(this.pickerHost, picker, trigger, false);
    }

    private void openBeside(final PopoverHost host, final PopoverHost.PopoverContent content, final WLButton trigger, final boolean preferLeft) {
        this.activeTrigger = trigger;
        this.openBeside(host, content, new LiquidRect(trigger.getX(), trigger.getY(), trigger.getWidth(), trigger.getHeight(), trigger.getCornerRadius()), preferLeft);
    }

    private void openBeside(final PopoverHost host, final PopoverHost.PopoverContent content, final LiquidRect anchor, final boolean preferLeft) {
        final var window = Minecraft.getInstance().getWindow();
        final int screenW = window.getGuiScaledWidth();
        final int screenH = window.getGuiScaledHeight();
        final int pickerW = content.preferredWidth();
        final int pickerH = content.preferredHeight();
        final int gap = Math.round(10f * this.s());
        final int margin = Math.round(PopoverHost.screenMargin());
        final int rightRoom = this.sideRoom(false);
        final int leftRoom = this.sideRoom(true);
        final int left = this.lastPanelX - gap - pickerW;
        final int right = this.lastPanelX + this.lastPanelW + gap;
        final int boundsX;
        if (preferLeft) {
            boundsX = leftRoom >= pickerW ? left : rightRoom >= pickerW ? right : Math.max(margin, Math.min(screenW - margin - pickerW, this.lastPanelX + (this.lastPanelW - pickerW) / 2));
        } else if (rightRoom >= pickerW) {
            boundsX = right;
        } else if (leftRoom >= pickerW) {
            boundsX = left;
        } else {
            boundsX = Math.max(margin, Math.min(screenW - margin - pickerW, this.lastPanelX + (this.lastPanelW - pickerW) / 2));
        }
        final int boundsY = Math.max(margin, Math.min(screenH - margin - pickerH, this.lastPanelY + (this.lastPanelH - pickerH) / 2));
        host.openCentered(anchor, boundsX, boundsY, pickerW, pickerH, screenW, screenH, content);
    }

    @Override
    public boolean mouseClickedOutside(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.nbtHost.isOpen() && this.nbtHost.mouseClicked(event, doubleClick)) {
            return true;
        }
        return this.pickerHost.isOpen() && this.pickerHost.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (this.nbtHost.isOpen() && this.nbtHost.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (this.pickerHost.isOpen() && this.pickerHost.mouseClicked(event, doubleClick)) {
            return true;
        }
        final boolean right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (right || event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            final int contentsTarget = this.contentsSlotAt(event.x(), event.y());
            if (this.contentsCursor.holding() && this.overContentsTrash(event.x(), event.y())) {
                final List<ItemSpec> changed = this.contentsCursor.discard(this.current().containerContents());
                if (changed != null) {
                    this.commitContents(changed);
                }
                return true;
            }
            if (this.contentsCursor.holding()) {
                if (contentsTarget >= 0) {
                    this.contentsCursor.press(event.button(), contentsTarget);
                    return true;
                }
                this.contentsCursor.cancel();
            } else if (right && contentsTarget >= 0) {
                this.contentsCursor.pickHalf(this.current().containerContents(), contentsTarget);
                return true;
            }
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        final double mx = event.x();
        final double my = event.y();
        if (this.isOverThumb(mx, my)) {
            this.draggingThumb = true;
            this.thumbGrabOffset = (float) my - this.thumbY();
            return true;
        }
        final int contentsSlot = this.contentsSlotAt(mx, my);
        if (contentsSlot >= 0) {
            this.pressedContentsSlot = contentsSlot;
            this.draggingContents = false;
            this.contentsPressX = mx;
            this.contentsPressY = my;
            this.contentsDragX = mx;
            this.contentsDragY = my;
            return true;
        }
        if (this.changeItemButton.isMouseOver(mx, my)) {
            this.changeItemButton.onClick(event, doubleClick);
            return true;
        }
        if (this.removeItemButton.isMouseOver(mx, my)) {
            this.removeItemButton.onClick(event, doubleClick);
            return true;
        }
        if (my < this.lastBodyTop || my >= this.lastBodyClipBottom) {
            return true;
        }
        return this.bodyClicked(event, doubleClick, mx, my);
    }

    private boolean bodyClicked(final MouseButtonEvent event, final boolean doubleClick, final double mx, final double my) {
        for (final SliderControl slider : this.sliders()) {
            if (slider.isOverField(mx, my)) {
                this.blurTextFields();
                slider.setFieldFocused(true);
                slider.fieldClicked(event, doubleClick);
                return true;
            }
            if (slider.isOverTrack(mx, my)) {
                this.blurTextFields();
                slider.startDrag(mx);
                this.draggingSlider = slider;
                return true;
            }
        }
        if (this.showsUnbreakable() && this.unbreakableToggle.isOverControl(mx, my)) {
            return this.unbreakableToggle.mouseClicked(event);
        }
        if (this.focusField(this.nameField, event, doubleClick, mx, my)) {
            return true;
        }
        for (final TextFieldControl field : this.loreFields) {
            if (this.focusField(field, event, doubleClick, mx, my)) {
                return true;
            }
        }
        for (final TextFieldControl field : this.enchantLevelFields.values()) {
            if (this.focusField(field, event, doubleClick, mx, my)) {
                return true;
            }
        }
        if (this.addLoreButton.active && this.addLoreButton.isMouseOver(mx, my)) {
            this.addLoreButton.onClick(event, doubleClick);
            return true;
        }
        if (this.addEnchantButton.isMouseOver(mx, my)) {
            this.addEnchantButton.onClick(event, doubleClick);
            return true;
        }
        if (this.editNbtButton.isMouseOver(mx, my)) {
            this.editNbtButton.onClick(event, doubleClick);
            return true;
        }
        for (final WLButton button : this.loreRemoveButtons) {
            if (button.isMouseOver(mx, my)) {
                button.onClick(event, doubleClick);
                return true;
            }
        }
        for (final WLButton button : this.enchantRemoveButtons.values()) {
            if (button.isMouseOver(mx, my)) {
                button.onClick(event, doubleClick);
                return true;
            }
        }
        if (this.paletteClicked(mx, my)) {
            return true;
        }
        this.blurTextFields();
        return true;
    }

    private boolean focusField(final TextFieldControl field, final MouseButtonEvent event, final boolean doubleClick, final double mx, final double my) {
        if (!field.isOver(mx, my)) {
            return false;
        }
        this.blurTextFields();
        field.setFocused(true);
        field.clicked(event, doubleClick);
        this.paletteTarget = field;
        return true;
    }

    private boolean paletteClicked(final double mx, final double my) {
        final float size = SWATCH_SIZE * this.s();
        if (this.paletteY == 0f || my < this.paletteY || my >= this.paletteY + size) {
            return false;
        }
        final float stride = size + SWATCH_GAP * this.s();
        final int index = (int) Math.floor((mx - this.paletteX) / stride);
        final List<LegacyText.Swatch> swatches = LegacyText.colorSwatches();
        if (index < 0 || index > swatches.size()) {
            return false;
        }
        final TextFieldControl target = this.paletteTarget == null ? this.nameField : this.paletteTarget;
        target.setFocused(true);
        this.paletteTarget = target;
        target.insertAtCaret(LegacyText.AMPERSAND + String.valueOf(index == swatches.size() ? 'r' : swatches.get(index).code()));
        return true;
    }

    private void blurTextFields() {
        this.nameField.setFocused(false);
        for (final TextFieldControl field : this.loreFields) {
            field.setFocused(false);
        }
        for (final TextFieldControl field : this.enchantLevelFields.values()) {
            field.setFocused(false);
        }
        for (final SliderControl slider : this.sliders()) {
            slider.setFieldFocused(false);
        }
    }

    private SliderControl[] sliders() {
        final List<SliderControl> live = new ArrayList<>(2);
        if (this.showsCount()) {
            live.add(this.countSlider());
        }
        if (this.showsDurability()) {
            live.add(this.durabilitySlider());
        }
        return live.toArray(new SliderControl[0]);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (this.nbtHost.isOpen()) {
            return this.nbtHost.mouseDragged(event, dragX, dragY);
        }
        if (this.pickerHost.isOpen()) {
            return this.pickerHost.mouseDragged(event, dragX, dragY);
        }
        if (this.draggingThumb) {
            final float travel = Math.max(0f, this.lastBodyHeight - this.thumbHeight());
            final float max = this.maxScroll();
            final float t = travel <= 0f ? 0f : ((float) event.y() - this.thumbGrabOffset - this.lastBodyTop) / travel;
            this.scrollTarget = Mth.clamp(t * max, 0f, max);
            this.scrollY = this.scrollTarget;
            return true;
        }
        if (this.draggingSlider != null) {
            this.draggingSlider.dragTo(event.x());
            return true;
        }
        if (this.contentsCursor.pressing()) {
            this.contentsCursor.dragOver(this.current().containerContents(), this.contentsSlotAt(event.x(), event.y()), CONTENTS_RULES);
            return true;
        }
        if (this.pressedContentsSlot >= 0) {
            this.contentsDragX = event.x();
            this.contentsDragY = event.y();
            if (!this.draggingContents) {
                final double slop = 3f * this.s();
                if (Math.abs(event.x() - this.contentsPressX) < slop && Math.abs(event.y() - this.contentsPressY) < slop) {
                    return true;
                }
                if (this.current().containerContents().get(this.pressedContentsSlot) == null) {
                    return true;
                }
                this.draggingContents = true;
                WLRenderSystem.cursors().request(Cursors.Cursor.MOVE);
            }
            return true;
        }
        if (this.showsUnbreakable() && this.unbreakableToggle.isPressed()) {
            return this.unbreakableToggle.mouseDragged(event);
        }
        for (final SliderControl slider : this.sliders()) {
            if (slider.isFieldFocused() && slider.fieldDragged(event, dragX, dragY)) {
                return true;
            }
        }
        if (this.nameField.isFocused() && this.nameField.dragged(event, dragX, dragY)) {
            return true;
        }
        for (final TextFieldControl field : this.loreFields) {
            if (field.isFocused() && field.dragged(event, dragX, dragY)) {
                return true;
            }
        }
        for (final TextFieldControl field : this.enchantLevelFields.values()) {
            if (field.isFocused() && field.dragged(event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.nbtHost.isOpen() && this.nbtHost.mouseReleased(event)) {
            return true;
        }
        if (this.pickerHost.isOpen() && this.pickerHost.mouseReleased(event)) {
            return true;
        }
        boolean handled = this.draggingThumb;
        this.draggingThumb = false;
        if (this.contentsCursor.pressing()) {
            final List<ItemSpec> changed = this.contentsCursor.release(this.current().containerContents(), CONTENTS_RULES);
            if (changed != null) {
                this.commitContents(changed);
            }
            handled = true;
        }
        if (this.pressedContentsSlot >= 0) {
            final int from = this.pressedContentsSlot;
            final boolean wasDragging = this.draggingContents;
            this.pressedContentsSlot = -1;
            this.draggingContents = false;
            final int target = this.contentsSlotAt(event.x(), event.y());
            if (wasDragging) {
                if (this.overContentsTrash(event.x(), event.y())) {
                    this.writeContentsSlot(from, null);
                } else if (target >= 0 && target != from) {
                    this.dropContents(from, target);
                }
            } else if (target == from) {
                this.openContentsSlot(from);
            }
            handled = true;
        }
        if (this.draggingSlider != null) {
            this.draggingSlider.release();
            this.draggingSlider = null;
            handled = true;
        }
        if (this.unbreakableToggle.mouseReleased(event)) {
            handled = true;
        }
        for (final WLButton button : this.allButtons()) {
            button.onRelease(event);
        }
        return handled;
    }

    private List<WLButton> allButtons() {
        final List<WLButton> buttons = new ArrayList<>();
        buttons.add(this.changeItemButton);
        buttons.add(this.removeItemButton);
        buttons.add(this.addLoreButton);
        buttons.add(this.addEnchantButton);
        buttons.add(this.editNbtButton);
        buttons.addAll(this.loreRemoveButtons);
        buttons.addAll(this.enchantRemoveButtons.values());
        return buttons;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (this.pickerHost.isOpen()) {
            return this.pickerHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (this.nbtHost.isOpen()) {
            this.nbtHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        this.scrollTarget = Mth.clamp(this.scrollTarget - (float) scrollY * SCROLL_STEP * this.s(), 0f, this.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.nbtHost.isOpen() && this.nbtHost.keyPressed(event)) {
            return true;
        }
        if (this.pickerHost.isOpen() && this.pickerHost.keyPressed(event)) {
            return true;
        }
        for (final SliderControl slider : this.sliders()) {
            if (slider.isFieldFocused() && slider.fieldKeyPressed(event)) {
                return true;
            }
        }
        if (this.nameField.isFocused() && this.nameField.keyPressed(event)) {
            return true;
        }
        for (final TextFieldControl field : this.loreFields) {
            if (field.isFocused() && field.keyPressed(event)) {
                return true;
            }
        }
        for (final TextFieldControl field : this.enchantLevelFields.values()) {
            if (field.isFocused() && field.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.nbtHost.isOpen() && this.nbtHost.charTyped(event)) {
            return true;
        }
        if (this.pickerHost.isOpen() && this.pickerHost.charTyped(event)) {
            return true;
        }
        for (final SliderControl slider : this.sliders()) {
            if (slider.isFieldFocused() && slider.fieldCharTyped(event)) {
                return true;
            }
        }
        if (this.nameField.isFocused() && this.nameField.charTyped(event)) {
            return true;
        }
        for (final TextFieldControl field : this.loreFields) {
            if (field.isFocused() && field.charTyped(event)) {
                return true;
            }
        }
        for (final TextFieldControl field : this.enchantLevelFields.values()) {
            if (field.isFocused() && field.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    private static String enchantIdOf(final String row) {
        final int sep = row.lastIndexOf(LEVEL_SEP);
        return sep < 0 ? row : row.substring(0, sep);
    }

    private static int enchantLevelOf(final String row) {
        final int sep = row.lastIndexOf(LEVEL_SEP);
        if (sep < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(row.substring(sep + 1));
        } catch (final NumberFormatException malformed) {
            return 1;
        }
    }

    private static GlassStyle cardStyle() {
        return PopoverHost.surfaceStyle().tint(Theme.get().cardFill(), Theme.tintAlpha(Theme.get().cardFillAlpha()));
    }
}
