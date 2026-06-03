package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.impl.APIScreen;
import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.gui.impl.ConflictedConfigScreen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class ModWidget extends AbstractWidget {

    private final Mod mod;
    private final APIScreen parent;

    public ModWidget(final Mod mod, final APIScreen parent, final int x, final int y) {
        super(x, y, 140, 34, Component.empty());
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor extractor, final int mouseX, final int mouseY, final float a) {
        final boolean bl = this.mod.hasConfig();
        final boolean bl2 = this.mod.getOverridableConfigScreen(this.parent) != null;
        final int outlineColor;
        if (!bl && !bl2) {
            outlineColor = new Color(220, 60, 60, 180).getRGB();
        } else {
            outlineColor = this.isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB();
        }
        Graphics graphics = new Graphics(extractor);
        graphics.fillRoundedRectOutline_ModWidget(
                this.getX(),
                this.getY(),
                this.width,
                this.height,
                2,
                1,
                outlineColor
        );
        graphics.fillRoundedRectOutline_ModWidget(
                this.getX() - 1,
                this.getY() - 1,
                this.width + 2,
                this.height + 2,
                2,
                1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB()
        );

        extractor.text(
                Minecraft.getInstance().font,
                this.mod.getContainer().getMetadata().getName(),
                this.getX() + 38,
                this.getY() + (this.height / 2) - Minecraft.getInstance().font.lineHeight / 2,
                -1,
                true
        );
        extractor.blit(
                RenderPipelines.GUI_TEXTURED,
                this.mod.getModIcon(),
                this.getX() + 1,
                this.getY() + 1,
                0,
                0,
                32,
                32,
                32,
                32
        );
        if (this.isHovered()) {
            final String modDescription = this.mod.getContainer().getMetadata().getDescription();

            final String tooltipText;
            if (!bl && !bl2) {
                if (modDescription != null && !modDescription.isEmpty()) {
                    tooltipText = modDescription + "\nNo config screen";
                } else {
                    tooltipText = "\nNo config screen";
                }
            } else {
                tooltipText = modDescription;
            }
            if (tooltipText != null && !tooltipText.isEmpty()) {
                this.setTooltip(Tooltip.create(Component.literal(tooltipText)));
            }
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        final BaseScreen overridable = this.mod.getOverridableConfigScreen(this.parent);
        final boolean hasConfig = this.mod.hasConfig();
        if (!this.isHovered() || (!hasConfig && overridable == null)) {
            return super.mouseClicked(click, doubled);
        }

        Screen currentParent = this.parent;
        while (currentParent instanceof WalksyLibConfigScreen || currentParent instanceof APIScreen) {
            currentParent = (currentParent instanceof WalksyLibConfigScreen w) ? w.parent : ((APIScreen) currentParent).parent;
        }

        final Screen nextScreen;
        if (overridable != null && hasConfig) {
            final WalksyLibConfigScreen configScreen = new WalksyLibConfigScreen(currentParent, this.mod.getConfig(), this.mod.getContainer().getMetadata().getName());
            nextScreen = new ConflictedConfigScreen("Choose Screen", currentParent, overridable, configScreen, this.mod.getConflictedButtonTitles());
        } else if (hasConfig) {
            nextScreen = new WalksyLibConfigScreen(currentParent, this.mod.getConfig(), this.mod.getContainer().getMetadata().getName());
        } else {
            nextScreen = overridable;
        }
        Minecraft.getInstance().setScreenAndShow(nextScreen);
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {}

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {}
}
