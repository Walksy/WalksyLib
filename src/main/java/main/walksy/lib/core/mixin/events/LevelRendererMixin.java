package main.walksy.lib.core.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.api.render.WLGraphics3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(final PoseStack pose, final LevelRenderState renderState, final SubmitNodeCollector collector, final CallbackInfo ci) {
        final WLGraphics3D graphics = new WLGraphics3D(pose, collector, Minecraft.getInstance().getDeltaTracker());
        WalksyLibEvents.RENDER_3D.invoker().render(graphics);
        graphics.finish();
    }
}
