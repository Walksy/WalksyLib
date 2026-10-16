package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

public final class ModUpdateBanner extends WLWidget {
    private static final float PAD = 12f;
    private static final float CARD_CORNER = 12f;
    private static final float WELL_SIZE = 34f;
    private static final float WELL_CORNER = 10f;
    private static final float WELL_ICON = 16f;
    private static final float WELL_GAP = 10f;
    private static final float WELL_FILL_ALPHA = 0.16f;
    private static final float META_GAP = 8f;
    private static final float BUTTON_H = 20f;
    private static final float PRIMARY_W = 104f;
    private static final float SECONDARY_W = 104f;
    private static final float BUTTON_GAP = 6f;
    private static final float TEXT_GAP = 12f;
    private static final float PRIMARY_TINT = 1.25f;
    private static final int GREEN_INDEX = 3;
    private static final float GREEN_DARKEN = 0.22f;
    private final ModUpdate update;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final GlassStyle cardStyle = ResetButton.knobStyle(ControlLayers.BASE);
    private final WLButton installButton;
    private final WLButton modrinthButton;

    public ModUpdateBanner(final ModUpdate update, final int x, final int y, final int width) {
        super(x, y, width, 1);
        this.update = update;
        this.installButton = new WLButton(0, 0, 1, 1, "", press -> update.installLatest());
        this.installButton.setTextColor(Theme.get().onAccent());
        this.installButton.style().layer(ControlLayers.KNOB);
        this.modrinthButton = new WLButton(0, 0, 1, 1, "View on Modrinth", press -> WLClient.openUri(update.pageUrl()));
        this.modrinthButton.style().layer(ControlLayers.KNOB);
        this.setHeight(Math.round(preferredHeight(this.uiScale)));
    }

    public static float preferredHeight(final float uiScale) {
        return (PAD * 2f + WELL_SIZE) * uiScale;
    }

    private float s(final float v) {
        return v * this.uiScale;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float x = this.getX();
        final float y = this.getY();
        final float w = this.getWidth();
        final float h = this.getHeight();
        graphics.glass()
            .dimensions(x, y, w, h)
            .cornerRadius(this.s(CARD_CORNER))
            .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0x0FDA7E02L))
            .extract();
        final float left = x + this.s(PAD);
        final float buttonsLeft = x + w - this.s(PAD) - this.s(PRIMARY_W + BUTTON_GAP + SECONDARY_W);
        final float centerY = y + h / 2f;
        ResetButton.lift(false, () -> this.extractHeading(graphics, left, buttonsLeft - this.s(TEXT_GAP), centerY - this.s(WELL_SIZE) / 2f, alpha));
        this.extractButtons(graphics, buttonsLeft, centerY - this.s(BUTTON_H) / 2f, mouseX, mouseY, delta, alpha);
    }

    private void extractHeading(final WLGraphics2D graphics, final float left, final float right, final float top, final float alpha) {
        final float well = this.s(WELL_SIZE);
        final int green = green();
        final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(green, WELL_FILL_ALPHA), alpha);
        graphics.fillRoundedGradient(left, top, left + well, top + well, fill, fill, fill, fill, this.s(WELL_CORNER));
        Icons.UPDATE.extractCentered(graphics, left + well / 2f, top + well / 2f, this.s(WELL_ICON), ColorUtils.withAlpha(green, alpha), 0f);
        final float textX = left + well + this.s(WELL_GAP);
        if (right <= textX) {
            return;
        }
        final float eyebrowSize = Type.CAPTION1.size() * this.uiScale;
        final float titleSize = Type.TITLE3.size() * this.uiScale;
        final float eyebrowCenter = top + well * 0.27f;
        final float titleCenter = top + well * 0.68f;
        final String eyebrow = Fonts.SFPRO_DISPLAY_MEDIUM.truncate(this.eyebrow(), eyebrowSize, right - textX);
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, eyebrow, textX, eyebrowCenter, eyebrowSize, ColorUtils.withAlpha(this.eyebrowColor(), alpha));
        final String status = this.statusText();
        final float statusX = textX + Fonts.SFPRO_DISPLAY_MEDIUM.width(eyebrow, eyebrowSize) + this.s(META_GAP);
        if (!status.isEmpty() && statusX < right) {
            drawText(graphics, Fonts.SFPRO_DISPLAY, Fonts.SFPRO_DISPLAY.truncate(status, eyebrowSize, right - statusX), statusX, eyebrowCenter, eyebrowSize, ColorUtils.withAlpha(this.statusColor(), alpha));
        }
        final String title = Fonts.SFPRO_DISPLAY_BOLD.truncate(this.update.displayName() + " " + this.update.latestVersion(), titleSize, right - textX);
        drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, title, textX, titleCenter, titleSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private void extractButtons(final WLGraphics2D graphics, final float left, final float top, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float h = this.s(BUTTON_H);
        this.layOut(this.installButton, left, top, this.s(PRIMARY_W), h, alpha);
        this.installButton.setLabel(this.installLabel());
        this.installButton.active = this.canInstall();
        this.installButton.style().tint(green() & 0x00FFFFFF, PRIMARY_TINT);
        this.layOut(this.modrinthButton, left + this.s(PRIMARY_W) + this.s(BUTTON_GAP), top, this.s(SECONDARY_W), h, alpha);
        graphics.widget(this.installButton, mouseX, mouseY, delta);
        graphics.widget(this.modrinthButton, mouseX, mouseY, delta);
    }

    private void layOut(final WLButton button, final float x, final float y, final float w, final float h, final float alpha) {
        button.setX(Math.round(x));
        button.setY(Math.round(y));
        button.setWidth(Math.round(w));
        button.setHeight(Math.round(h));
        button.setCornerRadius(h / 2f);
        button.setAlpha(alpha);
    }

    private boolean canInstall() {
        return this.update.blockedReason() == null && this.update.installState() != ModUpdate.Install.DOWNLOADING && this.update.installState() != ModUpdate.Install.DONE;
    }

    private String eyebrow() {
        return switch (this.update.installState()) {
            case DOWNLOADING -> "DOWNLOADING";
            case DONE -> "READY AFTER RESTART";
            case FAILED -> "INSTALL FAILED";
            case NONE -> "UPDATE AVAILABLE";
        };
    }

    private int eyebrowColor() {
        return switch (this.update.installState()) {
            case FAILED -> Theme.get().destructive();
            case DOWNLOADING -> Theme.get().accent();
            default -> green();
        };
    }

    private String installLabel() {
        return switch (this.update.installState()) {
            case DOWNLOADING -> "Downloading...";
            case DONE -> "Installed";
            case FAILED -> "Try Again";
            case NONE -> "Install " + this.update.latestVersion();
        };
    }

    private String statusText() {
        return switch (this.update.installState()) {
            case DOWNLOADING, DONE -> "";
            case FAILED -> this.update.installError();
            case NONE -> {
                final String blocked = this.update.blockedReason();
                yield blocked != null ? blocked : "You have " + this.update.currentVersion();
            }
        };
    }

    private int statusColor() {
        return switch (this.update.installState()) {
            case FAILED -> Theme.get().destructive();
            default -> Theme.get().tertiaryLabel();
        };
    }

    private static void drawText(final WLGraphics2D graphics, final MsdfFont font, final String text, final float x, final float centerY, final float size, final int color) {
        font.extract(graphics, text, x, font.topForBaseline(centerY + size * 0.59043f * 0.5f, size), size, color);
    }

    public static int green() {
        final int[] palette = Theme.get().systemPalette();
        return palette.length > GREEN_INDEX ? ColorUtils.lerpColor(palette[GREEN_INDEX], 0xFF000000, GREEN_DARKEN) : Theme.get().label();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.installButton.isMouseOver(event.x(), event.y())) {
            return this.installButton.mouseClicked(event, doubleClick);
        }
        if (this.modrinthButton.isMouseOver(event.x(), event.y())) {
            return this.modrinthButton.mouseClicked(event, doubleClick);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        final boolean install = this.installButton.mouseReleased(event);
        final boolean modrinth = this.modrinthButton.mouseReleased(event);
        return install || modrinth;
    }
}
