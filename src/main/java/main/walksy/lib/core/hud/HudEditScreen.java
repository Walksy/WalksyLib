package main.walksy.lib.core.hud;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.glass.LiquidGlassUniforms;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.glass.api.GlassStyle;

import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.options.EmptyStateWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.gui.widget.options.types.KeybindOptionWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import main.walksy.lib.core.platform.WLClient;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class HudEditScreen extends WLScreen {
    private static final float HINT_SIZE = 11f;
    private static final float OUTLINE_PAD = 2f;
    private static final float MIN_GRAB = 14f;
    private static final float HANDLE_SIZE = 5f;
    private static final float HANDLE_GRAB = 11f;
    private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HOTBAR_OFFHAND = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier EXPERIENCE_BAR = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier FOOD = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static final float NUDGE = 0.5f;
    private static final float NUDGE_FAST = 5f;
    private static final float CHIP_PAD_X = 8f;
    private static final float CHIP_PAD_Y = 4f;
    private static final int CHIP_BLUR = 2;
    private static final float CHIP_TINT = 0.20f;
    private static final int CHIP_TEXT_DIM = ColorUtils.withAlpha(0xFFFFFFFF, 0.85f);
    private static final int BACKDROP = ColorUtils.withAlpha(0xFF000000, 0.15f);
    private static final int BLUR_KEEPER = 0x00000000;
    private static final int PANEL_LAYER = 4;
    private static final float PANEL_MARGIN = 14f;
    private static final float PANEL_WIDTH = 236f;
    private static final float PANEL_PADDING = 12f;
    private static final float PANEL_RADIUS = 18f;
    private static final float PANEL_MAX_HEIGHT_RATIO = 0.8f;
    private static final float TITLE_GAP = 2f;
    private static final float HEADER_GAP = 10f;
    private static final float RESET_HEIGHT = 18f;
    private static final float RESET_GAP = 8f;
    private static final String RESET_LABEL = "Reset Position";
    private static final String EMPTY_TITLE = "No settings";
    private static final String EMPTY_DETAIL = "Drag it to move it, or drag its corner to resize it";
    private static float panelPosX = Float.NaN;
    private static float panelPosY = Float.NaN;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final GlassStyle chipStyle = LiquidKnob.bevelKnobStyle(LiquidKnob.knobLayer())
        .blurRadius(CHIP_BLUR)
        .tint(0xFFFFFFFF, CHIP_TINT)
        .toneRange(Theme.get().toneFloor(), Theme.get().toneCeiling())
        .chroma(Theme.get().chroma())
        .innerShadow(Theme.get().innerShadow());
    private final PopoverHost popover = new PopoverHost();
    private @Nullable HudSpace dragging;
    private @Nullable HudSpace selected;
    private float grabOffsetX;
    private float grabOffsetY;
    private @Nullable HudSpace resizing;
    private float resizeLeft;
    private float resizeTop;
    private float resizeStartScale;
    private float resizeStartReach;
    private @Nullable ScrollPanel content;
    private @Nullable ResetButton resetButton;
    private String owner = "";
    private boolean panelOnRight;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private boolean draggingPanel;
    private float panelGrabX;
    private float panelGrabY;
    private PressTarget pressTarget = PressTarget.NONE;

    public HudEditScreen(final Screen parent) {
        this(parent, null);
    }

    public HudEditScreen(final Screen parent, final @Nullable HudSpace selected) {
        super(parent, "Edit HUD");
        this.selected = selected != null && selected.isBound() ? selected : null;
    }

    @Override
    protected void init() {
        super.init();
        this.content = null;
        this.resetButton = null;
        if (this.selected != null) {
            this.buildPanel(this.selected);
        }
    }

    public static boolean hasAnything() {
        return !WalksyLib.getInstance().getHudSpaces().isEmpty();
    }

    public static boolean isOpen() {
        return WLClient.screen() instanceof HudEditScreen;
    }

    @Override
    public void removed() {
        KeybindOptionWidget.cancelCapture();
        this.popover.closeInstant();
        WalksyLib.getInstance().getHudSpaces().save();
        for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            if (mod.hasConfig()) {
                mod.getConfig().saveAndNotify();
            }
        }
        super.removed();
    }

    private void select(final @Nullable HudSpace space) {
        if (space == this.selected) {
            return;
        }
        this.selected = space;
        this.popover.closeInstant();
        if (this.content != null) {
            this.removeWidget(this.content);
            this.content = null;
        }
        if (this.resetButton != null) {
            this.removeWidget(this.resetButton);
            this.resetButton = null;
        }
        if (space != null) {
            this.buildPanel(space);
        }
    }

    private void buildPanel(final HudSpace space) {
        final HudOptions options = HudOptions.of(space);
        this.owner = options.owner();
        this.panelOnRight = space.screenX() + space.width() / 2f < this.width / 2f;
        this.panelW = Math.round(PANEL_WIDTH * this.uiScale);
        final int pad = Math.round(PANEL_PADDING * this.uiScale);
        final ScrollPanel scroll = new ScrollPanel(0, 0, this.panelW - pad * 2, 1);
        scroll.setContentInsetBottom(pad);
        scroll.setScrollbarInsets(PANEL_PADDING / 2f, 0f);
        OptionWidgetFactory.populateGroups(scroll, options.groups(), this.rowContext(), (x, y, w) -> new EmptyStateWidget(EMPTY_TITLE, EMPTY_DETAIL, x, y, w, EmptyStateWidget.preferredHeight()));
        this.content = scroll;
        this.addRenderableWidget(scroll);
        final int resetH = Math.round(RESET_HEIGHT * this.uiScale);
        final ResetButton reset = new ResetButton(0, 0, ResetButton.preferredWidth(RESET_LABEL, resetH), resetH, RESET_LABEL, () -> {
            if (this.selected != null) {
                this.selected.reset();
            }
        });
        this.resetButton = reset;
        this.addRenderableWidget(reset);
        this.layoutPanel();
    }

    private RowContext rowContext() {
        return new RowContext(option -> { }, (anchor, content) -> this.popover.open(anchor, this.width, this.height, content), (anchor, content) -> this.popover.openCentered(anchor, this.panelX, this.panelY, this.panelW, this.panelH, this.width, this.height, content), (anchor, x, y, content) -> this.popover.openAt(anchor, x, y, this.width, this.height, content), this.popover::close);
    }

    private int headerHeight() {
        final float s = this.uiScale;
        return Math.round((PANEL_PADDING + TITLE_GAP + HEADER_GAP) * s + Type.TITLE3.size() * s + Type.SUBHEADLINE.size() * s);
    }

    private void layoutPanel() {
        final ScrollPanel scroll = this.content;
        final ResetButton reset = this.resetButton;
        if (scroll == null || reset == null || this.selected == null) {
            return;
        }
        final int pad = Math.round(PANEL_PADDING * this.uiScale);
        final int header = this.headerHeight();
        final int maxContent = Math.max(1, Math.round(this.height * PANEL_MAX_HEIGHT_RATIO) - header);
        final int contentHeight = Math.max(1, Math.min(scroll.contentHeight() + pad, maxContent));
        if (scroll.getHeight() != contentHeight) {
            scroll.setHeight(contentHeight);
        }
        this.panelH = header + contentHeight;
        final int margin = Math.round(PANEL_MARGIN * this.uiScale);
        final float wantedX = Float.isNaN(panelPosX) ? (this.panelOnRight ? this.width - margin - this.panelW : margin) : panelPosX;
        final float wantedY = Float.isNaN(panelPosY) ? margin : panelPosY;
        this.panelX = Math.round(Math.max(margin, Math.min(Math.max(margin, this.width - margin - this.panelW), wantedX)));
        this.panelY = Math.round(Math.max(margin, Math.min(Math.max(margin, this.height - margin - this.panelH), wantedY)));
        if (scroll.getX() != this.panelX + pad || scroll.getY() != this.panelY + header) {
            scroll.moveTo(this.panelX + pad, this.panelY + header);
        }
        reset.setX(this.panelX + this.panelW - pad - reset.getWidth());
        reset.setY(this.panelY + pad);
        reset.active = !this.selected.isDefault();
    }

    private boolean panelShown() {
        return this.selected != null && this.content != null;
    }

    private boolean inPanel(final double x, final double y) {
        return this.panelShown() && x >= this.panelX && x < this.panelX + this.panelW && y >= this.panelY && y < this.panelY + this.panelH;
    }

    private boolean inPanelHeader(final double x, final double y) {
        return this.inPanel(x, y) && y < this.panelY + this.headerHeight() && (this.resetButton == null || !this.resetButton.isMouseOver(x, y));
    }

    private void extractGameHud(final WLGraphics2D graphics) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (graphics.context().hudHidden()) {
            return;
        }
        if (minecraft.player != null && minecraft.level != null) {
            graphics.context().extractHud();
            return;
        }
        this.extractReferenceHud(graphics);
    }

    private void extractReferenceHud(final WLGraphics2D graphics) {
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        final int left = width / 2 - 91;
        graphics.blitSprite(HOTBAR, left, height - 22, 182, 22);
        graphics.blitSprite(HOTBAR_OFFHAND, left - 29, height - 23, 29, 24);
        graphics.blitSprite(HOTBAR_SELECTION, left - 1, height - 23, 24, 23);
        graphics.blitSprite(EXPERIENCE_BAR, left, height - 29, 182, 5);
        for (int i = 0; i < 10; i++) {
            graphics.blitSprite(HEART, left + i * 8, height - 39, 9, 9);
            graphics.blitSprite(FOOD, width / 2 + 91 - 9 - i * 8, height - 39, 9, 9);
        }
        graphics.blitSprite(CROSSHAIR, (width - 15) / 2, (height - 15) / 2, 15, 15);
    }

    @Override
    protected void extractBackground(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        if (Minecraft.getInstance().level == null) {
            this.extractPanorama(graphics, delta);
        }
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        graphics.fill(0, 0, this.width, this.height, BACKDROP, null);
        this.extractGameHud(graphics);
        graphics.nextStratum();
        this.layoutPanel();
        final List<HudSpace> spaces = WalksyLib.getInstance().getHudSpaces().all();
        final LiquidGlassUniforms glass = WLRenderSystem.glassUniforms();
        glass.holdGlassPass();
        try {
            this.extractSpaces(graphics, spaces, mouseX, mouseY);
        } finally {
            glass.releaseGlassPass();
        }
        glass.lowerEarlyTextBoundary(PANEL_LAYER - 1);
        graphics.nextStratum();
        glass.requestGlassPass(graphics);
        graphics.fill(0, 0, 1, 1, BLUR_KEEPER, null);
        if (this.panelShown()) {
            this.extractPanel(graphics, mouseX, mouseY);
        } else if (spaces.isEmpty()) {
            this.chipText(graphics, "No mod has registered anything to place here yet.", this.width / 2f, this.height / 2f, HINT_SIZE * this.uiScale);
        }
        this.popover.extract(graphics, mouseX, mouseY, delta);
    }

    private void extractSpaces(final WLGraphics2D graphics, final List<HudSpace> spaces, final int mouseX, final int mouseY) {
        final boolean idle = this.dragging == null && this.resizing == null && !this.draggingPanel && !this.inPanel(mouseX, mouseY) && !this.popover.isOpen();
        final HudSpace handle = idle ? this.handleAt(mouseX, mouseY) : this.resizing;
        final HudSpace pointed = idle ? (handle != null ? handle : this.at(mouseX, mouseY)) : (this.dragging != null ? this.dragging : this.resizing);
        for (final HudSpace space : spaces) {
            final float x = space.screenX();
            final float y = space.screenY();
            final boolean hovered = pointed == space;
            if (hovered) {
                WLRenderSystem.cursors().request(handle == space ? Cursors.Cursor.RESIZE_DIAGONAL : Cursors.Cursor.MOVE);
            }
            this.extractBox(graphics, x, y, space.width(), space.height(), hovered, this.selected == space, space.element().visible());
            this.extractElement(graphics, space);
            if (hovered || this.selected == space) {
                this.extractHandle(graphics, space, handle == space);
            }
        }
        this.extractAlignmentGuides(graphics);
    }

    private void extractElement(final WLGraphics2D graphics, final HudSpace space) {
        final LiquidGlassUniforms glass = WLRenderSystem.glassUniforms();
        final int glassBefore = glass.getCount();
        final TextQueueMark text = WLRenderSystem.textMark();
        final List<WLElement> shapes = new ArrayList<>();
        WLGraphics2D.capture(shapes, () -> space.render(graphics, true));
        final boolean overGlass = glass.getCount() > glassBefore;
        for (final WLElement shape : shapes) {
            if (overGlass) {
                WLRenderSystem.contentStratum().addEarly(shape);
            } else {
                graphics.context().submit(shape);
            }
        }
        WLRenderSystem.markEarlyText(text);
    }

    private void extractPanel(final WLGraphics2D graphics, final int mouseX, final int mouseY) {
        final HudSpace space = this.selected;
        if (space == null) {
            return;
        }
        final float s = this.uiScale;
        graphics.glass()
            .dimensions(this.panelX, this.panelY, this.panelW, this.panelH)
            .cornerRadius(PANEL_RADIUS * s)
            .style(WalksyLibPanelScreen.panelStyle().layer(PANEL_LAYER).scaled(s))
            .extract();
        WLRenderSystem.cursors().requestIf(this.draggingPanel || (this.dragging == null && this.resizing == null && this.inPanelHeader(mouseX, mouseY)), Cursors.Cursor.MOVE);
        final Theme theme = Theme.get();
        final float pad = PANEL_PADDING * s;
        final float left = this.panelX + pad;
        final float titleSize = Type.TITLE3.size() * s;
        final float subtitleSize = Type.SUBHEADLINE.size() * s;
        final float titleRoom = (this.resetButton == null ? this.panelX + this.panelW - pad : this.resetButton.getX() - RESET_GAP * s) - left;
        final String subtitle = (this.owner.isEmpty() ? "" : this.owner + "  ·  ") + Math.round(space.scale() * 100f) + "%";
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().begin();
        try {
            Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, Fonts.SFPRO_DISPLAY_BOLD.truncate(space.name(), titleSize, titleRoom), left, this.panelY + pad, titleSize, theme.label());
            Fonts.SFPRO_DISPLAY.extract(graphics, Fonts.SFPRO_DISPLAY.truncate(subtitle, subtitleSize, this.panelW - pad * 2f), left, this.panelY + pad + titleSize + TITLE_GAP * s, subtitleSize, theme.secondaryLabel());
        } finally {
            WLRenderSystem.contentStratum().end();
            WLRenderSystem.markContentText(textMark);
        }
    }

    private void chipText(final WLGraphics2D graphics, final String text, final float cx, final float top, final float size) {
        final float padX = CHIP_PAD_X * this.uiScale;
        final float padY = CHIP_PAD_Y * this.uiScale;
        final float chipW = Fonts.SFPRO_DISPLAY.width(text, size) + padX * 2f;
        final float chipH = size + padY * 2f;
        final float x0 = cx - chipW / 2f;
        graphics.glass()
            .dimensions(x0, top, chipW, chipH)
            .cornerRadius(chipH / 2f)
            .style(this.chipStyle.scaled(this.uiScale))
            .extract();
        Fonts.SFPRO_DISPLAY.extract(graphics, text, x0 + padX, top + padY, size, CHIP_TEXT_DIM);
    }

    private void extractAlignmentGuides(final WLGraphics2D graphics) {
        final HudSpace space = this.dragging;
        if (space == null) {
            return;
        }
        final float line = DevicePixels.snapSizeY(WLScreen.hairlinePx());
        final int color = ColorUtils.withAlpha(Theme.get().accent(), 0.7f);
        if (space.isOnAnchorX()) {
            final float x = DevicePixels.snapX(space.anchorX() * this.width);
            graphics.fill(Math.min(x, this.width - line), 0f, Math.min(x, this.width - line) + line, this.height, color, null);
        }
        if (space.isOnAnchorY()) {
            final float y = DevicePixels.snapY(space.anchorY() * this.height);
            graphics.fill(0f, Math.min(y, this.height - line), this.width, Math.min(y, this.height - line) + line, color, null);
        }
    }

    private void extractBox(final WLGraphics2D graphics, final float x, final float y, final float w, final float h, final boolean hovered, final boolean selected, final boolean live) {
        final float pad = OUTLINE_PAD * this.uiScale;
        final float line = DevicePixels.snapSizeY(WLScreen.hairlinePx());
        final int edge = hovered || selected ? Theme.get().accent() : ColorUtils.withAlpha(0xFFFFFFFF, live ? 0.85f : 0.35f);
        final float x0 = DevicePixels.snapX(x - pad);
        final float y0 = DevicePixels.snapY(y - pad);
        final float x1 = DevicePixels.snapX(x + w + pad);
        final float y1 = DevicePixels.snapY(y + h + pad);
        graphics.fill(x0, y0, x1, y0 + line, edge, null);
        graphics.fill(x0, y1 - line, x1, y1, edge, null);
        graphics.fill(x0, y0, x0 + line, y1, edge, null);
        graphics.fill(x1 - line, y0, x1, y1, edge, null);
    }

    private void extractHandle(final WLGraphics2D graphics, final HudSpace space, final boolean active) {
        final float size = HANDLE_SIZE * this.uiScale;
        final float line = DevicePixels.snapSizeY(WLScreen.hairlinePx());
        final float x0 = DevicePixels.snapX(this.handleX(space) - size / 2f);
        final float y0 = DevicePixels.snapY(this.handleY(space) - size / 2f);
        final float x1 = x0 + DevicePixels.snapSizeX(size);
        final float y1 = y0 + DevicePixels.snapSizeY(size);
        final int accent = Theme.get().accent();
        final int edge = active ? accent : 0xFFFFFFFF;
        final int fill = active ? 0xFFFFFFFF : accent;
        final float radius = size * 0.25f;
        graphics.fillRoundedGradient(x0, y0, x1, y1, edge, edge, edge, edge, radius);
        graphics.fillRoundedGradient(x0 + line, y0 + line, x1 - line, y1 - line, fill, fill, fill, fill, Math.max(0f, radius - line));
    }

    private float handleX(final HudSpace space) {
        return space.screenX() + space.width() + OUTLINE_PAD * this.uiScale;
    }

    private float handleY(final HudSpace space) {
        return space.screenY() + space.height() + OUTLINE_PAD * this.uiScale;
    }

    private @Nullable HudSpace handleAt(final double x, final double y) {
        final List<HudSpace> spaces = WalksyLib.getInstance().getHudSpaces().all();
        final float reach = HANDLE_GRAB * this.uiScale / 2f;
        for (int i = spaces.size() - 1; i >= 0; i--) {
            final HudSpace space = spaces.get(i);
            if (Math.abs(x - this.handleX(space)) <= reach && Math.abs(y - this.handleY(space)) <= reach) {
                return space;
            }
        }
        return null;
    }

    private @Nullable HudSpace at(final double x, final double y) {
        final List<HudSpace> spaces = WalksyLib.getInstance().getHudSpaces().all();
        final float minimum = MIN_GRAB * this.uiScale;
        for (int i = spaces.size() - 1; i >= 0; i--) {
            final HudSpace space = spaces.get(i);
            final float w = Math.max(minimum, space.width());
            final float h = Math.max(minimum, space.height());
            final float left = space.screenX() - (w - space.width()) / 2f;
            final float top = space.screenY() - (h - space.height()) / 2f;
            if (x >= left && x < left + w && y >= top && y < top + h) {
                return space;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.popover.isOpen()) {
            if (this.popover.mouseClicked(event, doubleClick)) {
                this.pressTarget = PressTarget.POPOVER;
                return true;
            }
            this.pressTarget = PressTarget.NONE;
            return true;
        }
        this.pressTarget = PressTarget.NONE;
        final boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        if (this.inPanel(event.x(), event.y())) {
            if (left && this.inPanelHeader(event.x(), event.y())) {
                this.draggingPanel = true;
                this.panelGrabX = (float) event.x() - this.panelX;
                this.panelGrabY = (float) event.y() - this.panelY;
                return true;
            }
            this.pressTarget = PressTarget.WIDGETS;
            super.mouseClicked(event, doubleClick);
            return true;
        }
        if (left) {
            final HudSpace handle = this.handleAt(event.x(), event.y());
            if (handle != null) {
                this.select(handle);
                this.resizing = handle;
                this.resizeLeft = handle.screenX();
                this.resizeTop = handle.screenY();
                this.resizeStartScale = handle.scale();
                this.resizeStartReach = Math.max(1f, (float) (event.x() - this.resizeLeft + event.y() - this.resizeTop));
                return true;
            }
            final HudSpace hit = this.at(event.x(), event.y());
            if (hit != null) {
                this.select(hit);
                this.dragging = hit;
                this.grabOffsetX = Math.max(0f, Math.min(hit.width(), (float) event.x() - hit.screenX()));
                this.grabOffsetY = Math.max(0f, Math.min(hit.height(), (float) event.y() - hit.screenY()));
                return true;
            }
            this.select(null);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.pressTarget == PressTarget.POPOVER) {
            return this.popover.mouseDragged(event, dragX, dragY);
        }
        if (this.draggingPanel) {
            panelPosX = (float) event.x() - this.panelGrabX;
            panelPosY = (float) event.y() - this.panelGrabY;
            this.layoutPanel();
            return true;
        }
        if (this.resizing != null) {
            final float reach = (float) (event.x() - this.resizeLeft + event.y() - this.resizeTop);
            this.resizing.resize(this.resizeStartScale * Math.max(0f, reach) / this.resizeStartReach);
            this.resizing.setScreenPosition(this.resizeLeft, this.resizeTop);
            return true;
        }
        if (this.dragging != null) {
            this.dragging.setScreenPosition((float) event.x() - this.grabOffsetX, (float) event.y() - this.grabOffsetY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        final PressTarget target = this.pressTarget;
        this.pressTarget = PressTarget.NONE;
        if (target == PressTarget.POPOVER) {
            return this.popover.mouseReleased(event);
        }
        if (this.draggingPanel || this.dragging != null || this.resizing != null) {
            this.draggingPanel = false;
            this.dragging = null;
            this.resizing = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.popover.isOpen()) {
            this.popover.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        if (this.inPanel(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        final HudSpace hit = this.at(mouseX, mouseY);
        if (hit != null) {
            this.select(hit);
            hit.setScale(hit.scale() + (float) scrollY * hit.element().scaleStep());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final KeybindOptionWidget capturing = KeybindOptionWidget.capturing();
        if (capturing != null) {
            return capturing.keyPressed(event);
        }
        if (this.popover.isOpen()) {
            return this.popover.keyPressed(event);
        }
        if (this.selected != null) {
            final float step = event.hasShiftDown() ? NUDGE_FAST : NUDGE;
            switch (event.key()) {
                case InputConstants.KEY_LEFT -> {
                    this.selected.nudge(-step, 0f);
                    return true;
                }
                case InputConstants.KEY_RIGHT -> {
                    this.selected.nudge(step, 0f);
                    return true;
                }
                case InputConstants.KEY_UP -> {
                    this.selected.nudge(0f, -step);
                    return true;
                }
                case InputConstants.KEY_DOWN -> {
                    this.selected.nudge(0f, step);
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.popover.isOpen()) {
            return this.popover.charTyped(event);
        }
        return super.charTyped(event);
    }

    private enum PressTarget {
        NONE,
        POPOVER,
        WIDGETS
    }
}
