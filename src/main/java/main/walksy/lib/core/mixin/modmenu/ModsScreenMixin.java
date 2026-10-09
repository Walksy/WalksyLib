package main.walksy.lib.core.mixin.modmenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(com.terraformersmc.modmenu.gui.ModsScreen.class)
public abstract class ModsScreenMixin extends Screen {
    protected ModsScreenMixin(final Component title) {
        super(title);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.isInGameUi()) {
            this.extractTransparentBackground(graphics);
        } else {
            if (this.minecraft.level == null) {
                this.extractPanorama(graphics, a);
            }
            this.extractBlurredBackground(graphics);
            this.minecraft.gui.hud.extractDeferredSubtitles();
        }
    }
}
