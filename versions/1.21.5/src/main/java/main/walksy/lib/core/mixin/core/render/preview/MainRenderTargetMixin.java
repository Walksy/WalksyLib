package main.walksy.lib.core.mixin.core.render.preview;

import main.walksy.lib.core.WalksyLib;
import com.mojang.blaze3d.pipeline.RenderTarget;
import main.walksy.lib.core.render.preview.PreviewItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MainRenderTargetMixin {
    @Unique
    private static final boolean WALKSYLIB$VULKANMOD = FabricLoader.getInstance().isModLoaded("vulkanmod");

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void walksylib$redirectPreviewEntity(final CallbackInfoReturnable<RenderTarget> cir) {
        final RenderTarget entity = WalksyLib.getInstance().getPreviewEntities().redirectedTarget();
        final RenderTarget redirected = entity != null ? entity : PreviewItems.redirectedTarget();
        if (redirected != null && !walksylib$calledByVulkanMod()) {
            cir.setReturnValue(redirected);
        }
    }

    @Unique
    private static boolean walksylib$calledByVulkanMod() {
        if (!WALKSYLIB$VULKANMOD) {
            return false;
        }
        final String self = Minecraft.class.getName();
        return StackWalker.getInstance().walk(frames -> frames.map(StackWalker.StackFrame::getClassName).filter(name -> !name.equals(self) && !name.startsWith("main.walksy.")).findFirst()).map(name -> name.startsWith("net.vulkanmod.")).orElse(false);
    }
}
