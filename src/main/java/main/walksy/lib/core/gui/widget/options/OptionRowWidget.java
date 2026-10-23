package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.texture.Icons;
import net.minecraft.client.input.MouseButtonEvent;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.List;

public abstract class OptionRowWidget<T> extends WLWidget implements ScrollPanel.Revalidating, ScrollPanel.ClickInterceptor {
    public static final float LABEL_SIZE = Type.SUBHEADLINE.size();
    protected static final float PADDING = 8f;
    protected static final float DESCRIPTION_SIZE = Type.FOOTNOTE.size();
    protected static final float DESCRIPTION_TOP_GAP = 1f;
    protected static final float DESCRIPTION_LINE_SPACING = 1.28f;
    protected static final float DESCRIPTION_BOTTOM_GAP = 4f;
    private static final float DESCRIPTION_RENDER_HEIGHT = 40f;
    private static final float AVAIL_SPEED = 10f;
    private static final float AVAIL_DIM = 0.4f;
    private static final float RESET_GAP = 5f;
    private static final float RESET_SIZE_RATIO = 1.1f;
    private static final float RESET_SPEED = 14f;
    private static final float RESET_SLOP = 3f;
    protected final Option<T> option;
    protected final RowContext ctx;
    protected final float uiScale;
    private int baseHeight;
    private float availAnim = 1f;
    private float availFade = 1f;
    private List<String> descriptionCache;
    private float descriptionCacheWidth = -1f;
    private String descriptionCacheText;
    private float resetAnim;
    private float resetHover;
    private float resetX0, resetY0, resetX1, resetY1;
    private @Nullable BooleanSupplier resetChanged;
    private @Nullable Runnable resetAction;
    private @Nullable String resetLabel;
    private float resetRightInset;

    private @Nullable Integer labelColorOverride;

    public void setResetButton(final String label, final float rightInset, final BooleanSupplier changed, final Runnable action) {
        this.resetLabel = label;
        this.resetRightInset = rightInset;
        this.resetChanged = changed;
        this.resetAction = action;
    }

    public void setLabelColor(final @Nullable Integer color) {
        this.labelColorOverride = color;
    }

    public static int LABEL_COLOR() {
        return Theme.get().label();
    }

    protected static int LABEL_COLOR_DISABLED() {
        return Theme.get().tertiaryLabel();
    }

    protected OptionRowWidget(final Option<T> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.option = option;
        this.ctx = ctx;
        this.uiScale = WLScreen.uiScaleFactor();
        this.setHeight(height);
    }

    @Override
    public void setHeight(final int height) {
        if (this.option == null) {
            super.setHeight(height);
            return;
        }
        this.baseHeight = height;
        super.setHeight(height + Math.round(this.descriptionHeight() + this.extraBandHeight()));
    }

    @Override
    public void revalidateSize() {
        this.setHeight(this.baseHeight);
    }

    protected float extraBandHeight() {
        return 0f;
    }

    protected final float bandAlpha() {
        return Mth.clamp(this.getAlpha(), 0f, 1f);
    }

    protected final float extraBandTop() {
        return this.getY() + this.rowLabelHeight();
    }

    protected final float descriptionHeight() {
        final OptionDescription description = this.option == null ? null : this.option.getDescription();
        if (description == null) {
            return 0f;
        }
        if (description.getType() == OptionDescription.Type.RENDER) {
            return description.getRenderer() == null ? 0f : DESCRIPTION_TOP_GAP * this.uiScale + DESCRIPTION_RENDER_HEIGHT * this.uiScale + DESCRIPTION_BOTTOM_GAP * this.uiScale;
        }
        final List<String> lines = this.descriptionLines();
        if (lines.isEmpty()) {
            return 0f;
        }
        return DESCRIPTION_TOP_GAP * this.uiScale + lines.size() * DESCRIPTION_SIZE * this.uiScale * DESCRIPTION_LINE_SPACING + DESCRIPTION_BOTTOM_GAP * this.uiScale;
    }

    private List<String> descriptionLines() {
        final OptionDescription description = this.option == null ? null : this.option.getDescription();
        if (description == null || description.getType() != OptionDescription.Type.TEXT || description.getText() == null) {
            return List.of();
        }
        final String text = description.getText().get();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        final float width = this.descriptionWidth();
        if (this.descriptionCache != null && this.descriptionCacheWidth == width && text.equals(this.descriptionCacheText)) {
            return this.descriptionCache;
        }
        this.descriptionCacheText = text;
        this.descriptionCacheWidth = width;
        this.descriptionCache = this.wrapDescription(text, width);
        return this.descriptionCache;
    }

    private float descriptionWidth() {
        return Math.max(1f, this.controlAreaX() - (this.getX() + PADDING * this.uiScale));
    }

    private List<String> wrapDescription(final String text, final float maxWidth) {
        return this.wrapAt(text, maxWidth, DESCRIPTION_SIZE * this.uiScale);
    }

    protected final List<String> wrapAt(final String text, final float maxWidth, final float size) {
        final List<String> lines = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (final String word : paragraph.split(" ")) {
                final String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && Fonts.SFPRO_DISPLAY.width(candidate, size) > maxWidth) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return List.copyOf(lines);
    }

    private void extractDescription(final WLGraphics2D graphics) {
        final OptionDescription description = this.option.getDescription();
        if (description == null) {
            return;
        }
        final float height = this.descriptionHeight();
        if (height <= 0f) {
            return;
        }
        final float top = this.getY() + this.getHeight() - height + DESCRIPTION_TOP_GAP * this.uiScale;
        final float x = this.getX() + PADDING * this.uiScale;
        if (description.getType() == OptionDescription.Type.RENDER) {
            final int bandH = Math.round(DESCRIPTION_RENDER_HEIGHT * this.uiScale);
            graphics.enableScissor(Math.round(x), Math.round(top), Math.round(x + this.descriptionWidth()), Math.round(top) + bandH);
            try {
                description.getRenderer().accept(graphics, new OptionDescription.Panel(Math.round(x), Math.round(top), Math.round(this.descriptionWidth()), bandH));
            } catch (final RuntimeException broken) {
            } finally {
                graphics.disableScissor();
            }
            return;
        }
        final float size = DESCRIPTION_SIZE * this.uiScale;
        final float lineHeight = size * DESCRIPTION_LINE_SPACING;
        final int color = ColorUtils.withAlpha(Theme.get().secondaryLabel(), this.bandAlpha() * (this.dimsDescription() ? this.availAnim : 1f));
        float y = top;
        for (final String line : this.descriptionLines()) {
            Fonts.SFPRO_DISPLAY.extract(graphics, line, x, Fonts.SFPRO_DISPLAY.topForBaseline(y + size * 0.8f, size), size, color);
            y += lineHeight;
        }
    }

    protected final boolean openPopover(final LiquidRect anchor, final PopoverHost.PopoverContent content) {
        if (this.ctx == null || this.ctx.onOpenPopover() == null) {
            return false;
        }
        this.ctx.onOpenPopover().accept(anchor, content);
        return true;
    }

    protected final void closePopover() {
        if (this.ctx != null && this.ctx.onClosePopover() != null) {
            this.ctx.onClosePopover().run();
        }
    }

    @Override
    protected final void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active && !coveredByOverlay(mouseX, mouseY);
        final boolean available = this.option.isAvailable();
        this.availFade = MathUtils.approach(this.availFade, available ? 1f : 0f, AVAIL_SPEED, dt);
        this.availAnim = AVAIL_DIM + (1f - AVAIL_DIM) * this.availFade;
        if (hovered && this.ctx != null && this.ctx.onFocusDescription() != null) {
            this.ctx.onFocusDescription().accept(this.option);
        }
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f) * this.availAnim;
        if (alpha <= 0.001f) {
            return;
        }
        final float labelSize = LABEL_SIZE * this.uiScale;
        final float capHeight = labelSize * 0.59043f;
        final float baseline = this.getY() + this.rowLabelHeight() / 2f + capHeight * 0.5f;
        final float textTop = Fonts.SFPRO_DISPLAY.topForBaseline(baseline, labelSize);
        final int labelColor = available ? (this.labelColorOverride == null ? LABEL_COLOR() : this.labelColorOverride) : LABEL_COLOR_DISABLED();
        Fonts.SFPRO_DISPLAY.extract(graphics, this.option.getName(), this.getX() + PADDING * this.uiScale, textTop, labelSize, ColorUtils.withAlpha(labelColor, this.labelAlpha(alpha)));
        final float labelRight = this.getX() + PADDING * this.uiScale + Fonts.SFPRO_DISPLAY.width(this.option.getName(), labelSize);
        this.extractResetMark(graphics, labelRight, baseline - capHeight * 0.5f, labelSize, mouseX, mouseY, dt, alpha, available);
        this.extractControl(graphics, mouseX, mouseY, dt, alpha, available);
        this.extractDescription(graphics);
    }

    private boolean resettable(final boolean available) {
        if (this.resetAction != null) {
            return this.resetChanged.getAsBoolean();
        }
        return available && this.option.canReset() && this.option.hasChanged();
    }

    private void extractResetChip(final WLGraphics2D graphics, final float centerY, final int mouseX, final int mouseY, final float dt, final float alpha) {
        final boolean enabled = this.resetChanged.getAsBoolean();
        this.resetAnim = 1f;
        final float size = Type.CAPTION1.size() * this.uiScale;
        final float w = Fonts.SFPRO_DISPLAY_MEDIUM.width(this.resetLabel, size) + 11f * this.uiScale;
        final float h = size * 1.7f;
        final float x1 = this.getX() + this.getWidth() - this.resetRightInset * this.uiScale;
        final float x0 = x1 - w;
        final float y0 = centerY - h / 2f;
        this.resetX0 = x0;
        this.resetY0 = y0;
        this.resetX1 = enabled ? x1 : x0;
        this.resetY1 = y0 + h;
        final boolean hovered = enabled && !PopoverHost.anyOpen() && mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y0 + h;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        this.resetHover = MathUtils.approach(this.resetHover, hovered ? 1f : 0f, RESET_SPEED, dt);
        final int ink = Theme.get().isDark() ? 0xFFFFFF : 0x000000;
        final float fillAlpha = enabled ? 0.10f + 0.12f * this.resetHover : 0.05f;
        final int fill = ColorUtils.withAlpha((Math.round(fillAlpha * 255f) << 24) | ink, alpha);
        graphics.fillRoundedGradient(x0, y0, x1, y0 + h, fill, fill, fill, fill, h * 0.34f);
        final int text = ColorUtils.withAlpha(enabled ? Theme.get().label() : Theme.get().tertiaryLabel(), alpha);
        final float top = Fonts.SFPRO_DISPLAY_MEDIUM.topForBaseline(centerY + size * 0.59043f * 0.5f, size);
        Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, this.resetLabel, x0 + (w - Fonts.SFPRO_DISPLAY_MEDIUM.width(this.resetLabel, size)) / 2f, top, size, text);
    }

    private void extractResetMark(final WLGraphics2D graphics, final float labelRight, final float centerY, final float labelSize, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        if (this.resetLabel != null) {
            this.extractResetChip(graphics, centerY, mouseX, mouseY, dt, alpha);
            return;
        }
        final boolean show = this.resettable(available);
        this.resetAnim = MathUtils.approach(this.resetAnim, show ? 1f : 0f, RESET_SPEED, dt);
        if (this.resetAnim <= 0.001f) {
            this.resetX1 = this.resetX0;
            return;
        }
        final float size = labelSize * RESET_SIZE_RATIO;
        final float x = labelRight + RESET_GAP * this.uiScale;
        final float y = centerY - size / 2f;
        final float slop = RESET_SLOP * this.uiScale;
        this.resetX0 = x - slop;
        this.resetY0 = y - slop;
        this.resetX1 = x + size + slop;
        this.resetY1 = y + size + slop;
        final boolean hovered = show && !PopoverHost.anyOpen() && mouseX >= this.resetX0 && mouseX < this.resetX1 && mouseY >= this.resetY0 && mouseY < this.resetY1;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        this.resetHover = MathUtils.approach(this.resetHover, hovered ? 1f : 0f, RESET_SPEED, dt);
        final int color = ColorUtils.lerpColor(Theme.get().tertiaryLabel(), Theme.get().label(), this.resetHover);
        Icons.RESET.extract(graphics, x, y, size, ColorUtils.withAlpha(color, alpha * this.resetAnim));
    }

    @Override
    public boolean interceptClick(final MouseButtonEvent event) {
        if (this.resetAnim < 0.5f || PopoverHost.anyOpen() || !this.resettable(this.option.isAvailable())) {
            return false;
        }
        if (event.x() < this.resetX0 || event.x() >= this.resetX1 || event.y() < this.resetY0 || event.y() >= this.resetY1) {
            return false;
        }
        if (this.resetAction != null) {
            this.resetAction.run();
            return true;
        }
        this.option.reset();
        this.option.runChange();
        return true;
    }

    protected boolean dimsDescription() {
        return true;
    }

    protected final float availability() {
        return this.availFade;
    }

    protected static boolean coveredByOverlay(final double mouseX, final double mouseY) {
        return PopoverHost.anyPanelContains(mouseX, mouseY);
    }

    protected abstract void extractControl(WLGraphics2D graphics, int mouseX, int mouseY, float dt, float alpha, boolean available);

    protected float rowLabelHeight() {
        return Math.min(this.getHeight(), OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale);
    }

    protected final boolean isInteractive() {
        return this.active && this.option.isAvailable();
    }

    protected final boolean isOverRow(final double mouseX, final double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY < this.getY() + this.rowLabelHeight();
    }

    protected float controlAreaX() {
        return this.getX() + this.getWidth() * 0.55f;
    }

    protected float controlAreaWidth() {
        return this.getX() + this.getWidth() - PADDING * this.uiScale - this.controlAreaX();
    }

    protected float labelAlpha(final float alpha) {
        return alpha;
    }
}
