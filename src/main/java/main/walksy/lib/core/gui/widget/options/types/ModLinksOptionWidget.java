package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.value.ModLinks;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.TextFieldControl;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.OptionWidgetFactory;
import main.walksy.lib.core.gui.widget.options.RowContext;

public class ModLinksOptionWidget extends OptionRowWidget<ModLinks> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x4D0D1FL);
    private static final float PILL_GAP = 6f;
    private static final float PILL_HEIGHT = 20f;
    private static final float ICON_SIZE = 12f;
    private static final float PILL_INSET = 7f;
    private static final float PILL_ICON_GAP = 5f;
    private static final float PILL_CORNER_RATIO = 0.35f;
    private static final float BOTTOM_PADDING = 4f;
    private static final String EMPTY_TEXT = "No mods are using teams yet";
    private static final float EMPTY_TEXT_SIZE = Type.FOOTNOTE.size();
    private final List<Link> links = new ArrayList<>();

    public ModLinksOptionWidget(Option<ModLinks> option, RowContext ctx, int x, int y, int width, int rowHeight) {
        super(option, ctx, x, y, width, rowHeight);
        for (final Map.Entry<Mod, Option<?>> entry : WalksyLib.getInstance().getGlobalTeams().usingMods().entrySet()) {
            this.links.add(new Link(entry.getKey()));
        }
        this.recomputeHeight();
    }

    private float headerHeight() {
        return OptionWidgetFactory.ROW_HEIGHT_BASE * this.uiScale;
    }

    private float pillHeight() {
        return PILL_HEIGHT * this.uiScale;
    }

    private void recomputeHeight() {
        this.setHeight(Math.round(this.headerHeight() + this.pillHeight() + BOTTOM_PADDING * this.uiScale));
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float y = this.getY() + this.headerHeight();
        final float h = this.pillHeight();
        if (this.links.isEmpty()) {
            final float textSize = EMPTY_TEXT_SIZE * this.uiScale;
            final float baseline = y + h / 2f + textSize * 0.59043f * 0.5f;
            Fonts.SFPRO_DISPLAY.extract(graphics, EMPTY_TEXT, this.getX() + PADDING * this.uiScale, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
            return;
        }
        final boolean hoverEnabled = available && !this.coveredByOverlay(mouseX, mouseY);
        float x = this.getX() + PADDING * this.uiScale;
        final float gap = PILL_GAP * this.uiScale;
        final float right = this.getX() + this.getWidth() - PADDING * this.uiScale;
        for (final Link link : this.links) {
            final float w = link.width(this.uiScale);
            if (x + w > right && x > this.getX() + PADDING * this.uiScale) {
                break;
            }
            link.extract(graphics, x, y, w, h, mouseX, mouseY, dt, alpha, available, hoverEnabled);
            x += w + gap;
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive()) {
            return false;
        }
        for (final Link link : this.links) {
            if (link.button.isMouseOver(event.x(), event.y())) {
                return link.button.mouseClicked(event, doubleClick);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        boolean handled = false;
        for (final Link link : List.copyOf(this.links)) {
            if (link.button.isArmed()) {
                link.button.mouseReleased(event);
                handled = true;
            }
        }
        return handled;
    }

    private final class Link {
        private final Mod mod;
        private final String name;
        private final WLButton button;

        Link(final Mod mod) {
            this.mod = mod;
            this.name = mod.getContainer().getMetadata().getName();
            this.button = new WLButton(0, 0, 1, 1, "", b -> WalksyLibPanelScreen.openModTeams(this.mod));
            this.button.style().layer(ControlLayers.FILL).fadeKey(FADE_KEYS.incrementAndGet());
        }

        float width(final float uiScale) {
            final float textSize = Math.min(OptionRowWidget.LABEL_SIZE * uiScale, ModLinksOptionWidget.this.pillHeight() * 0.55f);
            return PILL_INSET * uiScale * 2f + ICON_SIZE * uiScale + PILL_ICON_GAP * uiScale + Fonts.SFPRO_DISPLAY.width(this.name, textSize);
        }

        void extract(WLGraphics2D graphics, float x, float y, float w, float h, int mouseX, int mouseY, float dt, float alpha, boolean available, boolean hoverEnabled) {
            this.button.setX(Math.round(x));
            this.button.setY(Math.round(y));
            this.button.setWidth(Math.round(w));
            this.button.setHeight(Math.round(h));
            this.button.setCornerRadius(h * PILL_CORNER_RATIO);
            this.button.active = available;
            this.button.setAlpha(alpha);
            final int mx = hoverEnabled ? mouseX : TextFieldControl.CURSOR_AWAY;
            final int my = hoverEnabled ? mouseY : TextFieldControl.CURSOR_AWAY;
            graphics.widget(this.button, mx, my, dt);
            final float s = ModLinksOptionWidget.this.uiScale;
            final float iconSize = ICON_SIZE * s;
            final float iconX = this.button.getX() + PILL_INSET * s;
            final float iconY = this.button.getY() + (h - iconSize) / 2f;
            final float textSize = Math.min(OptionRowWidget.LABEL_SIZE * s, h * 0.55f);
            final float textX = iconX + iconSize + PILL_ICON_GAP * s;
            final float baseline = this.button.getY() + h / 2f + textSize * 0.7f * 0.5f;
            this.button.stretchContent(() -> {
                graphics.blitRounded(this.mod.getModIcon(), DevicePixels.snapX(iconX, graphics.pose()), DevicePixels.snapY(iconY, graphics.pose()), 0f, 0f, iconSize, iconSize, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE, (int) ICON_SIZE, ColorUtils.withAlpha(0xFFFFFFFF, alpha), 3f * s);
                Fonts.SFPRO_DISPLAY.extract(graphics, this.name, textX, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(available ? Theme.get().label() : Theme.get().tertiaryLabel(), alpha));
            });
        }
}
}
