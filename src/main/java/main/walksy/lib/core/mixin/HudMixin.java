package main.walksy.lib.core.mixin;

import main.walksy.lib.core.utils.IHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Hud.class)
public abstract class HudMixin implements IHud {

    @Shadow
    protected abstract void extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    @Unique
    @Override
    public void extractCrosshair$walksyLib(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        this.extractCrosshair(graphics, deltaTracker);
    }
}
