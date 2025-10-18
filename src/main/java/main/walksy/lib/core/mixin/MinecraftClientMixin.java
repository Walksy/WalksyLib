package main.walksy.lib.core.mixin;

import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.utils.MarqueeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public Screen currentScreen;

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci)
    {
        if (this.currentScreen instanceof BaseScreen) {
            MarqueeUtil.tickCount++;
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    public void setScreen(Screen screen, CallbackInfo ci)
    {
        if (screen instanceof BaseScreen)
        {
            MarqueeUtil.tickCount = 0;
        }
    }
}
