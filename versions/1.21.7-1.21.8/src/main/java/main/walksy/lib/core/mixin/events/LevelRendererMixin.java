package main.walksy.lib.core.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.api.render.WLGraphics3D;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntities(final PoseStack pose, final MultiBufferSource.BufferSource source, final Camera camera, final DeltaTracker deltaTracker, final List<Entity> entities, final CallbackInfo ci) {
        final WLGraphics3D graphics = new WLGraphics3D(pose, source, deltaTracker);
        WalksyLibEvents.RENDER_3D.invoker().render(graphics);
        graphics.finish();
    }
}
