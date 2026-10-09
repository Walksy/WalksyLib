package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import net.minecraft.util.Mth;

public class OptionPageWidget extends WLWidget {
    private static final float LABEL_SIZE = Type.SUBHEADLINE.size();
    private static final float PADDING = 8f;
    private static final float AVAIL_SPEED = 10f;
    private static final float BUTTON_RATIO = 0.72f;
    private static final float ICON_RATIO = 0.42f;
    private static final float LABEL_GAP = 6f;
    private final Option<String> option;
    private final RowContext ctx;
    private final float uiScale;
    private final List<String> pageIds;
    private final Runnable onPageChanged;
    private final WLButton prevButton;
    private final WLButton nextButton;
    private float availAnim = 1f;

    private static int LABEL_COLOR() {
        return Theme.get().label();
    }

    private static int LABEL_COLOR_DISABLED() {
        return Theme.get().tertiaryLabel();
    }

    public OptionPageWidget(final Option<String> option, final RowContext ctx, final Runnable onPageChanged, final int x, final int y, final int width, final int height) {
        super(x, y, width, height);
        this.option = option;
        this.ctx = ctx;
        this.uiScale = WLScreen.uiScaleFactor();
        this.pageIds = List.copyOf(option.getPages().keySet());
        this.onPageChanged = onPageChanged;
        this.prevButton = WLButton.icon(0, 0, 1, 1, b -> this.step(-1));
        this.nextButton = WLButton.icon(0, 0, 1, 1, b -> this.step(1));
    }

    private void step(final int delta) {
        if (this.pageIds.size() < 2) {
            return;
        }
        final int current = Math.max(0, this.pageIds.indexOf(this.option.getValue()));
        final int next = Math.floorMod(current + delta, this.pageIds.size());
        this.option.setValue(this.pageIds.get(next));
        if (this.onPageChanged != null) {
            this.onPageChanged.run();
        }
    }

    private boolean isInteractive() {
        return this.active && this.option.isAvailable();
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        final float dt = WLRenderSystem.frameDeltaSeconds();
        final boolean hovered = this.isHovered() && this.active && !coveredByOverlay(mouseX, mouseY);
        final boolean available = this.option.isAvailable();
        WLRenderSystem.cursors().requestControl(this.isHovered() && this.active && !coveredByOverlay(mouseX, mouseY), available);
        this.availAnim = MathUtils.approach(this.availAnim, available ? 1f : 0.4f, AVAIL_SPEED, dt);
        if (hovered && this.ctx != null && this.ctx.onFocusDescription() != null) {
            this.ctx.onFocusDescription().accept(this.option);
        }
        final float alpha = Mth.clamp(this.getAlpha(), 0f, 1f) * this.availAnim;
        if (alpha <= 0.001f) {
            return;
        }
        final float h = this.getHeight();
        final float btnSize = h * BUTTON_RATIO;
        final float y = this.getY() + (h - btnSize) / 2f;
        final float leftX = this.getX() + PADDING * this.uiScale;
        final float rightX = this.getX() + this.getWidth() - PADDING * this.uiScale - btnSize;
        final boolean hoverEnabled = available && !coveredByOverlay(mouseX, mouseY);
        this.extractArrow(graphics, this.prevButton, 180f, leftX, y, btnSize, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        this.extractArrow(graphics, this.nextButton, 0f, rightX, y, btnSize, mouseX, mouseY, dt, alpha, available, hoverEnabled);
        final String current = this.option.getValue();
        final float labelLeft = leftX + btnSize + LABEL_GAP * this.uiScale;
        final float labelRight = rightX - LABEL_GAP * this.uiScale;
        final float room = Math.max(4f, labelRight - labelLeft);
        final float size = Math.min(LABEL_SIZE * this.uiScale, h * 0.55f);
        final String label = Fonts.SFPRO_DISPLAY.truncate(current == null ? "" : current, size, room);
        final float textWidth = Fonts.SFPRO_DISPLAY.width(label, size);
        final float textX = labelLeft + (room - textWidth) / 2f;
        final float baseline = this.getY() + h / 2f + size * 0.59043f * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, label, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, size), size, ColorUtils.withAlpha(available ? LABEL_COLOR() : LABEL_COLOR_DISABLED(), alpha));
    }

    private void extractArrow(final WLGraphics2D graphics, final WLButton button, final float rotationDegrees, final float x, final float y, final float size, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available, final boolean hoverEnabled) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.round(size));
        button.setHeight(Math.round(size));
        button.setCornerRadius(size / 2f);
        button.active = available && this.pageIds.size() > 1;
        button.setAlpha(alpha);
        final int mx = hoverEnabled ? mouseX : TextFieldControl.CURSOR_AWAY;
        final int my = hoverEnabled ? mouseY : TextFieldControl.CURSOR_AWAY;
        graphics.widget(button, mx, my, dt);
        final float iconSize = size * ICON_RATIO;
        final int iconColor = button.active ? Theme.get().label() : LABEL_COLOR_DISABLED();
        button.stretchContent(() -> Icons.CHEVRON.extractRotated(graphics, button.getX() + (button.getWidth() - iconSize) / 2f, button.getY() + (button.getHeight() - iconSize) / 2f, iconSize, ColorUtils.withAlpha(iconColor, alpha), rotationDegrees));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (!this.isInteractive()) {
            return false;
        }
        if (this.prevButton.isMouseOver(event.x(), event.y())) {
            return this.prevButton.mouseClicked(event, doubleClick);
        }
        if (this.nextButton.isMouseOver(event.x(), event.y())) {
            return this.nextButton.mouseClicked(event, doubleClick);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        boolean handled = WLButton.release(this.prevButton, event);
        handled |= WLButton.release(this.nextButton, event);
        return handled;
    }

    private static boolean coveredByOverlay(final double mouseX, final double mouseY) {
        return PopoverHost.anyPanelContains(mouseX, mouseY);
    }
}
