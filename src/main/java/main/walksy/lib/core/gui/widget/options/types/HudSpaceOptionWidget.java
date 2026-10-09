package main.walksy.lib.core.gui.widget.options.types;
import com.mojang.blaze3d.platform.InputConstants;

import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.platform.WLClient;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.hud.HudEditScreen;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.concurrent.atomic.AtomicLong;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.OptionRowWidget;
import main.walksy.lib.core.gui.widget.options.RowContext;

public final class HudSpaceOptionWidget extends OptionRowWidget<Runnable> {
    private static final AtomicLong FADE_KEYS = new AtomicLong(0x8D5ACEL);
    private static final String LABEL = "Edit HUD";
    private static final float PILL_H_PADDING = 8f;
    private static final float PILL_HEIGHT_RATIO = 0.8f;
    private static final float HOVER_SPEED = 14f;
    private final GlassStyle pillStyle;
    private float pillX0;
    private float pillY0;
    private float pillW;
    private float pillH;
    private float hoverAnim;

    public HudSpaceOptionWidget(final Option<Runnable> option, final RowContext ctx, final int x, final int y, final int width, final int height) {
        super(option, ctx, x, y, width, height);
        this.pillStyle = ResetButton.knobStyle(ControlLayers.BASE).fadeKey(FADE_KEYS.incrementAndGet());
    }

    @Override
    protected void extractControl(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha, final boolean available) {
        final float textSize = Type.FOOTNOTE.size() * this.uiScale;
        this.pillH = this.rowLabelHeight() * PILL_HEIGHT_RATIO;
        this.pillW = Fonts.SFPRO_DISPLAY.width(LABEL, textSize) + PILL_H_PADDING * 2f * this.uiScale;
        this.pillX0 = this.getX() + this.getWidth() - PADDING * this.uiScale - this.pillW;
        this.pillY0 = this.getY() + (this.rowLabelHeight() - this.pillH) / 2f;
        final boolean hovered = available && this.isOverPill(mouseX, mouseY) && !this.coveredByOverlay(mouseX, mouseY);
        WLRenderSystem.cursors().requestControl(this.isOverPill(mouseX, mouseY) && !this.coveredByOverlay(mouseX, mouseY), available);
        this.hoverAnim = MathUtils.approach(this.hoverAnim, hovered ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(this.pillX0, this.pillY0, this.pillW, this.pillH)
            .cornerRadius(this.pillH / 2f)
            .style(this.pillStyle.scaled(this.uiScale).opacity(alpha))
            .hover(this.hoverAnim)
            .extract();
        final float capHeight = textSize * 0.59043f;
        final float baseline = this.pillY0 + this.pillH / 2f + capHeight * 0.5f;
        Fonts.SFPRO_DISPLAY.extract(graphics, LABEL, this.pillX0 + (this.pillW - Fonts.SFPRO_DISPLAY.width(LABEL, textSize)) / 2f, Fonts.SFPRO_DISPLAY.topForBaseline(baseline, textSize), textSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
    }

    private boolean isOverPill(final double mouseX, final double mouseY) {
        return this.pillW > 0f && mouseX >= this.pillX0 && mouseX < this.pillX0 + this.pillW && mouseY >= this.pillY0 && mouseY < this.pillY0 + this.pillH;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isInteractive() || !this.isOverPill(event.x(), event.y())) {
            return false;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        WLClient.setScreen(new HudEditScreen(WLClient.screen(), this.option.getHudSpace()));
        return true;
    }
}
