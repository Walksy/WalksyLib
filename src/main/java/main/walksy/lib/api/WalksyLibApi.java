package main.walksy.lib.api;

import main.walksy.lib.core.config.impl.ModConfig;
import main.walksy.lib.core.gui.impl.BaseScreen;
import net.minecraft.client.gui.screens.Screen;

public interface WalksyLibApi {

    default ModConfig getConfig() {
        return null;
    }

    default BaseScreen getOverridableScreen(Screen parent) {
        return null;
    }

    default String[] getConflictedConfigButtonTitles() {
        return new String[]{"Screen", "Config"};
    }
}