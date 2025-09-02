package main.walksy.lib.core.manager;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import net.minecraft.client.gui.screen.Screen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;

import java.awt.*;

public class WalksyLibScreenManager {

    public PixelGrid gridClipboard = null;

    public Screen create(Screen parent) {
        return new WalksyLibConfigScreen(parent);
    }

    public void openAllMods()
    {

    }

    public void tick() {
        LocalConfig config = WalksyLib.getInstance().getConfigManager().localConfig;
        if (config == null) return;

        for (Category category : config.getCategories()) {
            for (Option<?> option : category.options()) {
                if (Color.class.isAssignableFrom(option.getType())) {
                    option.tick();
                }
            }

            for (var group : category.optionGroups()) {
                for (Option<?> option : group.getOptions()) {
                    if (Color.class.isAssignableFrom(option.getType())) {
                        option.tick();
                    }
                }
            }
        }
    }


    public static class Globals
    {
        public static boolean DEBUG = false;

        public static int OPTION_WIDTH = 450 - 22;
        public static int OPTION_HEIGHT = 20;
        public static int OPTION_GROUP_SEPARATION = 10;

        public static int OPTION_PANEL_STARTX;
        public static int OPTION_PANEL_STARTY;
        public static int OPTION_PANEL_ENDX;
        public static int OPTION_PANEL_ENDY;
    }
}
