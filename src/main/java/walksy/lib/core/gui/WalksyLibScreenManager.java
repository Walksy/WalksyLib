package walksy.lib.core.gui;

import net.minecraft.client.gui.screen.Screen;
import walksy.lib.core.config.WalksyLibConfig;
import walksy.lib.core.gui.impl.WalksyLibConfigScreen;

public class WalksyLibScreenManager {

    public static Screen create(Screen parent, WalksyLibConfig config) {
        return new WalksyLibConfigScreen(parent, config);
    }

    public static void openAllMods()
    {

    }
}
