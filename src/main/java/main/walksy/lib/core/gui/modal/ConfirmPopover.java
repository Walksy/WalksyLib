package main.walksy.lib.core.gui.modal;

import main.walksy.lib.core.render.TextQueueMark;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.PopoverStyle;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class ConfirmPopover implements PopoverHost.PopoverContent {
    private static final float PADDING = 12f;
    private static final float GAP = 8f;
    private static final float TITLE_GAP = 4f;
    private static final float ICON_GAP = 6f;
    private static final float ICON_SCALE = 1.2f;
    private static final float CAP_RATIO = 0.59043f;
    private static final float BUTTON_H = 20f;
    private static final float WIDTH = 190f;
    private final String message;
    private final String confirmLabel;
    private final Runnable onConfirm;
    private final Runnable onClosed;
    private final Runnable dismiss;
    private final Supplier<GlassStyle> originStyle;
    private final int panelLayer;
    private final float uiScale = WLScreen.uiScaleFactor();
    private PopoverStyle style;
    private String title = "";
    private Icons.Icon icon;
    private int iconColor;
    private Runnable onCancel;
    private boolean confirmed;
    private WLButton confirmButton;
    private WLButton cancelButton;
    private List<String> lines;
    private List<String> titleLines;

    public ConfirmPopover(final String message, final String confirmLabel, final Runnable onConfirm, final Runnable onClosed, final Runnable dismiss, final Supplier<GlassStyle> originStyle, final int panelLayer) {
        this.message = message == null ? "" : message;
        this.confirmLabel = confirmLabel;
        this.onConfirm = onConfirm;
        this.onClosed = onClosed;
        this.dismiss = dismiss;
        this.originStyle = originStyle;
        this.panelLayer = panelLayer;
    }

    public ConfirmPopover style(final PopoverStyle style) {
        this.style = style;
        return this;
    }

    public ConfirmPopover title(final String title) {
        this.title = title == null ? "" : title.strip();
        this.titleLines = null;
        return this;
    }

    public ConfirmPopover icon(final Icons.Icon icon, final int color) {
        this.icon = icon;
        this.iconColor = color;
        this.titleLines = null;
        return this;
    }

    public ConfirmPopover onCancel(final Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    @Override
    public PopoverStyle style() {
        return this.style == null ? PopoverStyle.getDefault() : this.style;
    }

    @Override
    public int preferredWidth() {
        return Math.round(WIDTH * this.uiScale);
    }

    @Override
    public int preferredHeight() {
        return Math.round(PADDING * this.uiScale * 2f + this.headerHeight() + this.messageLines().size() * Type.SUBHEADLINE.lineHeight() * this.uiScale + GAP * this.uiScale + BUTTON_H * this.uiScale);
    }

    @Override
    @Nullable
    public GlassStyle originStyle() {
        return this.originStyle == null ? null : this.originStyle.get();
    }

    @Override
    public void onClosed() {
        if (this.onClosed != null) {
            this.onClosed.run();
        }
        if (!this.confirmed && this.onCancel != null) {
            this.onCancel.run();
        }
    }

    @Override
    public void extract(final WLGraphics2D graphics, final int panelX, final int panelY, final int panelW, final int panelH, final int mouseX, final int mouseY, final float delta, final float reveal) {
        final float pad = PADDING * this.uiScale;
        final float left = panelX + pad;
        float y = panelY + pad;
        final TextQueueMark textMark = WLRenderSystem.textMark();
        WLRenderSystem.contentStratum().beginPopover(this.panelLayer);
        try {
            if (this.hasHeader()) {
                final MsdfFont titleFont = Type.HEADLINE.regular();
                final float titleSize = Type.HEADLINE.size() * this.uiScale;
                if (this.icon != null) {
                    final float iconSize = this.iconSize();
                    final float iconY = this.hasTitle() ? y + titleSize * (1f - CAP_RATIO / 2f) - iconSize / 2f : y;
                    this.icon.extract(graphics, left, iconY, iconSize, ColorUtils.withAlpha(this.iconColor, reveal));
                }
                final int titleColor = ColorUtils.withAlpha(this.style().title(), reveal);
                float titleY = y;
                for (final String line : this.titleLines()) {
                    titleFont.extract(graphics, line, left + this.titleIndent(), titleFont.topForBaseline(titleY + titleSize, titleSize), titleSize, titleColor);
                    titleY += Type.HEADLINE.lineHeight() * this.uiScale;
                }
                y += this.headerHeight();
            }
            final MsdfFont bodyFont = Type.SUBHEADLINE.regular();
            final float body = Type.SUBHEADLINE.size() * this.uiScale;
            final float lineHeight = Type.SUBHEADLINE.lineHeight() * this.uiScale;
            final int bodyColor = ColorUtils.withAlpha(this.hasTitle() ? this.style().secondaryLabel() : this.style().label(), reveal);
            for (final String line : this.messageLines()) {
                bodyFont.extract(graphics, line, left, bodyFont.topForBaseline(y + body, body), body, bodyColor);
                y += lineHeight;
            }
        } finally {
            WLRenderSystem.contentStratum().endPopover();
            WLRenderSystem.markPopoverText(textMark);
        }
        final float buttonH = BUTTON_H * this.uiScale;
        final float buttonY = panelY + panelH - pad - buttonH;
        final float buttonW = (panelW - pad * 2f - GAP * this.uiScale) / 2f;
        if (this.confirmButton == null) {
            this.cancelButton = new WLButton(0, 0, 1, 1, "Cancel", b -> this.dismiss.run()).flat();
            this.confirmButton = new WLButton(0, 0, 1, 1, this.confirmLabel, b -> {
                this.confirmed = true;
                this.onConfirm.run();
                this.dismiss.run();
            }).flat();
            this.cancelButton.style().layer(this.panelLayer + 1);
            this.confirmButton.style().layer(this.panelLayer + 1);
        }
        this.place(this.cancelButton, left, buttonY, buttonW, buttonH, reveal);
        this.place(this.confirmButton, left + buttonW + GAP * this.uiScale, buttonY, buttonW, buttonH, reveal);
        graphics.widget(this.cancelButton, mouseX, mouseY, delta);
        graphics.widget(this.confirmButton, mouseX, mouseY, delta);
    }

    private void place(final WLButton button, final float x, final float y, final float w, final float h, final float reveal) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.round(w));
        button.setHeight(Math.round(h));
        button.setCornerRadius(h / 2f);
        button.setAlpha(reveal);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick, final int panelX, final int panelY, final int panelW, final int panelH) {
        if (this.cancelButton == null) {
            return false;
        }
        if (this.cancelButton.isMouseOver(event.x(), event.y())) {
            return this.cancelButton.mouseClicked(event, doubleClick);
        }
        if (this.confirmButton.isMouseOver(event.x(), event.y())) {
            return this.confirmButton.mouseClicked(event, doubleClick);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (this.cancelButton == null) {
            return false;
        }
        boolean handled = this.cancelButton.mouseReleased(event);
        handled |= this.confirmButton.mouseReleased(event);
        return handled;
    }

    private boolean hasTitle() {
        return !this.title.isEmpty();
    }

    private boolean hasHeader() {
        return this.hasTitle() || this.icon != null;
    }

    private float iconSize() {
        return Type.HEADLINE.size() * ICON_SCALE * this.uiScale;
    }

    private float titleIndent() {
        return this.icon == null ? 0f : this.iconSize() + ICON_GAP * this.uiScale;
    }

    private float headerHeight() {
        if (!this.hasHeader()) {
            return 0f;
        }
        final float titleBlock = this.titleLines().size() * Type.HEADLINE.lineHeight() * this.uiScale;
        final float block = Math.max(titleBlock, this.icon == null ? 0f : this.iconSize());
        return block + (this.messageLines().isEmpty() ? 0f : TITLE_GAP * this.uiScale);
    }

    private float contentWidth() {
        return this.preferredWidth() - PADDING * this.uiScale * 2f;
    }

    private List<String> titleLines() {
        if (this.titleLines == null) {
            this.titleLines = Type.HEADLINE.regular().wrap(this.title, Type.HEADLINE.size() * this.uiScale, this.contentWidth() - this.titleIndent());
        }
        return this.titleLines;
    }

    private List<String> messageLines() {
        if (this.lines == null) {
            this.lines = Type.SUBHEADLINE.regular().wrap(this.message, Type.SUBHEADLINE.size() * this.uiScale, this.contentWidth());
        }
        return this.lines;
    }

    public static LiquidRect pillAnchor(final float x, final float y, final float w, final float h) {
        return new LiquidRect(x, y, w, h, h / 2f);
    }
}
