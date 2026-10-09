package main.walksy.lib.core.gui.widget.options.types;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.Preview;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.ScrollPanel;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.api.render.PreviewCanvas;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public class PreviewOptionWidget extends OptionRowWidget<Preview> implements ScrollPanel.ExtendedPaint {
    private static final float CANVAS_HEIGHT = 150f;
    private static final float BOTTOM_PADDING = 6f;
    private static final float CORNER_RADIUS = 10f;
    private static final float BACKGROUND_ALPHA = 0.05f;
    private static final float START_YAW = 0.6f;
    private static final float START_PITCH = 0.35f;
    private static final float SPIN_SPEED = 0.5f;
    private static final float DRAG_SENSITIVITY = 0.012f;
    private static final float MAX_PITCH = 1.4f;
    private final List<ResetButton> buttons = new ArrayList<>();
    private List<PreviewCanvas.Toggle> toggles = List.of();
    private float yaw = START_YAW;
    private float pitch = START_PITCH;
    private float time;
    private float headerAlpha = 1f;
    private boolean dragging;

    public PreviewOptionWidget(final Option<Preview> option, final RowContext ctx, final int x, final int y, final int width, final int rowHeight) {
        super(option, ctx, x, y, width, rowHeight);
        this.setHeight(Math.round(this.headerHeight() + this.canvasHeight() + BOTTOM_PADDING * this.uiScale));
    }

    private float headerHeight() {
        return OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale;
    }

    private float canvasHeight() {
        return CANVAS_HEIGHT * this.uiScale;
    }

    private float canvasX() {
        return this.getX() + PADDING * this.uiScale;
    }

    private float canvasY() {
        return this.getY() + this.headerHeight();
    }

    private float canvasWidth() {
        return this.getWidth() - PADDING * this.uiScale * 2f;
    }

    private boolean isOverCanvas(final double mouseX, final double mouseY) {
        return mouseX >= this.canvasX() && mouseX < this.canvasX() + this.canvasWidth() && mouseY >= this.canvasY() && mouseY < this.canvasY() + this.canvasHeight();
    }

    @Override
    public float paintBottom() {
        return this.getY() + this.getHeight();
    }

    @Override
    public void setHeaderAlpha(final float alpha) {
        this.headerAlpha = Mth.clamp(alpha, 0f, 1f);
    }

    @Override
    public void setDissolveBand(final float top, final float height) {
    }

    @Override
    protected float labelAlpha(final float alpha) {
        return alpha * this.headerAlpha;
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        this.time += dt;
        if (this.dragging && !WalksyLib.getInstance().getMouseHandler().isLeftDown()) {
            this.dragging = false;
        }
        if (!this.dragging) {
            this.yaw += SPIN_SPEED * dt;
        }
        final float x = this.canvasX();
        final float y = this.canvasY();
        final float w = this.canvasWidth();
        final float h = this.canvasHeight();
        final int background = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().label(), BACKGROUND_ALPHA), alpha);
        graphics.fillRoundedGradient(x, y, x + w, y + h, background, background, background, background, CORNER_RADIUS * this.uiScale);
        final PreviewCanvas canvas = new PreviewCanvas(graphics, x, y, w, h, alpha, this.uiScale, this.time, this.yaw, this.pitch);
        try {
            this.option.getValue().render(canvas);
            canvas.flush();
            this.toggles = canvas.toggles();
        } catch (final RuntimeException broken) {
            WalksyLib.getLogger().err("Preview '" + this.option.getName() + "' failed to draw: " + broken);
        }
        this.extractButtons(graphics, mouseX, mouseY, alpha);
    }

    private void extractButtons(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float alpha) {
        while (this.buttons.size() < this.toggles.size()) {
            final int index = this.buttons.size();
            final int size = Math.round(this.toggles.get(index).size());
            this.buttons.add(new ResetButton(0, 0, size, size, "", () -> this.runToggle(index), ControlLayers.KNOB));
        }
        for (int i = 0; i < this.toggles.size(); i++) {
            final PreviewCanvas.Toggle toggle = this.toggles.get(i);
            final ResetButton button = this.buttons.get(i);
            button.setX(Math.round(toggle.x()));
            button.setY(Math.round(toggle.y()));
            button.icon(toggle.itemId(), !toggle.on());
            button.setAlpha(alpha);
            graphics.widget(button, mouseX, mouseY, 0f);
        }
    }

    private void runToggle(final int index) {
        if (index < this.toggles.size()) {
            this.toggles.get(index).action().run();
        }
    }

    private List<ResetButton> visibleButtons() {
        return this.buttons.subList(0, Math.min(this.buttons.size(), this.toggles.size()));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        for (final ResetButton button : this.visibleButtons()) {
            if (button.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isOverCanvas(event.x(), event.y())) {
            return false;
        }
        this.dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        for (final ResetButton button : this.visibleButtons()) {
            if (button.mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        if (!this.dragging) {
            return false;
        }
        this.yaw += (float) dragX * DRAG_SENSITIVITY;
        this.pitch = Mth.clamp(this.pitch + (float) dragY * DRAG_SENSITIVITY, -MAX_PITCH, MAX_PITCH);
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        for (final ResetButton button : this.visibleButtons()) {
            if (button.mouseReleased(event)) {
                return true;
            }
        }
        final boolean was = this.dragging;
        this.dragging = false;
        return was;
    }
}
