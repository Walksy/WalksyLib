package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

public final class ModUpdateBanner extends WLWidget {
    private static final float PAD_X = 12f;
    private static final float PAD_Y = 10f;
    private static final float RADIUS = 8f;
    private static final float ICON_SIZE = 15f;
    private static final float ICON_GAP = 9f;
    private static final float LINE_GAP = 3f;
    private static final float BUTTON_W = 104f;
    private static final float BUTTON_H = 24f;
    private static final float BUTTON_GAP = 12f;
    private static final float TITLE_SIZE = Type.SUBHEADLINE.size();
    private static final float STATUS_SIZE = Type.CAPTION1.size();
    private static final float STATUS_ALPHA = 0.85f;
    private static final int GREEN_INDEX = 3;
    private static final float GREEN_DARKEN = 0.22f;
    private final ModUpdate update;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final WLButton button;

    public ModUpdateBanner(final ModUpdate update, final int x, final int y, final int width) {
        super(x, y, width, 1);
        this.update = update;
        this.button = new WLButton(0, 0, 1, 1, "Update", press -> update.installLatest());
        this.button.setTextColor(Theme.get().onAccent());
        this.setHeight(Math.round(preferredHeight(this.uiScale)));
    }

    public static float preferredHeight(final float uiScale) {
        return (PAD_Y * 2f + TITLE_SIZE + LINE_GAP + STATUS_SIZE) * uiScale;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float padX = PAD_X * this.uiScale;
        final float padY = PAD_Y * this.uiScale;
        graphics.glass()
            .dimensions(this.getX(), this.getY(), this.getWidth(), this.getHeight())
            .cornerRadius(RADIUS * this.uiScale)
            .style(cardStyle().opacity(alpha).scaled(this.uiScale))
            .extract();
        final int accent = green();
        final float iconSize = ICON_SIZE * this.uiScale;
        Icons.UPDATE.extract(graphics, this.getX() + padX, this.getY() + (this.getHeight() - iconSize) / 2f, iconSize, ColorUtils.withAlpha(accent, alpha));
        final float textX = this.getX() + padX + iconSize + ICON_GAP * this.uiScale;
        final float titleSize = TITLE_SIZE * this.uiScale;
        final float statusSize = STATUS_SIZE * this.uiScale;
        float y = this.getY() + padY;
        Fonts.SFPRO_DISPLAY_MEDIUM.extract(graphics, this.title(), textX, Fonts.SFPRO_DISPLAY_MEDIUM.topForBaseline(y + titleSize, titleSize), titleSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        y += titleSize + LINE_GAP * this.uiScale;
        final float statusWidth = Math.max(1f, this.buttonX() - BUTTON_GAP * this.uiScale - textX);
        Fonts.SFPRO_DISPLAY.extract(graphics, this.status(statusSize, statusWidth), textX, Fonts.SFPRO_DISPLAY.topForBaseline(y + statusSize, statusSize), statusSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha * STATUS_ALPHA));
        this.layOutButton(accent, alpha);
        graphics.widget(this.button, mouseX, mouseY, delta);
    }

    private void layOutButton(final int accent, final float alpha) {
        final float buttonH = BUTTON_H * this.uiScale;
        this.button.setX(Math.round(this.buttonX()));
        this.button.setY(Math.round(this.getY() + (this.getHeight() - buttonH) / 2f));
        this.button.setWidth(Math.round(BUTTON_W * this.uiScale));
        this.button.setHeight(Math.round(buttonH));
        this.button.setCornerRadius(buttonH / 2f);
        this.button.setAlpha(alpha);
        this.button.setLabel(this.buttonLabel());
        this.button.active = this.update.blockedReason() == null && this.update.installState() == ModUpdate.Install.NONE;
        this.button.style().tint(accent & 0x00FFFFFF, 1.25f);
    }

    private float buttonX() {
        return this.getX() + this.getWidth() - PAD_X * this.uiScale - BUTTON_W * this.uiScale;
    }

    private String buttonLabel() {
        return switch (this.update.installState()) {
            case DOWNLOADING -> "Downloading";
            case DONE -> "Restart";
            case FAILED -> "Try Again";
            case NONE -> "Update";
        };
    }

    private String title() {
        return this.update.displayName() + " " + this.update.latestVersion() + " is available";
    }

    private String status(final float size, final float maxWidth) {
        return Fonts.SFPRO_DISPLAY.truncate(this.statusText(), size, maxWidth);
    }

    private String statusText() {
        return switch (this.update.installState()) {
            case DOWNLOADING -> "Downloading from Modrinth...";
            case DONE -> this.update.installNote();
            case FAILED -> "That did not work: " + this.update.installError();
            case NONE -> {
                final String blocked = this.update.blockedReason();
                if (blocked != null) {
                    yield blocked;
                }
                yield this.update.changelog().isEmpty() ? "You are running " + this.update.currentVersion() + "." : "You are running " + this.update.currentVersion() + ". " + this.update.changelog();
            }
        };
    }

    public static int green() {
        final int[] palette = Theme.get().systemPalette();
        return palette.length > GREEN_INDEX ? ColorUtils.lerpColor(palette[GREEN_INDEX], 0xFF000000, GREEN_DARKEN) : Theme.get().label();
    }

    private static GlassStyle cardStyle() {
        return new GlassStyle()
            .layer(ControlLayers.BASE)
            .tint(Theme.get().cardSurface(), 0.78f)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .refractionReach(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .edgeDarken(0f)
            .brightEdge(LiquidGlassConstants.CHROME_BRIGHT_EDGE)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return this.button.isMouseOver(event.x(), event.y()) && this.button.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return this.button.mouseReleased(event);
    }
}
