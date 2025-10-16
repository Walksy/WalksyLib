package main.walksy.lib.core.manager;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.gui.impl.APIScreen;
import main.walksy.lib.core.gui.impl.HudEditorScreen;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class WalksyLibScreenManager {

    public PixelGrid gridClipboard = null;

    public Screen create(Screen parent) {
        WalksyLib.retrieveEntryPointList();
        return new WalksyLibConfigScreen(parent);
    }

    public void openAPIScreen(Screen parent)
    {
        MinecraftClient.getInstance().setScreen(new APIScreen(parent));
    }

    public Screen getAndOpenAPIScreen(Screen parent) {
        APIScreen screen = new APIScreen(parent);
        MinecraftClient.getInstance().setScreen(screen);
        return screen;
    }

    public void openHudEditorScreen(WalksyLibConfigScreen screen, Option<?> option)
    {
        MinecraftClient.getInstance().setScreen(new HudEditorScreen(screen, option));
    }

    public void tick() {
        LocalConfig config = WalksyLib.getInstance().getConfigManager().getLocal();
        if (config == null) return;

        for (Category category : config.categories()) {
            for (Option<?> option : category.options()) {
                option.tick();
            }

            for (var group : category.optionGroups()) {
                for (Option<?> option : group.getOptions()) {
                    option.tick();
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
