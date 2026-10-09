package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.element.WLRect;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.value.PixelGrid;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.core.gui.animation.Materialize;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.hud.HudEditScreen;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import main.walksy.lib.core.gui.widget.options.control.SliderControl;
import main.walksy.lib.core.gui.widget.options.control.PillSelectorWidget;
import main.walksy.lib.core.gui.widget.options.control.ColorPickerBody;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class PixelGridAnimationOptionWidget extends OptionRowWidget<PixelGridAnimation> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x91DE00L);
    private static final float PREVIEW_CORNER_RATIO = 0.3f;
    private static final float HOVER_SPEED = 14f;
    private static final String EDIT_LABEL = "Edit Animation";
    private static final String HUD_LABEL = "Edit HUD";
    private static final float EDIT_PILL_GAP = 8f;
    private static final float EDIT_PILL_H_PADDING = 8f;
    private static final float DESCRIPTION_SIZE = Type.FOOTNOTE.size();
    private static final float POPOVER_WIDTH = 296f;
    private static final float POP_PAD = 14f;
    private static final float POP_GAP = 7f;
    private static final float LIVE_PREVIEW_SIZE = 34f;
    private static final float CARD_ROW_H = 30f;
    private static final float CARD_RADIUS = 8f;
    private static final float SETTINGS_LABEL_COLUMN = 74f;
    private static final float BLOCK_GAP = 12f;
    private static final float THUMB_SIZE = 22f;
    private static final float THUMB_GAP = 6f;
    private static final int MAX_STRIP_ROWS = 4;
    private static final float MIN_THUMB_SIZE = 8f;
    private static final float FRAME_BUTTON_W = THUMB_SIZE;
    private static final float FRAME_BUTTON_GAP = 4f;
    private static final float FRAME_ICON_RATIO = 0.42f;
    private static final float OUTLINE_ICON_RATIO = 0.55f;
    private static final float DISABLED_ICON_ALPHA = 0.35f;
    private static final float FRAME_BUTTONS_RESERVE = FRAME_BUTTON_W * 2f + FRAME_BUTTON_GAP;
    private static final float ACTION_ROW_H = 22f;
    private static final float CLEAR_BUTTON_W = 46f;
    private static final float CLEAR_BUTTON_HEIGHT_RATIO = 0.72f;
    private static final float SMALL_BUTTON_RATIO = 0.78f;
    private static final float UNIT_FIELD_WIDTH = 46f;
    private static final int UNIT_FIELD_LENGTH = 8;
    private static final int MAX_FRAME_TICKS = 20;
    private static final int MIN_PIXEL_SIZE = 1;
    private static final int MAX_PIXEL_SIZE = 10;
    private static final float CANVAS_SIZE = 160f;
    private static final float MIN_GRID_ZOOM = 1f;
    private static final float MAX_GRID_ZOOM = 4f;
    private static final float GRID_ZOOM_STEP = 0.35f;
    private static final int CARD_LAYER = PopoverHost.CONTENT_LAYER;
    private static final int EDITOR_CONTENT_LAYER = PopoverHost.CONTENT_LAYER + 1;
    private static final int NESTED_PANEL_LAYER = 40;
    private static final int NESTED_CONTENT_LAYER = NESTED_PANEL_LAYER + 1;
    private static final float NESTED_PICKER_EDGE_MARGIN = 4f;
    private static final float NESTED_PICKER_GAP = 14f;
    private static final float PANEL_REFRACTION_IOR = 1.25f;
    private static final int GRID_SIZE_PANEL_LAYER = NESTED_CONTENT_LAYER + 5;
    private static final float PICKER_BOX_ASPECT = 0.4f;
    private static final float PICKER_HUE_H = 10f;
    private static final float PICKER_HEX_ROW_H = 18f;
    private static final float PICKER_SWATCH_GAP = 3f;
    private static final float PICKER_SWATCH_ROW_LABEL_H = 10f;
    private static final float PICKER_SWATCH_LABEL_GAP = 3f;
    private static final List<PixelGrid.Resolution> GRID_SIZE_VALUES = List.of(PixelGrid.Resolution.values());
    private static final int HOVER_HIGHLIGHT = ColorUtils.withAlpha(Theme.get().label(), 0.25f);
    private static final int CHECKER_CELLS = 4;
    private static final int GRID_LINE_COLOR = ColorUtils.withAlpha(Theme.get().label(), 0.10f);
    private static final float UNSELECTED_THUMB_ALPHA = 0.45f;
    private static final float CENTER_RING_ALPHA = 0.55f;
    private final GlassStyle previewStyle;
    private final GlassStyle editPillStyle;
    private final GlassStyle hudPillStyle;
    private final long livePreviewFadeKey;
    private final WLButton addFrameButton;
    private final WLButton removeFrameButton;
    private final WLButton clearFrameButton;
    private final WLButton copyFrameButton;
    private final WLButton pasteFrameButton;
    private static PixelGrid frameClipboard;
    private final PopoverHost gridSizePopover = new PopoverHost(GRID_SIZE_PANEL_LAYER);
    private final PillSelectorWidget<PixelGrid.Resolution> gridSizePill;
    private final SliderControl speedSlider;
    private final SliderControl sizeSlider;
    private final ColorPickerBody colorPickerBody;
    private final long pickerTriggerFadeKey;
    private final long cardFadeKey;
    private float previewX0;
    private float previewY0;
    private float previewSize;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;
    private float hudPillX0;
    private float hudPillW;
    private float hudHoverAnim;
    private boolean popoverActive;
    private float hoverAnim;
    private int selectedFrame;
    private WLColor paintColor = new WLColor(255, 255, 255);

    private static int CHECKER_DARK() {
        return Theme.get().gridCheckerDark();
    }

    private static int CHECKER_LIGHT() {
        return Theme.get().gridCheckerLight();
    }

    private static int SELECTED_RING_COLOR() {
        return Theme.get().accent();
    }

    private static int SELECTED_THUMB_RING() {
        return Theme.get().label();
    }

    public PixelGridAnimationOptionWidget(final Option<PixelGridAnimation> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.previewStyle = ResetButton.knobStyle(ControlLayers.BASE).fadeKey(FADE_KEYS.incrementAndGet());
        this.editPillStyle = ResetButton.knobStyle(ControlLayers.BASE).fadeKey(FADE_KEYS.incrementAndGet());
        this.hudPillStyle = ResetButton.knobStyle(ControlLayers.BASE).fadeKey(FADE_KEYS.incrementAndGet());
        this.livePreviewFadeKey = FADE_KEYS.incrementAndGet();
        this.pickerTriggerFadeKey = FADE_KEYS.incrementAndGet();
        this.cardFadeKey = FADE_KEYS.incrementAndGet();
        final long colorPickerFadeKeyBase = FADE_KEYS.addAndGet(ColorPickerBody.FADE_KEY_COUNT) - ColorPickerBody.FADE_KEY_COUNT + 1;
        this.colorPickerBody = new ColorPickerBody(() -> this.paintColor, t -> this.mutatePaintColor(c -> { c.setHue(t); c.syncValueFromHsb(); }), (sat, val) -> this.mutatePaintColor(c -> { c.setSaturation(sat); c.setBrightness(val); c.syncValueFromHsb(); }), rgb -> this.paintColor = new WLColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF), this::applyPaintSwatch, this.uiScale, colorPickerFadeKeyBase, ColorPickerBody.Metrics.embeddedPicker(), new ColorPickerBody.Layers(NESTED_CONTENT_LAYER + 4, NESTED_CONTENT_LAYER + 3, NESTED_CONTENT_LAYER, NESTED_PANEL_LAYER));
        this.addFrameButton = WLButton.icon(0, 0, 1, 1, b -> this.addFrame());
        this.removeFrameButton = WLButton.icon(0, 0, 1, 1, b -> this.removeSelectedFrame());
        this.clearFrameButton = new WLButton(0, 0, 1, 1, "Clear", b -> this.clearSelectedFrame());
        this.copyFrameButton = new WLButton(0, 0, 1, 1, "", b -> this.copySelectedFrame());
        this.pasteFrameButton = new WLButton(0, 0, 1, 1, "", b -> this.pasteIntoSelectedFrame());
        for (final WLButton button : new WLButton[]{this.addFrameButton, this.removeFrameButton, this.clearFrameButton, this.copyFrameButton, this.pasteFrameButton}) {
            button.style().layer(EDITOR_CONTENT_LAYER);
        }
        this.gridSizePill = new PillSelectorWidget<>(this::currentFrameSize, this::resizeGrid, GRID_SIZE_VALUES, r -> r.cells() + " x " + r.cells(), PillSelectorWidget.defaultStyle().layer(EDITOR_CONTENT_LAYER).fadeKey(this.reserveFadeKeys()), this.uiScale, GRID_SIZE_PANEL_LAYER, (anchor, ax, ay, content) -> {
            var window = Minecraft.getInstance().getWindow();
            this.gridSizePopover.openAt(anchor, ax, ay, window.getGuiScaledWidth(), window.getGuiScaledHeight(), content);
        }, this.gridSizePopover::close).localAnchors();
        this.speedSlider = new SliderControl(() -> (double) frameTicks(this.option.getValue().getAnimationSpeed()), 1, MAX_FRAME_TICKS, 1, this.reserveFadeKeys(), ticks -> this.mutate(a -> a.setAnimationSpeed(MAX_FRAME_TICKS + 1 - (int) Math.round(ticks))), true, v -> Math.round(v) + (Math.round(v) == 1 ? " tick" : " ticks"), unitMetrics(), this.uiScale, EDITOR_CONTENT_LAYER);
        this.sizeSlider = new SliderControl(() -> (double) Math.max(MIN_PIXEL_SIZE, Math.min(MAX_PIXEL_SIZE, Math.round(this.option.getValue().getSize()))), MIN_PIXEL_SIZE, MAX_PIXEL_SIZE, 1, this.reserveFadeKeys(), size -> this.mutate(a -> a.setSize((float) Math.round(size))), true, v -> Math.round(v) + " px", unitMetrics(), this.uiScale, EDITOR_CONTENT_LAYER);
    }

    private static SliderControl.Metrics unitMetrics() {
        final SliderControl.Metrics base = SliderControl.Metrics.popover();
        return new SliderControl.Metrics(base.knobWidthRatio(), base.knobRestWidthRatio(), base.knobRestH(), base.knobPressH(), base.trackH(), UNIT_FIELD_WIDTH, base.valueGap(), base.fieldHeightRatio(), base.labelSize(), base.fieldTextSize(), base.fieldTextInset(), base.textColor(), UNIT_FIELD_LENGTH, false, base.fieldCapHeight());
    }

    private static int frameTicks(final int animationSpeed) {
        return Math.max(1, Math.min(MAX_FRAME_TICKS, MAX_FRAME_TICKS + 1 - animationSpeed));
    }

    private long reserveFadeKeys() {
        return FADE_KEYS.addAndGet(5) - 4;
    }

    private void mutate(final Consumer<PixelGridAnimation> mutator) {
        final PixelGridAnimation updated = this.option.getValue().copy();
        mutator.accept(updated);
        this.option.setValue(updated);
    }

    private void mutatePaintColor(final Consumer<WLColor> mutator) {
        final WLColor updated = this.paintColor.copy();
        mutator.accept(updated);
        this.paintColor = updated;
    }

    private void applyPaintSwatch(final int argb) {
        this.paintColor = new WLColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    private PixelGrid.Resolution currentFrameSize() {
        final PixelGrid frame = this.option.getValue().getFrame(this.selectedFrame);
        return frame == null ? PixelGrid.DEFAULT_RESOLUTION : frame.getResolution();
    }

    private void addFrame() {
        final List<PixelGrid> frames = this.option.getValue().getFrames();
        final PixelGrid template = frames.isEmpty() ? null : frames.get(0);
        final PixelGrid.Resolution resolution = template == null ? PixelGrid.DEFAULT_RESOLUTION : template.getResolution();
        this.mutate(a -> a.addFrame(PixelGrid.create(resolution).build()));
        this.selectedFrame = this.option.getValue().getFrames().size() - 1;
    }

    private void removeSelectedFrame() {
        if (this.option.getValue().getFrames().size() <= 1) {
            return;
        }
        final int index = this.selectedFrame;
        this.mutate(a -> a.removeFrame(index));
        this.selectedFrame = Math.max(0, Math.min(this.selectedFrame, this.option.getValue().getFrames().size() - 1));
    }

    private void copySelectedFrame() {
        final PixelGrid frame = this.option.getValue().getFrame(this.selectedFrame);
        if (frame == null) {
            return;
        }
        frameClipboard = frame.copy();
    }

    private void pasteIntoSelectedFrame() {
        final PixelGrid source = frameClipboard;
        if (source == null) {
            return;
        }
        final int index = this.selectedFrame;
        this.mutate(a -> {
            final PixelGrid target = a.getFrame(index);
            if (target == null) {
                return;
            }
            final int offsetX = (target.getWidth() - source.getWidth()) / 2;
            final int offsetY = (target.getHeight() - source.getHeight()) / 2;
            for (int y = 0; y < target.getHeight(); y++) {
                for (int x = 0; x < target.getWidth(); x++) {
                    final int sx = x - offsetX;
                    final int sy = y - offsetY;
                    final boolean inside = sx >= 0 && sy >= 0 && sx < source.getWidth() && sy < source.getHeight();
                    target.setColor(x, y, inside ? source.getColor(sx, sy) : 0);
                }
            }
        });
    }

    private void clearSelectedFrame() {
        final PixelGrid frame = this.option.getValue().getFrame(this.selectedFrame);
        if (frame == null) {
            return;
        }
        final PixelGrid.Resolution resolution = frame.getResolution();
        final int index = this.selectedFrame;
        this.mutate(a -> a.getFrames().set(index, PixelGrid.create(resolution).build()));
    }

    private void resizeGrid(final PixelGrid.Resolution target) {
        this.mutate(a -> {
            final List<PixelGrid> frames = a.getFrames();
            for (int i = 0; i < frames.size(); i++) {
                final PixelGrid old = frames.get(i);
                if (old.getResolution() != target) {
                    frames.set(i, old.resizedTo(target));
                }
            }
        });
    }

    private float pad() {
        return POP_PAD * this.uiScale;
    }

    private float gap() {
        return POP_GAP * this.uiScale;
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float previewSize = this.rowLabelHeight() * 0.8f;
        final float rowRight = this.getX() + this.getWidth() - PADDING * this.uiScale;
        final float pillTextSize = Type.FOOTNOTE.size() * this.uiScale;
        final float pillTextWidth = Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, pillTextSize);
        final float pillW = pillTextWidth + EDIT_PILL_H_PADDING * 2f * this.uiScale;
        final float pillH = previewSize;
        final float pillX1 = rowRight;
        final float pillX0 = pillX1 - pillW;
        final float pillY0 = this.getY() + (this.rowLabelHeight() - pillH) / 2f;
        this.pillX0 = pillX0;
        this.pillY0 = pillY0;
        this.pillW = pillW;
        this.pillH = pillH;
        float hudW = 0f;
        if (this.hasHudSpace()) {
            hudW = Fonts.SFPRO_DISPLAY.width(HUD_LABEL, pillTextSize) + EDIT_PILL_H_PADDING * 2f * this.uiScale;
        }
        this.hudPillW = hudW;
        this.hudPillX0 = pillX0 - EDIT_PILL_GAP * this.uiScale - hudW;
        final float x1 = (hudW > 0f ? this.hudPillX0 : pillX0) - EDIT_PILL_GAP * this.uiScale;
        final float x0 = x1 - previewSize;
        final float y0 = this.getY() + (this.rowLabelHeight() - previewSize) / 2f;
        this.previewX0 = x0;
        this.previewY0 = y0;
        this.previewSize = previewSize;
        graphics.glass()
            .dimensions(x0, y0, previewSize, previewSize)
            .cornerRadius(previewSize * PREVIEW_CORNER_RATIO)
            .style(this.previewStyle.scaled(this.uiScale).opacity(alpha))
            .extract();
        final float inset = 3f * this.uiScale;
        extractAnimationFitted(graphics, this.option.getValue(), x0 + inset, y0 + inset, previewSize - inset * 2f, previewSize - inset * 2f);
        this.extractHudPill(graphics, mouseX, mouseY, dt, alpha, available, pillY0, pillH);
        if (this.popoverActive) {
            this.hoverAnim = 0f;
            return;
        }
        final float swatchAlpha = alpha;
        final boolean hovered = available && this.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(this.isOverControl(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY), available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(pillX0, pillY0, pillW, pillH)
            .cornerRadius(pillH / 2f)
            .style(this.editPillStyle.scaled(this.uiScale).opacity(swatchAlpha))
            .hover(this.hoverAnim)
            .extract();
        this.extractEditPillLabel(graphics, swatchAlpha);
    }

    private void extractHudPill(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, boolean available, final float pillY0, final float pillH) {
        if (this.hudPillW <= 0f) {
            return;
        }
        final boolean hudHovered = available && this.isOverHudPill(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(this.isOverHudPill(mouseX, mouseY) && !coveredByOverlay(mouseX, mouseY), available);
        this.hudHoverAnim = MathUtils.approach(this.hudHoverAnim, hudHovered ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(this.hudPillX0, pillY0, this.hudPillW, pillH)
            .cornerRadius(pillH / 2f)
            .style(this.hudPillStyle.scaled(this.uiScale).opacity(alpha))
            .hover(this.hudHoverAnim)
            .extract();
        final float hudTextSize = Type.FOOTNOTE.size() * this.uiScale;
        final float hudTextWidth = Fonts.SFPRO_DISPLAY.width(HUD_LABEL, hudTextSize);
        final float capHeight = hudTextSize * 0.59043f;
        final float baseline = pillY0 + pillH / 2f + capHeight * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, HUD_LABEL, this.hudPillX0 + (this.hudPillW - hudTextWidth) / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, hudTextSize), hudTextSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private boolean hasHudSpace() {
        return this.option.getHudElement() != null;
    }

    private boolean isOverHudPill(final double mouseX, final double mouseY) {
        return this.hudPillW > 0f && mouseX >= this.hudPillX0 && mouseX < this.hudPillX0 + this.hudPillW && mouseY >= this.pillY0 && mouseY < this.pillY0 + this.pillH;
    }

    private void extractEditPillLabel(final WLGraphics2D graphics, final float alpha) {
        final float pillTextSize = Type.FOOTNOTE.size() * this.uiScale;
        final float pillTextWidth = Fonts.SFPRO_DISPLAY.width(EDIT_LABEL, pillTextSize);
        final float capHeight = pillTextSize * 0.59043f;
        final float baseline = this.pillY0 + this.pillH / 2f + capHeight * 0.5f;
        final float pillTextTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, pillTextSize);
        Fonts.SFPRO_DISPLAY.extract(graphics, EDIT_LABEL, this.pillX0 + (this.pillW - pillTextWidth) / 2f, pillTextTop, pillTextSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private static void extractAnimationFitted(final WLGraphics2D graphics, final PixelGridAnimation anim, final float boxX, final float boxY, final float boxW, final float boxH) {
        final PixelGrid frame = anim.getCurrentFrame();
        if (frame == null) {
            return;
        }
        final float contentW = frame.getWidth() * anim.getSize();
        final float contentH = frame.getHeight() * anim.getSize();
        if (contentW <= 0f || contentH <= 0f) {
            return;
        }
        final float scale = Math.min(boxW / contentW, boxH / contentH);
        final float x = boxX + (boxW - contentW * scale) / 2f;
        final float y = boxY + (boxH - contentH * scale) / 2f;
        extractLitCells(graphics, frame, x, y, anim.getSize() * scale, 1f);
    }

    private static void extractLitCells(final WLGraphics2D graphics, final PixelGrid grid, final float x0, final float y0, final float cell, final float alpha) {
        final int w = grid.getWidth();
        final int h = grid.getHeight();
        final Matrix3x2f pose = graphics.pose();
        float[] ex = DevicePixels.snapEdgesX(x0, cell, w, pose);
        float[] ey = DevicePixels.snapEdgesY(y0, cell, h, pose);
        final float minW = 1f / DevicePixels.perLogicalX();
        final float minH = 1f / DevicePixels.perLogicalY();
        for (int gy = 0; gy < h; gy++) {
            for (int gx = 0; gx < w; gx++) {
                final int color = grid.getColor(gx, gy);
                if (color == 0) {
                    continue;
                }
                final int c = ColorUtils.withAlpha(color, alpha);
                graphics.fill(ex[gx], ey[gy], Math.max(ex[gx + 1], ex[gx] + minW), Math.max(ey[gy + 1], ey[gy] + minH), c, c);
            }
        }
    }

    private boolean isOverControl(final double mouseX, final double mouseY) {
        final float slop = 2f * this.uiScale;
        return mouseX >= this.pillX0 - slop && mouseX <= this.pillX0 + this.pillW + slop && mouseY >= this.pillY0 - slop && mouseY <= this.pillY0 + this.pillH + slop;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || this.ctx == null || this.ctx.onOpenPopover() == null) {
            return false;
        }
        if (this.isOverHudPill(event.x(), event.y())) {
            var minecraft = Minecraft.getInstance();
            WLClient.setScreen(new HudEditScreen(WLClient.screen(), this.option.getHudSpace()));
            return true;
        }
        if (!this.isOverControl(event.x(), event.y())) {
            return false;
        }
        this.popoverActive = true;
        final LiquidRect anchor = new LiquidRect(this.pillX0, this.pillY0, this.pillW, this.pillH, this.pillH / 2f);
        this.ctx.onOpenPopover().accept(anchor, new AnimationEditorPopover());
        return true;
    }

    private void extractGrid(final WLGraphics2D graphics, final PixelGrid grid, final float x0, final float y0, final float cell, final float alpha) {
        final int w = grid.getWidth();
        final int h = grid.getHeight();
        final Matrix3x2f pose = graphics.pose();
        float[] ex = DevicePixels.snapEdgesX(x0, cell, w, pose);
        float[] ey = DevicePixels.snapEdgesY(y0, cell, h, pose);
        final int backing = ColorUtils.withAlpha(CHECKER_DARK(), alpha);
        graphics.fill(ex[0], ey[0], ex[w], ey[h], backing, backing);
        extractLitCells(graphics, grid, x0, y0, cell, alpha);
    }

    private void extractEditorGrid(final WLGraphics2D graphics, final PixelGrid grid, final float x0, final float y0, final float cell, final float alpha, final int hoverGx, final int hoverGy) {
        final int w = grid.getWidth();
        final int h = grid.getHeight();
        final Matrix3x2f pose = graphics.pose();
        float[] ex = DevicePixels.snapEdgesX(x0, cell, w, pose);
        float[] ey = DevicePixels.snapEdgesY(y0, cell, h, pose);
        final float gx0 = ex[0];
        final float gy0 = ey[0];
        final float x1 = ex[w];
        final float y1 = ey[h];
        final int paper = ColorUtils.withAlpha(CHECKER_LIGHT(), alpha);
        graphics.fill(gx0, gy0, x1, y1, paper, paper);
        final int checker = ColorUtils.withAlpha(CHECKER_DARK(), alpha);
        for (int by = 0; by * CHECKER_CELLS < h; by++) {
            for (int bx = 0; bx * CHECKER_CELLS < w; bx++) {
                if (((bx + by) & 1) != 0) {
                    continue;
                }
                final float bx0 = ex[bx * CHECKER_CELLS];
                final float by0 = ey[by * CHECKER_CELLS];
                final float bx1 = ex[Math.min(w, (bx + 1) * CHECKER_CELLS)];
                final float by1 = ey[Math.min(h, (by + 1) * CHECKER_CELLS)];
                graphics.fill(bx0, by0, bx1, by1, checker, checker);
            }
        }
        for (int gy = 0; gy < h; gy++) {
            for (int gx = 0; gx < w; gx++) {
                final int color = grid.getColor(gx, gy);
                if (color == 0) {
                    continue;
                }
                final int fill = ColorUtils.withAlpha(color, alpha);
                graphics.fill(ex[gx], ey[gy], ex[gx + 1], ey[gy + 1], fill, fill);
            }
        }
        final float line = main.walksy.lib.core.gui.WLScreen.hairlinePx();
        final int lineColor = ColorUtils.withAlpha(GRID_LINE_COLOR, alpha);
        for (int gx = 0; gx <= w; gx++) {
            final float lx = Math.min(ex[gx], x1 - line);
            graphics.fill(lx, gy0, lx + line, y1, lineColor, null);
        }
        for (int gy = 0; gy <= h; gy++) {
            final float ly = Math.min(ey[gy], y1 - line);
            graphics.fill(gx0, ly, x1, ly + line, lineColor, null);
        }
        if (hoverGx >= 0 && hoverGy >= 0) {
            graphics.fill(ex[hoverGx], ey[hoverGy], ex[hoverGx + 1], ey[hoverGy + 1], ColorUtils.withAlpha(HOVER_HIGHLIGHT, alpha), null);
        }
        final int centerGx = w / 2;
        final int centerGy = h / 2;
        final float ccx0 = ex[centerGx];
        final float ccy0 = ey[centerGy];
        final float ccx1 = ex[centerGx + 1];
        final float ccy1 = ey[centerGy + 1];
        final float ringT = line;
        final int ringColor = ColorUtils.withAlpha(SELECTED_RING_COLOR(), alpha * CENTER_RING_ALPHA);
        graphics.fill(ccx0, ccy0, ccx1, ccy0 + ringT, ringColor, null);
        graphics.fill(ccx0, ccy1 - ringT, ccx1, ccy1, ringColor, null);
        graphics.fill(ccx0, ccy0, ccx0 + ringT, ccy1, ringColor, null);
        graphics.fill(ccx1 - ringT, ccy0, ccx1, ccy1, ringColor, null);
    }

    private List<String> descriptionLines(final float panelWidth) {
        final OptionDescription desc = this.option.getDescription();
        if (desc == null || desc.getType() != OptionDescription.Type.TEXT) {
            return List.of();
        }
        final String text = desc.getText().get();
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return Fonts.SFPRO_DISPLAY.wrap(text, DESCRIPTION_SIZE * this.uiScale, panelWidth - this.pad() * 2f);
    }

    private float pickerSwatchSize(final int panelW) {
        final float contentWidth = panelW - this.pad() * 2f;
        return (contentWidth - (ColorPickerBody.SWATCH_COLUMNS - 1) * PICKER_SWATCH_GAP * this.uiScale) / ColorPickerBody.SWATCH_COLUMNS;
    }

    private record Layout(float titleTop, float titleSize, float descTop, List<String> descLines, float descLineHeight, float previewX0, float previewY0, float previewSize, float canvasInsetX, float canvasY0, float canvasSize, float actionY0, float actionH, float stripY0, float stripY1, float stripThumb, float stripGap, int stripPerRow, float cardY0, float cardY1, float rowH, float[] rowY, boolean showSpeed, float bottom) {

        float canvasX0(int panelX) {
            return panelX + canvasInsetX;
        }
        float canvasY1() {
            return canvasY0 + canvasSize;
        }

        float thumbX(int i, float x0) {
            return x0 + (i % stripPerRow) * (stripThumb + stripGap);
        }

        float thumbY(int i) {
            return stripY0 + (i / stripPerRow) * (stripThumb + stripGap);
        }

        float gridSizeY() {
            return rowY[0];
        }
        float paintY() {
            return rowY[1];
        }
        float speedY() {
            return rowY[2];
        }
        float sizeY() {
            return rowY[showSpeed ? 3 : 2];
        }
    }

    private static int fitPerRow(final float avail, final float thumb, final float gap) {
        return Math.max(1, (int) Math.floor((avail + gap) / (thumb + gap)));
    }

    private static int ceilDiv(final int a, final int b) {
        return (a + b - 1) / b;
    }

    private Layout computeLayout(final int panelY, final int panelW, final PixelGridAnimation anim) {
        final float scale = this.uiScale;
        final float available = panelW - this.pad() * 2f;
        final float blockGap = BLOCK_GAP * scale;
        final float previewSize = LIVE_PREVIEW_SIZE * scale;
        final float previewX0 = panelW - this.pad() - previewSize;
        final float previewY0 = panelY + this.pad();
        final float titleSize = Type.HEADLINE.size() * scale;
        final float capHeight = titleSize * 0.59043f;
        final float titleTop = Fonts.SFPRO_DISPLAY_BOLD.topForBaseline(previewY0 + capHeight, titleSize);
        final float descTop = titleTop + titleSize + 4f * scale;
        final List<String> descLines = this.descriptionLines(Math.round(panelW - previewSize - this.gap()));
        final float descLineHeight = Type.FOOTNOTE.lineHeight() * scale;
        final float headerBottom = Math.max(previewY0 + previewSize, descLines.isEmpty() ? titleTop + titleSize : descTop + descLines.size() * descLineHeight);
        final float canvasSize = Math.min(CANVAS_SIZE * scale, available);
        final float canvasInsetX = this.pad() + (available - canvasSize) / 2f;
        final float canvasY0 = headerBottom + blockGap;
        final float canvasY1 = canvasY0 + canvasSize;
        final float actionY0 = canvasY1 + 8f * scale;
        final float actionH = ACTION_ROW_H * scale;
        final float stripY0 = actionY0 + actionH + blockGap;
        final float thumbGap = THUMB_GAP * scale;
        final float stripAvail = available - FRAME_BUTTONS_RESERVE * scale - thumbGap;
        float thumb = THUMB_SIZE * scale;
        final int frameCount = Math.max(1, anim.getFrames().size());
        int perRow = fitPerRow(stripAvail, thumb, thumbGap);
        int stripRows = ceilDiv(frameCount, perRow);
        if (stripRows > MAX_STRIP_ROWS) {
            thumb = Math.max(MIN_THUMB_SIZE * scale, (stripAvail + thumbGap) / ceilDiv(frameCount, MAX_STRIP_ROWS) - thumbGap);
            perRow = fitPerRow(stripAvail, thumb, thumbGap);
            stripRows = ceilDiv(frameCount, perRow);
        }
        final float stripY1 = stripY0 + stripRows * (thumb + thumbGap) - thumbGap;
        final boolean showSpeed = anim.getFrames().size() > 1;
        final float rowH = CARD_ROW_H * scale;
        final int rows = showSpeed ? 4 : 3;
        final float cardY0 = stripY1 + blockGap;
        float[] rowY = new float[4];
        for (int i = 0; i < rows; i++) {
            rowY[i] = cardY0 + i * rowH;
        }
        final float cardY1 = cardY0 + rows * rowH;
        if (!showSpeed) {
            rowY[3] = cardY1;
        }
        final float bottom = cardY1 + this.pad();
        return new Layout(titleTop, titleSize, descTop, descLines, descLineHeight, previewX0, previewY0, previewSize, canvasInsetX, canvasY0, canvasSize, actionY0, actionH, stripY0, stripY1, thumb, thumbGap, perRow, cardY0, cardY1, rowH, rowY, showSpeed, bottom);
    }

    private record GridView(float originX, float originY, float cell, float vx0, float vy0, float size) {

        int[] cellAt(double mouseX, double mouseY, int cells) {
            if (mouseX < this.vx0 || mouseX >= this.vx0 + this.size || mouseY < this.vy0 || mouseY >= this.vy0 + this.size) {
                return null;
            }
            final int gx = (int) Math.floor((mouseX - this.originX) / this.cell);
            final int gy = (int) Math.floor((mouseY - this.originY) / this.cell);
            if (gx < 0 || gx >= cells || gy < 0 || gy >= cells) {
                return null;
            }
            return new int[]{gx, gy};
        }
    }

    private static boolean inRect(final double x, final double y, final float x0, final float y0, final float x1, final float y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    private final class AnimationEditorPopover implements PopoverHost.PopoverContent {
        private final PopoverHost colorPopover = new PopoverHost(NESTED_PANEL_LAYER);
        private float triggerSwatchX0;
        private float triggerSwatchY0;
        private float triggerSwatchSize;
        private boolean painting;
        private boolean paintErasing;
        private SliderControl draggingSlider;
        private float gridZoom = 1f;
        private float panX;
        private float panY;

        private GridView gridView(final Layout l, final int panelX, final PixelGrid frame) {
            final float window = l.canvasSize();
            final float cell = window / frame.getWidth() * this.gridZoom;
            final float span = cell * frame.getWidth();
            final float slack = Math.max(0f, (span - window) / 2f);
            this.panX = Math.max(-slack, Math.min(slack, this.panX));
            this.panY = Math.max(-slack, Math.min(slack, this.panY));
            final float vx0 = l.canvasX0(panelX);
            final float vy0 = l.canvasY0();
            return new GridView(vx0 + (window - span) / 2f + this.panX, vy0 + (window - span) / 2f + this.panY, cell, vx0, vy0, window);
        }

        @Override
        public int preferredWidth() {
            return Math.round(POPOVER_WIDTH * PixelGridAnimationOptionWidget.this.uiScale);
        }

        @Override
        public float panelRefractionFactor() {
            return PANEL_REFRACTION_IOR;
        }

        @Override
        public float reserveRightPx() {
            return (NESTED_PICKER_GAP + ColorPickerPopover.WIDTH + NESTED_PICKER_EDGE_MARGIN) * PixelGridAnimationOptionWidget.this.uiScale;
        }

        @Override
        public int preferredHeight() {
            return Math.round(PixelGridAnimationOptionWidget.this.computeLayout(0, preferredWidth(), PixelGridAnimationOptionWidget.this.option.getValue()).bottom());
        }

        @Override
        public boolean growFromTop() {
            return true;
        }

        @Override
        public GlassStyle originStyle() {
            return PixelGridAnimationOptionWidget.this.editPillStyle.scaled(PixelGridAnimationOptionWidget.this.uiScale)
                .layer(PopoverHost.PANEL_LAYER);
        }

        @Override
        public void onClosed() {
            PixelGridAnimationOptionWidget.this.popoverActive = false;
        }

        private SliderControl[] sliders() {
            return PixelGridAnimationOptionWidget.this.option.getValue()
                .getFrames()
                .size() > 1 ? new SliderControl[]{PixelGridAnimationOptionWidget.this.speedSlider, PixelGridAnimationOptionWidget.this.sizeSlider} : new SliderControl[]{PixelGridAnimationOptionWidget.this.sizeSlider};
        }

        private void paintCell(final int gx, final int gy) {
            final PixelGrid frame = PixelGridAnimationOptionWidget.this.option.getValue().getFrame(PixelGridAnimationOptionWidget.this.selectedFrame);
            if (frame == null) {
                return;
            }
            final int target = this.paintErasing ? 0 : (PixelGridAnimationOptionWidget.this.paintColor.getRGB() | 0xFF000000);
            if (frame.getColor(gx, gy) == target) {
                return;
            }
            final int index = PixelGridAnimationOptionWidget.this.selectedFrame;
            PixelGridAnimationOptionWidget.this.mutate(a -> a.getFrames().get(index).setColor(gx, gy, target));
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final PixelGridAnimation anim = PixelGridAnimationOptionWidget.this.option.getValue();
            final List<PixelGrid> frames = anim.getFrames();
            PixelGridAnimationOptionWidget.this.selectedFrame = Math.max(0, Math.min(PixelGridAnimationOptionWidget.this.selectedFrame, frames.size() - 1));
            final Layout l = PixelGridAnimationOptionWidget.this.computeLayout(panelY, panelW, anim);
            final float x0 = panelX + PixelGridAnimationOptionWidget.this.pad();
            final float x1 = panelX + panelW - PixelGridAnimationOptionWidget.this.pad();
            final float cx = (x0 + x1) / 2f;
            final Matrix3x2fStack pose = graphics.pose();
            final float dtSeconds = delta / 20f;
            final WLRect outerScissor = graphics.scissor();
            graphics.disableScissor();
            this.colorPopover.extract(graphics, mouseX, mouseY, delta);
            if (outerScissor != null) {
                graphics.enableScissor(outerScissor.left(), outerScissor.top(), outerScissor.left() + outerScissor.width(), outerScissor.top() + outerScissor.height());
            }
            graphics.disableScissor();
            PixelGridAnimationOptionWidget.this.gridSizePopover.extract(graphics, mouseX, mouseY, delta);
            if (outerScissor != null) {
                graphics.enableScissor(outerScissor.left(), outerScissor.top(), outerScissor.left() + outerScissor.width(), outerScissor.top() + outerScissor.height());
            }
            final boolean showSpeed = l.showSpeed();
            final int sections = 4;
            int section = 0;
            float a;
            final float headerCy = l.previewY0() + l.previewSize() / 2f;
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, sections), cx, headerCy, PixelGridAnimationOptionWidget.this.uiScale);
            Fonts.SFPRO_DISPLAY_BOLD.extract(graphics, PixelGridAnimationOptionWidget.this.option.getName(), x0, l.titleTop(), l.titleSize(), ColorUtils.withAlpha(LABEL_COLOR(), a));
            final float descSize = Type.FOOTNOTE.size() * PixelGridAnimationOptionWidget.this.uiScale;
            for (int i = 0; i < l.descLines().size(); i++) {
                final float lineTop = Fonts.SFPRO_DISPLAY.topForBaseline(l.descTop() + i * l.descLineHeight() + descSize * 0.59043f, descSize);
                Fonts.SFPRO_DISPLAY.extract(graphics, l.descLines().get(i), x0, lineTop, descSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), a));
            }
            final float previewX = panelX + l.previewX0();
            graphics.glass()
                .dimensions(previewX, l.previewY0(), l.previewSize(), l.previewSize())
                .cornerRadius(8f * PixelGridAnimationOptionWidget.this.uiScale)
                .style(new GlassStyle()
                    .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
                    .tint(Theme.get().controlTint(), Theme.get().fillTertiary())
                    .refractionStrength(0f)
                    .refractionThickness(0f)
                    .refractionFactor(0f)
                    .refractionDispersion(0f)
                    .fresnelFactor(0f)
                    .glareFactor(0f)
                    .layer(EDITOR_CONTENT_LAYER)
                    .fadeKey(PixelGridAnimationOptionWidget.this.livePreviewFadeKey)
                    .scaled(PixelGridAnimationOptionWidget.this.uiScale)
                    .opacity(a))
                .extract();
            final float previewInset = 4f * PixelGridAnimationOptionWidget.this.uiScale;
            extractAnimationFitted(graphics, anim, previewX + previewInset, l.previewY0() + previewInset, l.previewSize() - previewInset * 2f, l.previewSize() - previewInset * 2f);
            Materialize.exit(pose);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, sections), cx, (l.canvasY0() + l.canvasY1()) / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            final PixelGrid selected = anim.getFrame(PixelGridAnimationOptionWidget.this.selectedFrame);
            if (selected != null) {
                final GridView view = this.gridView(l, panelX, selected);
                int[] hover = view.cellAt(mouseX, mouseY, selected.getWidth());
                final boolean clipped = this.gridZoom > 1f;
                if (clipped) {
                    graphics.enableScissor(Math.round(view.vx0()), Math.round(view.vy0()), Math.round(view.vx0() + view.size()), Math.round(view.vy0() + view.size()));
                }
                PixelGridAnimationOptionWidget.this.extractEditorGrid(graphics, selected, view.originX(), view.originY(), view.cell(), a, hover == null ? -1 : hover[0], hover == null ? -1 : hover[1]);
                if (clipped) {
                    graphics.disableScissor();
                }
            }
            final float actionH = l.actionH();
            final float actionGap = FRAME_BUTTON_GAP * PixelGridAnimationOptionWidget.this.uiScale;
            final float clearW = CLEAR_BUTTON_W * PixelGridAnimationOptionWidget.this.uiScale;
            PixelGridAnimationOptionWidget.this.pasteFrameButton.active = frameClipboard != null;
            final float actionSize = actionH * SMALL_BUTTON_RATIO;
            final float actionY = l.actionY0() + (actionH - actionSize) / 2f;
            this.layoutFrameButton(PixelGridAnimationOptionWidget.this.copyFrameButton, x0, actionY, actionSize, actionSize, a);
            this.layoutFrameButton(PixelGridAnimationOptionWidget.this.pasteFrameButton, x0 + actionSize + actionGap, actionY, actionSize, actionSize, a);
            final float clearH = actionH * CLEAR_BUTTON_HEIGHT_RATIO;
            this.layoutFrameButton(PixelGridAnimationOptionWidget.this.clearFrameButton, x1 - clearW, l.actionY0() + (actionH - clearH) / 2f, clearW, clearH, a);
            graphics.widget(PixelGridAnimationOptionWidget.this.copyFrameButton, mouseX, mouseY, delta);
            graphics.widget(PixelGridAnimationOptionWidget.this.pasteFrameButton, mouseX, mouseY, delta);
            graphics.widget(PixelGridAnimationOptionWidget.this.clearFrameButton, mouseX, mouseY, delta);
            this.extractFrameButtonIcon(graphics, Icons.COPY, PixelGridAnimationOptionWidget.this.copyFrameButton, a, 0f, OUTLINE_ICON_RATIO);
            this.extractFrameButtonIcon(graphics, Icons.PASTE, PixelGridAnimationOptionWidget.this.pasteFrameButton, a * (frameClipboard != null ? 1f : DISABLED_ICON_ALPHA), 0f, OUTLINE_ICON_RATIO);
            final String frameLabel = "Frame " + (PixelGridAnimationOptionWidget.this.selectedFrame + 1) + " of " + frames.size();
            final float counterSize = Type.CAPTION1.size() * PixelGridAnimationOptionWidget.this.uiScale;
            final float counterW = Fonts.SFPRO_DISPLAY.width(frameLabel, counterSize);
            final float counterBaseline = l.actionY0() + actionH / 2f + counterSize * 0.59043f / 2f;
            Fonts.SFPRO_DISPLAY.extract(graphics, frameLabel, cx - counterW / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(counterBaseline, counterSize), counterSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), a));
            Materialize.exit(pose);
            final float thumbSize = l.stripThumb();
            final float buttonsReserve = FRAME_BUTTONS_RESERVE * PixelGridAnimationOptionWidget.this.uiScale;
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, sections), cx, (l.stripY0() + l.stripY1()) / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            for (int i = 0; i < frames.size(); i++) {
                final float fx = l.thumbX(i, x0);
                final float fy = l.thumbY(i);
                final boolean isSelected = i == PixelGridAnimationOptionWidget.this.selectedFrame;
                PixelGridAnimationOptionWidget.this.extractGrid(graphics, frames.get(i), fx, fy, thumbSize / Math.max(1, frames.get(i).getWidth()), isSelected ? a : a * UNSELECTED_THUMB_ALPHA);
                if (isSelected) {
                    final Matrix3x2f ringPose = graphics.pose();
                    final float ringGap = 1f * PixelGridAnimationOptionWidget.this.uiScale;
                    float ringT = Math.max(main.walksy.lib.core.gui.WLScreen.hairlinePx(), 1f * PixelGridAnimationOptionWidget.this.uiScale);
                    ringT = DevicePixels.snapSizeY(ringT);
                    final float r0x = DevicePixels.snapX(fx - ringGap - ringT, ringPose);
                    final float r0y = DevicePixels.snapY(fy - ringGap - ringT, ringPose);
                    final float r1x = DevicePixels.snapX(fx + thumbSize + ringGap + ringT, ringPose);
                    final float r1y = DevicePixels.snapY(fy + thumbSize + ringGap + ringT, ringPose);
                    final int ring = ColorUtils.withAlpha(SELECTED_THUMB_RING(), a);
                    graphics.fill(r0x, r0y, r1x, r0y + ringT, ring, null);
                    graphics.fill(r0x, r1y - ringT, r1x, r1y, ring, null);
                    graphics.fill(r0x, r0y, r0x + ringT, r1y, ring, null);
                    graphics.fill(r1x - ringT, r0y, r1x, r1y, ring, null);
                }
            }
            final float buttonW = FRAME_BUTTON_W * PixelGridAnimationOptionWidget.this.uiScale;
            final float buttonGap = FRAME_BUTTON_GAP * PixelGridAnimationOptionWidget.this.uiScale;
            final float buttonsX0 = x1 - buttonsReserve;
            final float buttonH = THUMB_SIZE * PixelGridAnimationOptionWidget.this.uiScale;
            PixelGridAnimationOptionWidget.this.removeFrameButton.active = frames.size() > 1;
            final float smallW = buttonW * SMALL_BUTTON_RATIO;
            final float smallH = buttonH * SMALL_BUTTON_RATIO;
            final float smallY = l.stripY0() + (buttonH - smallH) / 2f;
            this.layoutFrameButton(PixelGridAnimationOptionWidget.this.addFrameButton, buttonsX0 + (buttonW - smallW) / 2f, smallY, smallW, smallH, a);
            this.layoutFrameButton(PixelGridAnimationOptionWidget.this.removeFrameButton, buttonsX0 + buttonW + buttonGap + (buttonW - smallW) / 2f, smallY, smallW, smallH, a);
            graphics.widget(PixelGridAnimationOptionWidget.this.addFrameButton, mouseX, mouseY, delta);
            graphics.widget(PixelGridAnimationOptionWidget.this.removeFrameButton, mouseX, mouseY, delta);
            this.extractFrameButtonIcon(graphics, Icons.PLUS, PixelGridAnimationOptionWidget.this.addFrameButton, a, 0f);
            this.extractFrameButtonIcon(graphics, Icons.MINUS, PixelGridAnimationOptionWidget.this.removeFrameButton, a * (frames.size() > 1 ? 1f : DISABLED_ICON_ALPHA), 0f);
            Materialize.exit(pose);
            final float cardBeat = Materialize.stagger(reveal, section++, sections);
            final float cardCy = (l.cardY0() + l.cardY1()) / 2f;
            a = Materialize.enter(pose, cardBeat, cx, cardCy, PixelGridAnimationOptionWidget.this.uiScale);
            graphics.glass()
                .dimensions(x0, l.cardY0(), x1 - x0, l.cardY1() - l.cardY0())
                .cornerRadius(CARD_RADIUS * PixelGridAnimationOptionWidget.this.uiScale)
                .style(this.cardStyle().scaled(PixelGridAnimationOptionWidget.this.uiScale).opacity(a))
                .extract();
            final float hair = main.walksy.lib.core.gui.WLScreen.hairlinePx();
            final float rowInset = 10f * PixelGridAnimationOptionWidget.this.uiScale;
            final int rowCount = showSpeed ? 4 : 3;
            for (int i = 1; i < rowCount; i++) {
                final float sy = l.cardY0() + i * l.rowH();
                graphics.fill(x0 + rowInset, sy, x1 - rowInset, sy + hair, ColorUtils.withAlpha(Theme.get().separator(), a), null);
            }
            final float rowLabelSize = Type.SUBHEADLINE.size() * PixelGridAnimationOptionWidget.this.uiScale;
            final float rowTextX = x0 + rowInset;
            final float rowCtrlX1 = x1 - rowInset;
            this.extractCardRowLabel(graphics, "Grid Size", rowTextX, l.gridSizeY(), l.rowH(), rowLabelSize, a);
            Materialize.exit(pose);
            PixelGridAnimationOptionWidget.this.gridSizePill.extract(graphics, a, rowCtrlX1, l.gridSizeY(), l.rowH(), mouseX, mouseY, delta);
            a = Materialize.enter(pose, cardBeat, cx, cardCy, PixelGridAnimationOptionWidget.this.uiScale);
            this.extractCardRowLabel(graphics, "Paint Color", rowTextX, l.paintY(), l.rowH(), rowLabelSize, a);
            final float swatchSize = l.rowH() * 0.5f;
            final float swatchX0 = rowCtrlX1 - swatchSize;
            final float swatchY0 = l.paintY() + (l.rowH() - swatchSize) / 2f;
            this.triggerSwatchX0 = swatchX0;
            this.triggerSwatchY0 = swatchY0;
            this.triggerSwatchSize = swatchSize;
            if (!this.colorPopover.isOpen()) {
                WLRenderSystem.cursors().requestIf(mouseX >= swatchX0 && mouseX < swatchX0 + swatchSize && mouseY >= swatchY0 && mouseY < swatchY0 + swatchSize, Cursors.Cursor.POINTER);
                graphics.glass()
                    .dimensions(swatchX0, swatchY0, swatchSize, swatchSize)
                    .cornerRadius(swatchSize * 0.3f)
                    .style(new GlassStyle()
                        .blurRadius(LiquidGlassConstants.BLUR_POPOVER)
                        .layer(EDITOR_CONTENT_LAYER)
                        .fadeKey(PixelGridAnimationOptionWidget.this.pickerTriggerFadeKey)
                        .tint(PixelGridAnimationOptionWidget.this.paintColor.getRGB() & 0x00FFFFFF, 1.25f)
                        .shadow(0f, 0f, 0f, 0f)
                        .shadowColor(Theme.get().shadowColor(), 0f)
                        .scaled(PixelGridAnimationOptionWidget.this.uiScale)
                        .opacity(a))
                    .extract();
            }
            final float ctrlX0 = rowTextX + SETTINGS_LABEL_COLUMN * PixelGridAnimationOptionWidget.this.uiScale;
            if (showSpeed) {
                this.extractCardRowLabel(graphics, "Frame Time", rowTextX, l.speedY(), l.rowH(), rowLabelSize, a);
                PixelGridAnimationOptionWidget.this.speedSlider.extractInRow(graphics, dtSeconds, ctrlX0, rowCtrlX1, l.speedY(), l.rowH(), a, mouseX, mouseY, delta);
            }
            this.extractCardRowLabel(graphics, "Pixel Size", rowTextX, l.sizeY(), l.rowH(), rowLabelSize, a);
            PixelGridAnimationOptionWidget.this.sizeSlider.extractInRow(graphics, dtSeconds, ctrlX0, rowCtrlX1, l.sizeY(), l.rowH(), a, mouseX, mouseY, delta);
            Materialize.exit(pose);
        }

        private GlassStyle cardStyle() {
            return PopoverHost.surfaceStyle().layer(CARD_LAYER).fadeKey(PixelGridAnimationOptionWidget.this.cardFadeKey);
        }

        private void layoutFrameButton(final WLButton button, final float x, final float y, final float w, final float h, final float alpha) {
            button.setX(Math.round(x));
            button.setY(Math.round(y));
            button.setWidth(Math.round(w));
            button.setHeight(Math.round(h));
            button.setAlpha(alpha);
            button.setCornerRadius(Math.min(w, h) / 2f);
            button.setTextSize(Math.max(Type.CAPTION1.size() * PixelGridAnimationOptionWidget.this.uiScale, h * 0.5f));
        }

        private void extractFrameButtonIcon(final WLGraphics2D graphics, final Icons.Icon icon, final WLButton button, final float alpha, final float rotationDeg) {
            this.extractFrameButtonIcon(graphics, icon, button, alpha, rotationDeg, FRAME_ICON_RATIO);
        }

        private void extractFrameButtonIcon(final WLGraphics2D graphics, final Icons.Icon icon, final WLButton button, final float alpha, final float rotationDeg, final float ratio) {
            final float size = Math.min(button.getWidth(), button.getHeight()) * ratio;
            final float ix = button.getX() + (button.getWidth() - size) / 2f;
            final float iy = button.getY() + (button.getHeight() - size) / 2f;
            final int color = ColorUtils.withAlpha(Theme.get().label(), alpha);
            button.stretchContent(() -> {
                if (rotationDeg == 0f) {
                    icon.extract(graphics, ix, iy, size, color);
                } else {
                    icon.extractRotated(graphics, ix, iy, size, color, rotationDeg);
                }
            });
        }

        private void extractCardRowLabel(final WLGraphics2D graphics, final String text, final float x, final float rowY, final float rowH, final float size, final float alpha) {
            final float cap = size * 0.59043f;
            final float baseline = rowY + rowH / 2f + cap * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, text, x, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(Theme.get().label(), alpha));
        }

        @Override
        public boolean mouseClickedOutside(final MouseButtonEvent event, final boolean doubleClick) {
            if (this.colorPopover.isOpen() && this.colorPopover.mouseClicked(event, doubleClick)) {
                return true;
            }
            return PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen() && PixelGridAnimationOptionWidget.this.gridSizePopover.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.mouseClicked(event, doubleClick);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.mouseClicked(event, doubleClick);
            }
            final int button = event.button();
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            final PixelGridAnimation anim = PixelGridAnimationOptionWidget.this.option.getValue();
            final List<PixelGrid> frames = anim.getFrames();
            final Layout l = PixelGridAnimationOptionWidget.this.computeLayout(panelY, panelW, anim);
            final double mx = event.x(), my = event.y();
            final float x0 = panelX + PixelGridAnimationOptionWidget.this.pad();
            final float x1 = panelX + panelW - PixelGridAnimationOptionWidget.this.pad();
            final PixelGrid painted = anim.getFrame(PixelGridAnimationOptionWidget.this.selectedFrame);
            int[] cell = painted == null ? null : this.gridView(l, panelX, painted).cellAt(mx, my, painted.getWidth());
            if (cell != null) {
                this.paintErasing = button == InputConstants.MOUSE_BUTTON_RIGHT;
                this.painting = true;
                this.paintCell(cell[0], cell[1]);
                return true;
            }
            if (button != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            SliderControl clickedField = null;
            for (SliderControl slider : this.sliders()) {
                if (slider.isOverField(mx, my)) {
                    clickedField = slider;
                }
            }
            for (SliderControl slider : this.sliders()) {
                slider.setFieldFocused(slider == clickedField);
            }
            if (clickedField != null) {
                clickedField.fieldClicked(event, doubleClick);
                return true;
            }
            for (SliderControl slider : this.sliders()) {
                if (slider.isOverTrack(mx, my)) {
                    this.draggingSlider = slider;
                    slider.startDrag(mx);
                    return true;
                }
            }
            if (inRect(mx, my, x0, l.gridSizeY(), x1, l.gridSizeY() + l.rowH())) {
                PixelGridAnimationOptionWidget.this.gridSizePill.open();
                return true;
            }
            final float thumbSize = l.stripThumb();
            for (int i = 0; i < frames.size(); i++) {
                final float fx = l.thumbX(i, x0);
                final float fy = l.thumbY(i);
                if (inRect(mx, my, fx, fy, fx + thumbSize, fy + thumbSize)) {
                    PixelGridAnimationOptionWidget.this.selectedFrame = i;
                    return true;
                }
            }
            if (PixelGridAnimationOptionWidget.this.addFrameButton.isMouseOver(mx, my)) {
                return PixelGridAnimationOptionWidget.this.addFrameButton.mouseClicked(event, doubleClick);
            }
            if (PixelGridAnimationOptionWidget.this.removeFrameButton.isMouseOver(mx, my)) {
                return PixelGridAnimationOptionWidget.this.removeFrameButton.mouseClicked(event, doubleClick);
            }
            if (PixelGridAnimationOptionWidget.this.copyFrameButton.isMouseOver(mx, my)) {
                return PixelGridAnimationOptionWidget.this.copyFrameButton.mouseClicked(event, doubleClick);
            }
            if (PixelGridAnimationOptionWidget.this.pasteFrameButton.isMouseOver(mx, my)) {
                return PixelGridAnimationOptionWidget.this.pasteFrameButton.mouseClicked(event, doubleClick);
            }
            if (PixelGridAnimationOptionWidget.this.clearFrameButton.isMouseOver(mx, my)) {
                return PixelGridAnimationOptionWidget.this.clearFrameButton.mouseClicked(event, doubleClick);
            }
            if (inRect(mx, my, x0, l.paintY(), x1, l.paintY() + l.rowH())) {
                final LiquidRect triggerAnchor = new LiquidRect(this.triggerSwatchX0, this.triggerSwatchY0, this.triggerSwatchSize, this.triggerSwatchSize, this.triggerSwatchSize * 0.3f);
                var window = Minecraft.getInstance().getWindow();
                final int screenW = window.getGuiScaledWidth();
                final int screenH = window.getGuiScaledHeight();
                final ColorPickerPopover picker = new ColorPickerPopover(this.colorPopover);
                final int pickerW = picker.preferredWidth();
                final int gap = Math.round(NESTED_PICKER_GAP * PixelGridAnimationOptionWidget.this.uiScale);
                final int toRight = Math.round(panelX + panelW) + gap;
                final int edgeMargin = Math.round(NESTED_PICKER_EDGE_MARGIN * PixelGridAnimationOptionWidget.this.uiScale);
                int boundsX = Math.min(toRight, screenW - edgeMargin - pickerW);
                if (boundsX < Math.round(panelX)) {
                    boundsX = Math.round(panelX) - gap - pickerW;
                }
                final int pickerH = picker.preferredHeight();
                final int margin = Math.round(PopoverHost.screenMargin());
                int boundsY = Math.round(this.triggerSwatchY0 + this.triggerSwatchSize / 2f - pickerH / 2f);
                boundsY = Math.max(margin, Math.min(boundsY, screenH - margin - pickerH));
                this.colorPopover.openCentered(triggerAnchor, boundsX, boundsY, pickerW, pickerH, screenW, screenH, picker);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.mouseDragged(event, dragX, dragY);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.mouseDragged(event, dragX, dragY);
            }
            if (this.painting) {
                final PixelGridAnimation anim = PixelGridAnimationOptionWidget.this.option.getValue();
                final Layout l = PixelGridAnimationOptionWidget.this.computeLayout(panelY, panelW, anim);
                final PixelGrid painted = anim.getFrame(PixelGridAnimationOptionWidget.this.selectedFrame);
                int[] cell = painted == null ? null : this.gridView(l, panelX, painted).cellAt(event.x(), event.y(), painted.getWidth());
                if (cell != null) {
                    this.paintCell(cell[0], cell[1]);
                }
                return true;
            }
            if (this.draggingSlider != null) {
                this.draggingSlider.dragTo(event.x());
                return true;
            }
            for (SliderControl slider : this.sliders()) {
                if (slider.isFieldFocused()) {
                    return slider.fieldDragged(event, dragX, dragY);
                }
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            final PixelGridAnimation anim = PixelGridAnimationOptionWidget.this.option.getValue();
            final PixelGrid selected = anim.getFrame(PixelGridAnimationOptionWidget.this.selectedFrame);
            if (selected == null) {
                return false;
            }
            final Layout l = PixelGridAnimationOptionWidget.this.computeLayout(panelY, panelW, anim);
            final GridView before = this.gridView(l, panelX, selected);
            if (mouseX < before.vx0() || mouseX >= before.vx0() + before.size() || mouseY < before.vy0() || mouseY >= before.vy0() + before.size()) {
                return false;
            }
            final int cells = selected.getWidth();
            final float gridX = (float) (mouseX - before.originX()) / before.cell();
            final float gridY = (float) (mouseY - before.originY()) / before.cell();
            this.gridZoom = Math.max(MIN_GRID_ZOOM, Math.min(MAX_GRID_ZOOM, this.gridZoom + (float) scrollY * GRID_ZOOM_STEP));
            final float window = l.canvasSize();
            final float cellAfter = window / cells * this.gridZoom;
            final float spanAfter = cellAfter * cells;
            final float centeredX = before.vx0() + (window - spanAfter) / 2f;
            final float centeredY = before.vy0() + (window - spanAfter) / 2f;
            this.panX = (float) (mouseX - gridX * cellAfter) - centeredX;
            this.panY = (float) (mouseY - gridY * cellAfter) - centeredY;
            return true;
        }

        @Override
        public boolean mouseReleased(final MouseButtonEvent event) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.mouseReleased(event);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.mouseReleased(event);
            }
            this.painting = false;
            if (this.draggingSlider != null) {
                this.draggingSlider.release();
                this.draggingSlider = null;
            }
            return true;
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.keyPressed(event);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.keyPressed(event);
            }
            for (SliderControl slider : this.sliders()) {
                if (slider.isFieldFocused()) {
                    return slider.fieldKeyPressed(event);
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            if (this.colorPopover.isOpen()) {
                return this.colorPopover.charTyped(event);
            }
            if (PixelGridAnimationOptionWidget.this.gridSizePopover.isOpen()) {
                return PixelGridAnimationOptionWidget.this.gridSizePopover.charTyped(event);
            }
            for (SliderControl slider : this.sliders()) {
                if (slider.isFieldFocused()) {
                    return slider.fieldCharTyped(event);
                }
            }
            return false;
        }
}

    private final class ColorPickerPopover implements PopoverHost.PopoverContent {
        private static final float WIDTH = 200f;
        private static final float PAD = 14f;
        private static final float GAP = 8f;
        private final PopoverHost host;
        private Drag drag = Drag.NONE;
        private static final int SECTIONS = 6;

        private enum Drag {NONE, BOX, HUE}

        ColorPickerPopover(PopoverHost host) {
            this.host = host;
        }

        private float pad() {
            return PAD * PixelGridAnimationOptionWidget.this.uiScale;
        }

        private float gap() {
            return GAP * PixelGridAnimationOptionWidget.this.uiScale;
        }

        @Override
        public int preferredWidth() {
            return Math.round(WIDTH * PixelGridAnimationOptionWidget.this.uiScale);
        }

        @Override
        public int preferredHeight() {
            final int w = this.preferredWidth();
            final float available = w - this.pad() * 2f;
            final float swSize = PixelGridAnimationOptionWidget.this.pickerSwatchSize(w);
            final float h = this.pad() + available * PICKER_BOX_ASPECT + this.gap() + PICKER_HUE_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap() + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap() + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap() + PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale + swSize + this.gap() + PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale + swSize + this.pad();
            return Math.round(h);
        }

        @Override
        public float panelRefractionFactor() {
            return PANEL_REFRACTION_IOR;
        }

        @Override
        public GlassStyle originStyle() {
            return new GlassStyle()
                .blurRadius(LiquidGlassConstants.BLUR_POPOVER)
                .layer(this.host.panelLayer())
                .tint(PixelGridAnimationOptionWidget.this.paintColor.getRGB() & 0x00FFFFFF, 1.25f);
        }

        @Override
        public int bloomColor() {
            return 0x50000000 | (PixelGridAnimationOptionWidget.this.paintColor.getRGB() & 0x00FFFFFF);
        }

        private record PickerLayout(float boxY0, float boxY1, float hueY0, float hexY0, float rgbY0, float popularLabelY, float popularSwatchY, float favoritesLabelY, float favoritesSwatchY) {
        }

        private PickerLayout layout(final int panelY, final int panelW) {
            final float available = panelW - this.pad() * 2f;
            final float swSize = PixelGridAnimationOptionWidget.this.pickerSwatchSize(panelW);
            final float boxY0 = panelY + this.pad();
            final float boxY1 = boxY0 + available * PICKER_BOX_ASPECT;
            final float hueY0 = boxY1 + this.gap();
            final float hexY0 = hueY0 + PICKER_HUE_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap();
            final float rgbY0 = hexY0 + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap();
            final float popularLabelY = rgbY0 + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale + this.gap();
            final float popularSwatchY = popularLabelY + PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale;
            final float favoritesLabelY = popularSwatchY + swSize + this.gap();
            final float favoritesSwatchY = favoritesLabelY + PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale;
            return new PickerLayout(boxY0, boxY1, hueY0, hexY0, rgbY0, popularLabelY, popularSwatchY, favoritesLabelY, favoritesSwatchY);
        }

        @Override
        public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
            final float x0 = panelX + this.pad();
            final float x1 = panelX + panelW - this.pad();
            final float cx = (x0 + x1) / 2f;
            final PickerLayout l = this.layout(panelY, panelW);
            final float dtSeconds = delta / 20f;
            var pose = graphics.pose();
            int section = 0;
            float a;
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, (l.boxY0() + l.boxY1()) / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractBox(graphics, dtSeconds, x0, x1, l.boxY0(), l.boxY1(), this.drag == Drag.BOX, mouseX, mouseY, a);
            Materialize.exit(pose);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, l.hueY0() + PICKER_HUE_H * PixelGridAnimationOptionWidget.this.uiScale / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractHueStrip(graphics, dtSeconds, x0, x1, l.hueY0(), this.drag == Drag.HUE, mouseX, mouseY, a);
            Materialize.exit(pose);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, l.hexY0() + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractHexField(graphics, x0, l.hexY0(), x1 - x0, PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale, mouseX, mouseY, delta, a);
            Materialize.exit(pose);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, l.rgbY0() + PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractRgbRow(graphics, x0, x1, l.rgbY0(), PICKER_HEX_ROW_H * PixelGridAnimationOptionWidget.this.uiScale, mouseX, mouseY, delta, a);
            Materialize.exit(pose);
            final float swSize = PixelGridAnimationOptionWidget.this.pickerSwatchSize(panelW);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, l.popularLabelY() + (PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale + swSize) / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractSwatchLabel(graphics, "Main", x0, l.popularLabelY(), a);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractPopularRow(graphics, dtSeconds, x0, l.popularSwatchY(), swSize, mouseX, mouseY, a);
            Materialize.exit(pose);
            a = Materialize.enter(pose, Materialize.stagger(reveal, section++, SECTIONS), cx, l.favoritesLabelY() + (PICKER_SWATCH_ROW_LABEL_H * PixelGridAnimationOptionWidget.this.uiScale + PICKER_SWATCH_LABEL_GAP * PixelGridAnimationOptionWidget.this.uiScale + swSize) / 2f, PixelGridAnimationOptionWidget.this.uiScale);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractFavoritesLabel(graphics, x0, l.favoritesLabelY(), mouseX, mouseY, a);
            PixelGridAnimationOptionWidget.this.colorPickerBody.extractFavoritesRow(graphics, dtSeconds, x0, l.favoritesSwatchY(), swSize, mouseX, mouseY, a);
            Materialize.exit(pose);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
            final int button = event.button();
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            final double mx = event.x(), my = event.y();
            final float x0 = panelX + this.pad();
            final float x1 = panelX + panelW - this.pad();
            final PickerLayout l = this.layout(panelY, panelW);
            final float swSize = PixelGridAnimationOptionWidget.this.pickerSwatchSize(panelW);
            if (PixelGridAnimationOptionWidget.this.colorPickerBody.handleFavoritesRowClick(event, mx, my, x0, l.favoritesSwatchY(), swSize)) {
                return true;
            }
            if (button != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            if (PixelGridAnimationOptionWidget.this.colorPickerBody.fieldClicked(event, doubleClick, mx, my)) {
                return true;
            }
            if (my >= l.boxY0() && my < l.boxY1()) {
                this.drag = Drag.BOX;
                PixelGridAnimationOptionWidget.this.colorPickerBody.applyBox(mx, my, x0, x1, l.boxY0(), l.boxY1());
                return true;
            }
            if (my >= l.hueY0() && my < l.hueY0() + PICKER_HUE_H * PixelGridAnimationOptionWidget.this.uiScale) {
                this.drag = Drag.HUE;
                PixelGridAnimationOptionWidget.this.colorPickerBody.applyHue(mx, x0, x1);
                return true;
            }
            if (PixelGridAnimationOptionWidget.this.colorPickerBody.handlePopularRowClick(event, mx, my, x0, l.popularSwatchY(), swSize)) {
                return true;
            }
            return true;
        }

        @Override
        public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY, final int panelX, final int panelY, final int panelW, final int panelH) {
            if (this.drag != Drag.NONE) {
                final PickerLayout l = this.layout(panelY, panelW);
                final float x0 = panelX + this.pad();
                final float x1 = panelX + panelW - this.pad();
                if (this.drag == Drag.BOX) {
                    PixelGridAnimationOptionWidget.this.colorPickerBody.applyBox(event.x(), event.y(), x0, x1, l.boxY0(), l.boxY1());
                } else {
                    PixelGridAnimationOptionWidget.this.colorPickerBody.applyHue(event.x(), x0, x1);
                }
                return true;
            }
            if (PixelGridAnimationOptionWidget.this.colorPickerBody.isFieldFocused()) {
                return PixelGridAnimationOptionWidget.this.colorPickerBody.fieldDragged(event, dragX, dragY);
            }
            return false;
        }

        @Override
        public boolean mouseReleased(final MouseButtonEvent event) {
            this.drag = Drag.NONE;
            return true;
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            return PixelGridAnimationOptionWidget.this.colorPickerBody.isFieldFocused() && PixelGridAnimationOptionWidget.this.colorPickerBody.fieldKeyPressed(event);
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            return PixelGridAnimationOptionWidget.this.colorPickerBody.isFieldFocused() && PixelGridAnimationOptionWidget.this.colorPickerBody.fieldCharTyped(event);
        }
}
}
