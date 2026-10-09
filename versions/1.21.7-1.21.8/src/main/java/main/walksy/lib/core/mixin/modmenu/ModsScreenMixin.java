package main.walksy.lib.core.mixin.modmenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(com.terraformersmc.modmenu.gui.ModsScreen.class)
public abstract class ModsScreenMixin extends Screen {
    protected ModsScreenMixin(final Component title) {
        super(title);
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
        if (this.minecraft.level == null) {
            this.renderPanorama(graphics, a);
        }
        this.renderBlurredBackground(graphics);
    }
}
