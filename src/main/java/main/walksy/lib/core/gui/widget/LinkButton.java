package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.utils.ColorUtils;

public class LinkButton extends WLButton {
    private static final float ICON_RATIO = 0.55f;
    private final Icons.Icon icon;

    public LinkButton(final int x, final int y, final int size, final Icons.Icon icon, final String url) {
        super(x, y, size, size, "", button -> WLClient.openUri(url), true);
        this.icon = icon;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float a) {
        super.extract(graphics, mouseX, mouseY, a);
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float size = Math.min(this.getWidth(), this.getHeight()) * ICON_RATIO;
        final float x = this.getX() + (this.getWidth() - size) / 2f;
        final float y = this.getY() + (this.getHeight() - size) / 2f;
        ResetButton.lift(false, () -> this.stretchContent(() -> this.icon.extract(graphics, x, y, size, ColorUtils.withAlpha(Theme.get().label(), alpha))));
    }
}
