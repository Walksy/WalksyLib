package main.walksy.lib.core.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.api.render.WLGraphics3D;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0))
    private void onRenderEntities(final DeltaTracker deltaTracker, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f frustumMatrix, final Matrix4f projectionMatrix, final CallbackInfo ci) {
        final WLGraphics3D graphics = new WLGraphics3D(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), deltaTracker);
        WalksyLibEvents.RENDER_3D.invoker().render(graphics);
        graphics.finish();
    }
}
