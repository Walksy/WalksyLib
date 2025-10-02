package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.impl.APIScreen;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;

import java.awt.*;

public class ModWidget extends AbstractWidget {

    private final Mod mod;
    private final APIScreen parent;

    public ModWidget(Mod mod, APIScreen parent, int x, int y)
    {
        super(x, y, 140, 34, Text.empty());
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        WalksyLib.get2DRenderer().fillRoundedRectOutline_ModWidget(context, getX(), getY(), width, height, 2, 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        WalksyLib.get2DRenderer().fillRoundedRectOutline_ModWidget(context, getX() - 1, getY() - 1, width + 2, height + 2, 2, 1,
                this.active ? new Color(0, 0, 0, 191).getRGB() : new Color(30, 30, 30, 120).getRGB());
        //context.drawVerticalLine(getX() + 33, getY(), getY() + getHeight() - 1, isHovered() ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, mod.getContainer().getMetadata().getName(), getX() + 38, getY() + (height / 2) - MinecraftClient.getInstance().textRenderer.fontHeight / 2, -1);
        context.drawTexture(RenderLayer::getGuiTextured, mod.getModIcon(), getX() + 1, getY() + 1, 0, 0, 32, 32, 32, 32);

        if (isHovered())
        {
            String modDescription = mod.getContainer().getMetadata().getDescription();
            if (!modDescription.isEmpty()) {
                this.setTooltip(Tooltip.of(Text.of(modDescription)));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered())
        {
            MinecraftClient.getInstance().setScreen(this.mod.getConfigScreen(this.parent));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}
