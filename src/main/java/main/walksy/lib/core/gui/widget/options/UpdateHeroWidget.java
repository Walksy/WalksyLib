package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.mods.remote.ModrinthApi;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

public final class UpdateHeroWidget extends WLWidget {
    private static final float PAD = 12f;
    private static final float CARD_CORNER = 12f;
    private static final float WELL_SIZE = 34f;
    private static final float WELL_CORNER = 10f;
    private static final float WELL_ICON = 16f;
    private static final float WELL_GAP = 10f;
    private static final float WELL_FILL_ALPHA = 0.16f;
    private static final float ROW_GAP = 10f;
    private static final float PILL_PAD_X = 6f;
    private static final float PILL_HEIGHT = 1.6f;
    private static final float PILL_FILL_ALPHA = 0.18f;
    private static final float ARROW_SIZE = 7f;
    private static final float ARROW_GAP = 5f;
    private static final float META_GAP = 8f;
    private static final float BUTTON_H = 20f;
    private static final float PRIMARY_W = 104f;
    private static final float SECONDARY_W = 104f;
    private static final float BUTTON_GAP = 6f;
    private static final float STATUS_GAP = 10f;
    private static final float SECTION_GAP = 22f;
    private static final float SECTION_LABEL_GAP = 10f;
    private static final float PRIMARY_TINT = 1.25f;
    private final ModUpdate update;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final GlassStyle cardStyle = ResetButton.knobStyle(ControlLayers.BASE);
    private final WLButton installButton;
    private final WLButton modrinthButton;

    public UpdateHeroWidget(final ModUpdate update, final int x, final int y, final int width) {
        super(x, y, width, 1);
        this.update = update;
        this.installButton = new WLButton(0, 0, 1, 1, "", press -> update.installLatest());
        this.installButton.setTextColor(Theme.get().onAccent());
        this.installButton.style().layer(ControlLayers.KNOB);
        this.modrinthButton = new WLButton(0, 0, 1, 1, "View on Modrinth", press -> WLClient.openUri(update.pageUrl()));
        this.modrinthButton.style().layer(ControlLayers.KNOB);
        this.setHeight(Math.round(this.cardHeight() + this.s(SECTION_GAP) + this.sectionLabelSize() + this.s(SECTION_LABEL_GAP)));
    }

    private float s(final float v) {
        return v * this.uiScale;
    }

    private float eyebrowSize() {
        return Type.CAPTION1.size() * this.uiScale;
    }

    private float titleSize() {
        return Type.TITLE3.size() * this.uiScale;
    }

    private float metaSize() {
        return Type.CAPTION1.size() * this.uiScale;
    }

    private float sectionLabelSize() {
        return Type.FOOTNOTE.size() * this.uiScale;
    }

    private float pillHeight() {
        return this.metaSize() * PILL_HEIGHT;
    }

    private float cardHeight() {
        return this.s(PAD) + this.s(WELL_SIZE) + this.s(ROW_GAP) + this.pillHeight() + this.s(ROW_GAP) + this.s(BUTTON_H) + this.s(PAD);
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
        final float cardH = this.cardHeight();
        graphics.glass()
            .dimensions(x, y, w, cardH)
            .cornerRadius(this.s(CARD_CORNER))
            .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0x0FDA7E01L))
            .extract();
        final float left = x + this.s(PAD);
        final float right = x + w - this.s(PAD);
        final float wellTop = y + this.s(PAD);
        final float pillRow = wellTop + this.s(WELL_SIZE) + this.s(ROW_GAP);
        final float buttonTop = pillRow + this.pillHeight() + this.s(ROW_GAP);
        final float statusX = left + this.s(PRIMARY_W + BUTTON_GAP + SECONDARY_W + STATUS_GAP);
        ResetButton.lift(false, () -> {
            this.extractHeading(graphics, left, right, wellTop, alpha);
            this.extractVersionJump(graphics, left, right, pillRow + this.pillHeight() / 2f, alpha);
            this.extractStatus(graphics, statusX, right, buttonTop + this.s(BUTTON_H) / 2f, alpha);
        });
        this.extractButtons(graphics, left, buttonTop, mouseX, mouseY, delta, alpha);
        final float labelSize = this.sectionLabelSize();
        final float labelCenter = y + cardH + this.s(SECTION_GAP) + labelSize / 2f;
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, "WHAT'S NEW", x, labelCenter, labelSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
    }

    private void extractHeading(final WLGraphics2D graphics, final float left, final float right, final float top, final float alpha) {
        final float well = this.s(WELL_SIZE);
        final int green = ModUpdateBanner.green();
        final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(green, WELL_FILL_ALPHA), alpha);
        graphics.fillRoundedGradient(left, top, left + well, top + well, fill, fill, fill, fill, this.s(WELL_CORNER));
        Icons.UPDATE.extractCentered(graphics, left + well / 2f, top + well / 2f, this.s(WELL_ICON), ColorUtils.withAlpha(green, alpha), 0f);
        final float textX = left + well + this.s(WELL_GAP);
        final float eyebrowSize = this.eyebrowSize();
        final float titleSize = this.titleSize();
        final float eyebrowCenter = top + well * 0.27f;
        final float titleCenter = top + well * 0.68f;
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, this.eyebrow(), textX, eyebrowCenter, eyebrowSize, ColorUtils.withAlpha(this.eyebrowColor(), alpha));
        final String title = Fonts.SFPRO_DISPLAY_BOLD.truncate(this.update.displayName() + " " + this.update.latestVersion(), titleSize, right - textX);
        drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, title, textX, titleCenter, titleSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private void extractVersionJump(final WLGraphics2D graphics, final float left, final float right, final float centerY, final float alpha) {
        final float size = this.metaSize();
        float x = left;
        x += this.pill(graphics, this.update.currentVersion(), x, centerY, size, Theme.get().secondaryLabel(), alpha);
        x += this.s(ARROW_GAP);
        final float arrow = this.s(ARROW_SIZE);
        Icons.CHEVRON.extractCentered(graphics, x + arrow / 2f, centerY, arrow, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha), 0f);
        x += arrow + this.s(ARROW_GAP);
        x += this.pill(graphics, this.update.latestVersion(), x, centerY, size, ModUpdateBanner.green(), alpha);
        final String meta = "for Minecraft " + ModrinthApi.gameVersion();
        final float metaX = x + this.s(META_GAP);
        if (metaX < right) {
            drawText(graphics, Fonts.SFPRO_DISPLAY, Fonts.SFPRO_DISPLAY.truncate(meta, size, right - metaX), metaX, centerY, size, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
        }
    }

    private void extractStatus(final WLGraphics2D graphics, final float left, final float right, final float centerY, final float alpha) {
        final String status = this.statusText();
        if (status.isEmpty() || right - left <= 0f) {
            return;
        }
        final float size = this.metaSize();
        drawText(graphics, Fonts.SFPRO_DISPLAY, Fonts.SFPRO_DISPLAY.truncate(status, size, right - left), left, centerY, size, ColorUtils.withAlpha(this.statusColor(), alpha));
    }

    private void extractButtons(final WLGraphics2D graphics, final float left, final float top, final int mouseX, final int mouseY, final float delta, final float alpha) {
        final float h = this.s(BUTTON_H);
        this.layOut(this.installButton, left, top, this.s(PRIMARY_W), h, alpha);
        this.installButton.setLabel(this.installLabel());
        this.installButton.active = this.canInstall();
        this.installButton.style().tint(ModUpdateBanner.green() & 0x00FFFFFF, PRIMARY_TINT);
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

    private float pill(final WLGraphics2D graphics, final String text, final float x, final float centerY, final float size, final int color, final float alpha) {
        final float w = Fonts.SFPRO_DISPLAY_MEDIUM.width(text, size) + this.s(PILL_PAD_X) * 2f;
        final float h = this.pillHeight();
        final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(color, PILL_FILL_ALPHA), alpha);
        graphics.fillRoundedGradient(x, centerY - h / 2f, x + w, centerY + h / 2f, fill, fill, fill, fill, h / 2f);
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, text, x + this.s(PILL_PAD_X), centerY, size, ColorUtils.withAlpha(color, alpha));
        return w;
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
            default -> ModUpdateBanner.green();
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
            case DOWNLOADING -> "Downloading from Modrinth...";
            case DONE -> "";
            case FAILED -> "That did not work: " + this.update.installError();
            case NONE -> {
                final String blocked = this.update.blockedReason();
                yield blocked != null ? blocked : "";
            }
        };
    }

    private int statusColor() {
        return switch (this.update.installState()) {
            case FAILED -> Theme.get().destructive();
            case DONE -> ModUpdateBanner.green();
            default -> Theme.get().secondaryLabel();
        };
    }

    private static void drawText(final WLGraphics2D graphics, final MsdfFont font, final String text, final float x, final float centerY, final float size, final int color) {
        font.extract(graphics, text, x, font.topForBaseline(centerY + size * 0.59043f * 0.5f, size), size, color);
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
