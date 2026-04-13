package main.walksy.lib.api;

import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.gui.impl.BaseScreen;
import net.minecraft.client.gui.screens.Screen;

public interface WalksyLibApi {
    
    /**
     * Used to populate option widgets inside {@link main.walksy.lib.core.gui.impl.WalksyLibConfigScreen}.
     * Return null if a config is not required or if an overridable screen will be used instead.
     */
    LocalConfig getConfig();


    default BaseScreen getOverridableScreen(Screen parent) {
        return null;
    }

    default String[] getConflictedConfigButtonTitles() {
        return new String[]{"Screen", "Config"};
    }
}