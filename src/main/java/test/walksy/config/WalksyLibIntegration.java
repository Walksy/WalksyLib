package test.walksy.config;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.gui.screen.Screen;
import main.walksy.lib.api.WalksyLibApi;

import java.util.function.UnaryOperator;

public class WalksyLibIntegration implements WalksyLibApi {

    @Override
    public UnaryOperator<Screen> getConfigScreen() {
        return parent -> WalksyLib.getInstance().getScreenManager().create(parent);
    }
}
