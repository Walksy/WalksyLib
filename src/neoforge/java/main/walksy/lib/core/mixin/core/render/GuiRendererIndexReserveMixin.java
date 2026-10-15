package main.walksy.lib.core.mixin.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererIndexReserveMixin {
    private static final int WALKSYLIB_RESERVED_INDICES = 393216;

    @Inject(method = "draw", at = @At("HEAD"))
    private void walksylib$reserveQuadIndices(final CallbackInfo ci) {
        RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(WALKSYLIB_RESERVED_INDICES);
    }
}
