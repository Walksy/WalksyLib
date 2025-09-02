package test.walksy;

import main.walksy.lib.core.WalksyLib;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import test.walksy.config.Config;

public class WalksyLibTest implements ModInitializer {

    @Override
    public void onInitialize() {
        WalksyLib.onInitialize(new Config());

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
        {
            WalksyLib.getInstance().get2DRenderer().renderGridTexture(drawContext, Config.testGrid.getCurrentFrame(), drawContext.getScaledWindowWidth() / 2, drawContext.getScaledWindowHeight() / 2, 1, 0, 1);
        });
    }
}
