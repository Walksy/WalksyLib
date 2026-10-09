package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.WLButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.mods.remote.ModrinthApi;
import main.walksy.lib.core.mods.remote.ModSuggestion;
import main.walksy.lib.core.mods.remote.SuggestedMods;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;

public final class SuggestionPaneWidget extends WLWidget {
    private static final float BODY_SIZE = Type.SUBHEADLINE.size();
    private static final float TOP_INSET = 0f;
    private static final float BLOCK_GAP = 14f;
    private static final float BUTTON_H = 24f;
    private static final float BUTTON_W = 150f;
    private final ModSuggestion suggestion;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final List<String> summaryLines;
    private final WLButton downloadButton;

    public SuggestionPaneWidget(final ModSuggestion suggestion, final int x, final int y, final int width) {
        super(x, y, width, 1);
        this.suggestion = suggestion;
        this.summaryLines = Fonts.SFPRO_DISPLAY.wrap(suggestion.summary(), BODY_SIZE * this.uiScale, width);
        this.downloadButton = suggestion.isAvailable() ? new WLButton(0, 0, 1, 1, "Download " + suggestion.versionNumber(), b -> WalksyLib.getInstance().getSuggestedMods().install(suggestion)) : null;
        if (this.downloadButton != null) {
            this.downloadButton.style().layer(ControlLayers.KNOB);
        }
        this.setHeight(Math.round(TOP_INSET * this.uiScale + this.summaryLines.size() * this.lineHeight() + BLOCK_GAP * this.uiScale + this.lineHeight() + (this.downloadButton != null ? BLOCK_GAP * this.uiScale + BUTTON_H * this.uiScale : 0f) + BLOCK_GAP * this.uiScale));
    }

    private float lineHeight() {
        return Type.SUBHEADLINE.lineHeight() * this.uiScale;
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        final float body = BODY_SIZE * this.uiScale;
        float y = this.getY() + TOP_INSET * this.uiScale;
        for (final String line : this.summaryLines) {
            Fonts.SFPRO_DISPLAY.extract(graphics, line, this.getX(), Fonts.SFPRO_DISPLAY.topForBaseline(y + body, body), body, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
            y += this.lineHeight();
        }
        y += BLOCK_GAP * this.uiScale;
        final String status = this.statusLine();
        Fonts.SFPRO_DISPLAY.extract(graphics, status, this.getX(), Fonts.SFPRO_DISPLAY.topForBaseline(y + body, body), body, ColorUtils.withAlpha(Theme.get().label(), alpha));
        y += this.lineHeight();
        if (this.downloadButton == null) {
            return;
        }
        y += BLOCK_GAP * this.uiScale;
        final float buttonH = BUTTON_H * this.uiScale;
        this.downloadButton.setX(this.getX());
        this.downloadButton.setY(Math.round(y));
        this.downloadButton.setWidth(Math.round(BUTTON_W * this.uiScale));
        this.downloadButton.setHeight(Math.round(buttonH));
        this.downloadButton.setCornerRadius(buttonH / 2f);
        this.downloadButton.setAlpha(alpha);
        this.downloadButton.active = WalksyLib.getInstance().getSuggestedMods().installState(this.suggestion) == SuggestedMods.Install.NONE || WalksyLib.getInstance().getSuggestedMods().installState(this.suggestion) == SuggestedMods.Install.FAILED;
        graphics.widget(this.downloadButton, mouseX, mouseY, delta);
    }

    private String statusLine() {
        if (!this.suggestion.isAvailable()) {
            return this.suggestion.reason();
        }
        return switch (WalksyLib.getInstance().getSuggestedMods().installState(this.suggestion)) {
            case DOWNLOADING -> "Downloading...";
            case INSTALLED -> "Installed. Restart Minecraft to load it.";
            case FAILED -> "Download failed: " + WalksyLib.getInstance().getSuggestedMods().installError(this.suggestion);
            case NONE -> "Version " + this.suggestion.versionNumber() + " is available for Minecraft " + ModrinthApi.gameVersion() + ".";
        };
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.downloadButton != null && this.downloadButton.isMouseOver(event.x(), event.y())) {
            return this.downloadButton.mouseClicked(event, doubleClick);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return this.downloadButton != null && this.downloadButton.mouseReleased(event);
    }
}
