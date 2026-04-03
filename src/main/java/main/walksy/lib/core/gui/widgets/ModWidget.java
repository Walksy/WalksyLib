package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.gui.impl.APIScreen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.renderer.Renderer2D;
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

    public ModWidget(Mod mod, APIScreen parent, int x, int y) {
        super(x, y, 140, 34, Component.empty());
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        Renderer2D.fillRoundedRectOutline_ModWidget(context, getX(), getY(), width, height, 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        Renderer2D.fillRoundedRectOutline_ModWidget(context, getX() - 1, getY() - 1, width + 2, height + 2, 2, 1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB());
        //context.verticalLine(getX() + 33, getY(), getY() + getHeight() - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        context.text(Minecraft.getInstance().font, mod.getContainer().getMetadata().getName(), getX() + 38, getY() + (height / 2) - Minecraft.getInstance().font.lineHeight / 2, -1, true);
        context.blit(RenderPipelines.GUI_TEXTURED, mod.getModIcon(), getX() + 1, getY() + 1, 0, 0, 32, 32, 32, 32);

        if (isHovered()) {
            String modDescription = mod.getContainer().getMetadata().getDescription();
            if (!modDescription.isEmpty()) {
                this.setTooltip(Tooltip.create(Component.literal(modDescription)));
            }
        }
    }
    

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (isHovered()) {
            Screen currentParent = this.parent;
            while (currentParent instanceof WalksyLibConfigScreen || currentParent instanceof APIScreen) {
                if (currentParent instanceof WalksyLibConfigScreen walksyParent) {
                    currentParent = walksyParent.parent;
                } else if (currentParent instanceof APIScreen apiParent) {
                    currentParent = apiParent.parent;
                } else {
                    break;
                }
            }
            Minecraft.getInstance().setScreen(new WalksyLibConfigScreen(currentParent, this.mod.getConfig()));
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}
