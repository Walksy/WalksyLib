package test.walksy;

import main.walksy.lib.core.WalksyLib;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import test.walksy.config.Config;

public class WalksyLibTest implements ModInitializer {

    @Override
    public void onInitialize() {
        WalksyLib.onInitialize(new Config());

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                Config.testGrid.render(drawContext));
    }
}
