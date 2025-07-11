package main.walksy.lib.core.gui;

import net.minecraft.client.gui.screen.Screen;
import main.walksy.lib.core.config.WalksyLibConfig;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;

public class WalksyLibScreenManager {

    public static Screen create(Screen parent, WalksyLibConfig config) {
        return new WalksyLibConfigScreen(parent, config);
    }

    public static void openAllMods()
    {

    }

    public static class Globals
    {
        public static boolean DEBUG = false;

        public static int OPTION_WIDTH = 450;
        public static int OPTION_HEIGHT = 20;
        public static int OPTION_GROUP_SEPARATION = 10;

        public static int OPTION_PANEL_STARTX;
        public static int OPTION_PANEL_STARTY;
        public static int OPTION_PANEL_ENDX;
        public static int OPTION_PANEL_ENDY;
    }
}
