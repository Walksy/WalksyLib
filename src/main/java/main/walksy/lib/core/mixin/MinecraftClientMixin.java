package main.walksy.lib.core.mixin;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.manager.WalksyLibTeamManager;
import main.walksy.lib.core.mods.ModEntryPointList;
import main.walksy.lib.core.utils.MarqueeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    @Nullable
    public Screen screen;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInitFinished(GameConfig gameConfig, CallbackInfo ci) {
        WalksyLib walksyLib = new WalksyLib();
        walksyLib.load();
    }


    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if (this.screen instanceof BaseScreen) {
            MarqueeUtil.tickCount++;
        }
        if (WalksyLib.getInstance() != null) {
            WalksyLib.getInstance().tick();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    public void setScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof BaseScreen) {
            MarqueeUtil.tickCount = 0;
        }
    }
}
