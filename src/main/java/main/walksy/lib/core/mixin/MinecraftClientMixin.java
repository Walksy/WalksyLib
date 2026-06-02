package main.walksy.lib.core.mixin;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.utils.MarqueeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    @Final
    public Gui gui;

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void onInit(final GameConfig gameConfig, final CallbackInfo ci) {
        WalksyLib.getInstance().setup();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private static void onInitFinished(final GameConfig gameConfig, final CallbackInfo ci) {
        WalksyLib.getInstance().load();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(final CallbackInfo ci) {
        if (this.gui.screen() instanceof BaseScreen) {
            MarqueeUtil.tickCount++;
        }
        if (WalksyLib.getInstance() != null) {
            WalksyLib.getInstance().tick();
        }
    }

    @Inject(method = "setScreenAndShow", at = @At("HEAD"))
    public void setScreen(final Screen screen, final CallbackInfo ci) {
        if (screen instanceof BaseScreen) {
            MarqueeUtil.tickCount = 0;
        }
    }
}
