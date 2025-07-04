package test.walksy.config;

import net.minecraft.client.gui.screen.Screen;
import walksy.lib.api.WalksyLibApi;
import walksy.lib.core.gui.WalksyLibScreenManager;

import java.util.function.UnaryOperator;

public class WalksyLibIntegration implements WalksyLibApi {

    @Override
    public UnaryOperator<Screen> getConfigScreen() {
        return parent -> WalksyLibScreenManager.create(parent, new Config());
    }
}
