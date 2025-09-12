package main.walksy.lib.core.mixin;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(RunArgs args, CallbackInfo ci)
    {
        WalksyLib.getInstance().onInitFinished();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci)
    {
        WalksyLib.getInstance().getScreenManager().tick();
    }
}
