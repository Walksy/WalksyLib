package main.walksy.lib.core.hud;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.glass.api.GlassStyle;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.options.OptionGroupHeaderWidget;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.LiquidKnob;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import main.walksy.lib.core.platform.WLClient;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class HudEditScreen extends WLScreen {
    private static final float HINT_SIZE = 11f;
    private static final float OUTLINE_PAD = 2f;
    private static final float MIN_GRAB = 14f;
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
    private static final float CHIP_GAP = 4f;
    private static final int GLASS_BLUR = 2;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final @Nullable HudSpace focus;
    private @Nullable HudSpace dragging;
    private @Nullable HudSpace selected;
    private float grabOffsetX;
    private float grabOffsetY;
    private final GlassStyle chipStyle = LiquidKnob.bevelKnobStyle(LiquidKnob.knobLayer())
        .blurRadius(GLASS_BLUR)
        .tint(0xFFFFFFFF, CHIP_TINT)
        .toneRange(Theme.get().toneFloor(), Theme.get().toneCeiling())
        .chroma(Theme.get().chroma())
        .innerShadow(Theme.get().innerShadow());
    private static final float CHIP_TINT = 0.20f;
    private static final int CHIP_TEXT = 0xFFFFFFFF;
    private static final int CHIP_TEXT_DIM = ColorUtils.withAlpha(0xFFFFFFFF, 0.85f);
    private static final Set<String> HIDDEN_IN_EDITOR = new LinkedHashSet<>();
    private static final float PANEL_MARGIN = 14f;
    private static final float PANEL_GLASS_PAD = 6f;
    private static final float PANEL_RADIUS = 10f;
    private static final float PANEL_WIDTH = 210f;
    private static final float PANEL_MAX_HEIGHT_RATIO = 0.6f;
    private static final float HEADER_HEIGHT = 20f;
    private static final float HEADER_TEXT_INSET = 8f;
    private static final float GRIP_DOT = 1.6f;
    private static final float GRIP_SPACING = 3.2f;
    private static final float MINIMISE_SIZE = 13f;
    private static final float RESET_INSET = 8f + 30f + 6f;
    private static final String HEADER_TITLE = "HUD Elements";
    private static final int WINDOW_LAYER = 4;
    private static final int WINDOW_BLUR = 6;
    private static final float WINDOW_TINT = 1.1f;
    private static final float FOOTER_LINE_SPACING = 1.5f;
    private static final String RESET_LABEL = "Reset Position";
    private static float panelPosX = Float.NaN;
    private static float panelPosY = Float.NaN;
    private static boolean panelMinimised;
    private float minimiseX0, minimiseY0, minimiseX1, minimiseY1;
    private boolean draggingPanel;
    private float lastFooterHeight;
    private int lastMouseX;
    private int lastMouseY;
    private float panelGrabX;
    private float panelGrabY;
    private @Nullable ScrollPanel editorPanel;

    public HudEditScreen(final Screen parent) {
        this(parent, null);
    }

    public HudEditScreen(final Screen parent, final @Nullable HudSpace focus) {
        super(parent, focus == null ? "Edit HUD" : "Place " + focus.name());
        this.focus = focus != null && focus.isBound() ? focus : null;
        this.selected = this.focus;
    }

    @Override
    protected void init() {
        super.init();
        this.editorPanel = null;
        if (this.focus != null) {
            return;
        }
        final List<HudSpace> spaces = WalksyLib.getInstance().getHudSpaces().all();
        if (spaces.isEmpty()) {
            return;
        }
        final OptionGroup.Builder group = OptionGroup.builder("");
        for (final HudSpace space : spaces) {
            group.option(Options.ofBoolean(space.name(), () -> !hiddenInEditor(space), true, shown -> setHiddenInEditor(space, !shown)).build());
        }
        final Category category = Category.builder("").group(group.build()).build();
        final int margin = Math.round(PANEL_MARGIN * this.uiScale);
        final int width = Math.round(PANEL_WIDTH * this.uiScale);
        final ScrollPanel panel = new ScrollPanel(margin, margin + this.headerHeight(), width, Math.max(1, Math.round(this.height * PANEL_MAX_HEIGHT_RATIO)));
        OptionWidgetFactory.populate(panel, category, new RowContext(option -> { }, (anchor, content) -> { }, (anchor, content) -> { }, (anchor, x, y, content) -> { }, () -> { }));
        int index = 0;
        for (final AbstractWidget child : panel.children()) {
            if (child instanceof final OptionRowWidget<?> row) {
                if (index < spaces.size()) {
                    final HudSpace space = spaces.get(index);
                    row.setResetButton(RESET_LABEL, RESET_INSET, () -> !space.isDefault(), space::reset);
                }
                index++;
            } else if (child instanceof OptionGroupHeaderWidget) {
                child.setHeight(0);
            }
        }
        panel.relayout();
        panel.setHeight(Math.max(1, Math.min(panel.getHeight(), panel.contentHeight())));
        this.editorPanel = panel;
        if (!panelMinimised) {
            this.addRenderableWidget(panel);
        }
        if (!Float.isNaN(panelPosX)) {
            this.placePanel(panelPosX, panelPosY);
        }
    }

    private static boolean hiddenInEditor(final HudSpace space) {
        return HIDDEN_IN_EDITOR.contains(space.name());
    }

    private static void setHiddenInEditor(final HudSpace space, final boolean hidden) {
        if (hidden) {
            HIDDEN_IN_EDITOR.add(space.name());
        } else {
            HIDDEN_IN_EDITOR.remove(space.name());
        }
    }

    private float chipText(final WLGraphics2D graphics, final String text, final float cx, final float top, final float size, final int color) {
        final float padX = CHIP_PAD_X * this.uiScale;
        final float padY = CHIP_PAD_Y * this.uiScale;
        final float textWidth = Fonts.SFPRO_DISPLAY.width(text, size);
        final float chipW = textWidth + padX * 2f;
        final float chipH = size + padY * 2f;
        final float x0 = cx - chipW / 2f;
        graphics.glass()
            .dimensions(x0, top, chipW, chipH)
            .cornerRadius(chipH / 2f)
            .style(this.chipStyle.scaled(this.uiScale))
            .extract();
        Fonts.SFPRO_DISPLAY.extract(graphics, text, x0 + padX, top + padY, size, color);
        return chipH;
    }

    public static boolean hasAnything() {
        return !WalksyLib.getInstance().getHudSpaces().isEmpty();
    }

    public static boolean isOpen() {
        return WLClient.screen() instanceof HudEditScreen;
    }

    @Override
    public void removed() {
        WalksyLib.getInstance().getHudSpaces().save();
        super.removed();
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
    public void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        this.extractGameHud(graphics);
        final List<HudSpace> spaces = WalksyLib.getInstance().getHudSpaces().all();
        for (final HudSpace space : spaces) {
            final boolean editable = this.focus == null || this.focus == space;
            final boolean live = space.element().visible();
            if (!editable && !live) {
                continue;
            }
            if (hiddenInEditor(space)) {
                continue;
            }
            final float x = space.screenX();
            final float y = space.screenY();
            final float w = space.width();
            final float h = space.height();
            final boolean hovered = this.dragging == space || (this.dragging == null && this.at(mouseX, mouseY) == space);
            WLRenderSystem.cursors().requestIf(hovered && editable, Cursors.Cursor.MOVE);
            if (editable) {
                this.extractBox(graphics, x, y, w, h, hovered, this.selected == space, live);
            }
            space.render(graphics, editable);
            if (editable) {
                this.extractLabel(graphics, space, x, y, w, hovered);
            }
        }
        this.extractAlignmentGuides(graphics);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.extractEditorPanelGlass(graphics);
        WLRenderSystem.cursors().requestIf(this.draggingPanel || (this.dragging == null && this.inPanelHeader(mouseX, mouseY) && !this.onMinimise(mouseX, mouseY)), Cursors.Cursor.MOVE);
        if (spaces.isEmpty() && this.focus == null) {
            final float size = HINT_SIZE * this.uiScale;
            this.chipText(graphics, "No mod has registered anything to place here yet.", this.width / 2f, this.height / 2f, size, CHIP_TEXT);
        }
        this.extractHint(graphics);
    }

    private int headerHeight() {
        return Math.round(HEADER_HEIGHT * this.uiScale);
    }

    private void placePanel(final float x, final float y) {
        final ScrollPanel panel = this.editorPanel;
        if (panel == null) {
            return;
        }
        final float pad = PANEL_GLASS_PAD * this.uiScale;
        final int header = this.headerHeight();
        final float maxX = Math.max(pad, this.width - panel.getWidth() - pad);
        final float maxY = Math.max(pad + header, this.height - panel.getHeight() - pad - (panelMinimised ? 0f : this.lastFooterHeight));
        final int nx = Math.round(Math.max(pad, Math.min(maxX, x)));
        final int ny = Math.round(Math.max(pad + header, Math.min(maxY, y)));
        panel.moveTo(nx, ny);
        panelPosX = nx;
        panelPosY = ny;
    }

    private boolean inPanelHeader(final double x, final double y) {
        final ScrollPanel panel = this.editorPanel;
        if (panel == null) {
            return false;
        }
        final float pad = PANEL_GLASS_PAD * this.uiScale;
        return x >= panel.getX() - pad && x < panel.getX() + panel.getWidth() + pad && y >= panel.getY() - this.headerHeight() - pad && y < panel.getY();
    }

    private boolean onMinimise(final double x, final double y) {
        return x >= this.minimiseX0 && x < this.minimiseX1 && y >= this.minimiseY0 && y < this.minimiseY1;
    }

    private void toggleMinimised() {
        final ScrollPanel panel = this.editorPanel;
        if (panel == null) {
            return;
        }
        panelMinimised = !panelMinimised;
        if (panelMinimised) {
            this.removeWidget(panel);
        } else {
            this.addRenderableWidget(panel);
        }
    }

    private static int ink(final float alpha) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | (Theme.get().isDark() ? 0xFFFFFF : 0x000000);
    }

    private GlassStyle windowStyle() {
        final Theme theme = Theme.get();
        final float edgeWidth = Math.max(1f, Math.round(0.75f * this.uiScale * DevicePixels.perLogicalY())) * 2f;
        return new GlassStyle().flat()
            .layer(WINDOW_LAYER)
            .blurRadius(WINDOW_BLUR)
            .smoothing(0f)
            .tint(theme.flatSurface(), WINDOW_TINT)
            .outline(edgeWidth, theme.isDark() ? 0xFFFFFF : 0x000000, theme.isDark() ? 0.22f : 0.18f)
            .nativePixels();
    }

    private List<String> footerLines(final float width) {
        final float size = Type.CAPTION1.size() * this.uiScale;
        final String hint = this.selected == null ? "Drag to move (snaps to centre and edges), scroll to resize, Esc to finish" : "Drag, or arrow keys for half a pixel (Shift for 5), scroll to resize, Esc to finish";
        return Fonts.SFPRO_DISPLAY.wrap(hint, size, width);
    }

    private String selectionLine() {
        if (this.selected == null) {
            return "Click an element to select it";
        }
        return this.selected.name() + "   " + Math.round(this.selected.width()) + " x " + Math.round(this.selected.height());
    }

    private float footerHeight(final ScrollPanel panel) {
        if (panelMinimised) {
            return 0f;
        }
        final float lineH = Type.CAPTION1.size() * this.uiScale * FOOTER_LINE_SPACING;
        return PANEL_GLASS_PAD * this.uiScale + lineH * (1 + this.footerLines(panel.getWidth()).size());
    }

    private void extractEditorPanelGlass(final WLGraphics2D graphics) {
        final ScrollPanel panel = this.editorPanel;
        if (panel == null) {
            return;
        }
        final Theme theme = Theme.get();
        final float s = this.uiScale;
        final float pad = PANEL_GLASS_PAD * s;
        final float header = this.headerHeight();
        final float footer = this.footerHeight(panel);
        this.lastFooterHeight = footer;
        final float x0 = panel.getX() - pad;
        final float y0 = panel.getY() - header - pad;
        final float x1 = panel.getX() + panel.getWidth() + pad;
        final float y1 = panelMinimised ? panel.getY() : panel.getY() + panel.getHeight() + pad + footer;
        graphics.glass()
            .dimensions(x0, y0, x1 - x0, y1 - y0)
            .cornerRadius(PANEL_RADIUS * s)
            .style(this.windowStyle())
            .extract();
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().begin();
        try {
            this.extractTitleBar(graphics, panel, theme, y0 + (header + pad) / 2f);
            if (!panelMinimised) {
                final float hair = WLScreen.hairlinePx();
                graphics.fill(panel.getX(), panel.getY() - hair, panel.getX() + panel.getWidth(), panel.getY(), theme.separator(), null);
                this.extractFooter(graphics, panel, theme, panel.getY() + panel.getHeight() + pad);
            }
        } finally {
            WLRenderSystem.contentStratum().end();
            WLRenderSystem.markContentText(textMark);
        }
    }

    private void extractTitleBar(final WLGraphics2D graphics, final ScrollPanel panel, final Theme theme, final float centerY) {
        final float s = this.uiScale;
        final float dot = GRIP_DOT * s;
        final float spacing = GRIP_SPACING * s;
        final float gripX = panel.getX() + dot;
        final int gripColor = theme.tertiaryLabel();
        for (int col = 0; col < 2; col++) {
            for (int row = -1; row <= 1; row++) {
                final float dx = gripX + col * spacing;
                final float dy = centerY + row * spacing;
                graphics.fillRoundedGradient(dx - dot / 2f, dy - dot / 2f, dx + dot / 2f, dy + dot / 2f, gripColor, gripColor, gripColor, gripColor, dot / 2f);
            }
        }
        final float size = Type.SUBHEADLINE.size() * s;
        drawCentered(graphics, Fonts.SFPRO_DISPLAY_BOLD, HEADER_TITLE, gripX + spacing + HEADER_TEXT_INSET * s, centerY, size, theme.label());
        final float button = MINIMISE_SIZE * s;
        this.minimiseX1 = panel.getX() + panel.getWidth();
        this.minimiseX0 = this.minimiseX1 - button;
        this.minimiseY0 = centerY - button / 2f;
        this.minimiseY1 = centerY + button / 2f;
        final boolean overMinimise = this.onMinimise(this.lastMouseX, this.lastMouseY);
        WLRenderSystem.cursors().requestIf(overMinimise, Cursors.Cursor.POINTER);
        final int chipFill = ink(overMinimise ? 0.22f : 0.10f);
        graphics.fillRoundedGradient(this.minimiseX0, this.minimiseY0, this.minimiseX1, this.minimiseY1, chipFill, chipFill, chipFill, chipFill, button * 0.34f);
        final String mark = panelMinimised ? "+" : "-";
        final float markSize = Type.CAPTION1.size() * s;
        drawCentered(graphics, Fonts.SFPRO_DISPLAY_BOLD, mark, this.minimiseX0 + (button - Fonts.SFPRO_DISPLAY_BOLD.width(mark, markSize)) / 2f, centerY, markSize, theme.label());
    }

    private void extractFooter(final WLGraphics2D graphics, final ScrollPanel panel, final Theme theme, final float top) {
        final float size = Type.CAPTION1.size() * this.uiScale;
        final float lineH = size * FOOTER_LINE_SPACING;
        final float hair = WLScreen.hairlinePx();
        graphics.fill(panel.getX(), top - hair, panel.getX() + panel.getWidth(), top, theme.separator(), null);
        float center = top + PANEL_GLASS_PAD * this.uiScale * 0.5f + lineH / 2f;
        drawCentered(graphics, Fonts.SFPRO_DISPLAY_BOLD, Fonts.SFPRO_DISPLAY_BOLD.truncate(this.selectionLine(), size, panel.getWidth()), panel.getX(), center, size, this.selected == null ? theme.secondaryLabel() : theme.label());
        for (final String line : this.footerLines(panel.getWidth())) {
            center += lineH;
            drawCentered(graphics, Fonts.SFPRO_DISPLAY, line, panel.getX(), center, size, theme.secondaryLabel());
        }
    }

    private static void drawCentered(final WLGraphics2D graphics, final MsdfFont font, final String text, final float x, final float centerY, final float size, final int color) {
        font.extract(graphics, text, x, font.topForBaseline(centerY + size * 0.59043f * 0.5f, size), size, color);
    }

    private boolean inEditorPanel(final double x, final double y) {
        final ScrollPanel panel = this.editorPanel;
        return panel != null && !panelMinimised && x >= panel.getX() && x < panel.getX() + panel.getWidth() && y >= panel.getY() && y < panel.getY() + panel.getHeight();
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

    private void extractLabel(final WLGraphics2D graphics, final HudSpace space, final float x, final float y, final float w, final boolean hovered) {
        final float size = Type.CAPTION1.size() * this.uiScale;
        final String name = space.element().visible() ? space.name() : space.name() + " (hidden)";
        final float chipH = size + CHIP_PAD_Y * 2f * this.uiScale;
        final float gap = OUTLINE_PAD * this.uiScale + CHIP_GAP * this.uiScale;
        final float above = y - gap - chipH;
        final float top = above >= 0f ? above : y + space.height() + gap;
        this.chipText(graphics, name, x + w / 2f, top, size, hovered || this.selected == space ? CHIP_TEXT : CHIP_TEXT_DIM);
    }

    private void extractHint(final WLGraphics2D graphics) {
        if (this.editorPanel != null) {
            return;
        }
        final float size = HINT_SIZE * this.uiScale;
        final String hint = this.selected == null ? "Drag to move (snaps to centre and edges)  ·  scroll to resize  ·  Esc to finish" : "Drag, or arrow keys for half a pixel (Shift for 5)" + "  ·  scroll to resize  ·  Esc to finish";
        float top = CHIP_GAP * 2f * this.uiScale;
        if (this.selected != null) {
            final String title = (this.focus != null ? "Placing " : "") + this.selected.name() + "   " + Math.round(this.selected.width()) + " x " + Math.round(this.selected.height());
            top += this.chipText(graphics, title, this.width / 2f, top, Type.CAPTION1.size() * this.uiScale, CHIP_TEXT) + CHIP_GAP * this.uiScale;
        }
        this.chipText(graphics, hint, this.width / 2f, top, size, CHIP_TEXT_DIM);
    }

    @Override
    protected void extractMenuBackground(final WLGraphics2D graphics, final int x, final int y, final int width, final int height) {
        graphics.fill(x, y, x + width, y + height, ColorUtils.withAlpha(0xFF000000, 0.15f), null);
    }

    private @Nullable HudSpace at(final double x, final double y) {
        final List<HudSpace> spaces = this.focus == null ? WalksyLib.getInstance().getHudSpaces().all() : List.of(this.focus);
        final float minimum = MIN_GRAB * this.uiScale;
        for (int i = spaces.size() - 1; i >= 0; i--) {
            final HudSpace space = spaces.get(i);
            if (hiddenInEditor(space)) {
                continue;
            }
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
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.editorPanel != null && this.onMinimise(event.x(), event.y())) {
            this.toggleMinimised();
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.editorPanel != null && this.inPanelHeader(event.x(), event.y())) {
            this.draggingPanel = true;
            this.panelGrabX = (float) event.x() - this.editorPanel.getX();
            this.panelGrabY = (float) event.y() - this.editorPanel.getY();
            return true;
        }
        if (this.inEditorPanel(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            final HudSpace hit = this.at(event.x(), event.y());
            if (hit != null) {
                this.dragging = hit;
                this.selected = hit;
                this.grabOffsetX = Math.max(0f, Math.min(hit.width(), (float) event.x() - hit.screenX()));
                this.grabOffsetY = Math.max(0f, Math.min(hit.height(), (float) event.y() - hit.screenY()));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        if (this.draggingPanel) {
            this.placePanel((float) event.x() - this.panelGrabX, (float) event.y() - this.panelGrabY);
            return true;
        }
        if (this.dragging == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        this.dragging.setScreenPosition((float) event.x() - this.grabOffsetX, (float) event.y() - this.grabOffsetY);
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.draggingPanel) {
            this.draggingPanel = false;
            return true;
        }
        if (this.dragging != null) {
            this.dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.inEditorPanel(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        final HudSpace hit = this.at(mouseX, mouseY);
        if (hit != null) {
            this.selected = hit;
            hit.setScale(hit.scale() + (float) scrollY * hit.element().scaleStep());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
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
}
