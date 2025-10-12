package main.walksy.lib.core;

import main.walksy.lib.core.callback.DropCallback;
import main.walksy.lib.core.config.WalksyLibConfig;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.mods.ModEntryPointList;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.renderer.Renderer2D;
import main.walksy.lib.core.utils.log.WalksyLibLogger;

import java.util.ArrayList;

public final class WalksyLib {
    static WalksyLib instance;
    private static final WalksyLibLogger LOGGER = new WalksyLibLogger();
    private static final Renderer2D renderer2D = new Renderer2D();
    private static final ModEntryPointList entryPointList = new ModEntryPointList();
    private final DropCallback windowDropCallback;
    private final WalksyLibScreenManager screenManager;
    private final WalksyLibConfig config;

    private WalksyLibConfigManager configManager;

    private WalksyLib(WalksyLibConfig config) {
        try {
            this.screenManager = new WalksyLibScreenManager();
            this.config = config;
            this.windowDropCallback = new DropCallback();
        } finally {
            LOGGER.info("WalksyLib Initialized");
        }
    }

    public static void onInitialize(WalksyLibConfig config) {
        instance = new WalksyLib(config);
    }

    public void onInitFinished() {
        this.configManager = new WalksyLibConfigManager(config.define());
        this.configManager.getLocal().load();
        this.configManager.getAPI().load();
        this.configManager.cleanCache();
        entryPointList.retrieve();
    }

    public WalksyLibScreenManager getScreenManager() {
        return this.screenManager;
    }

    public WalksyLibConfigManager getConfigManager() {
        return this.configManager;
    }

    public DropCallback getWindowDropCallback()
    {
        return this.windowDropCallback;
    }

    public static ArrayList<Mod> getEntryPointModList() {
        return entryPointList.get();
    }

    public static WalksyLib getInstance() {
        return instance;
    }

    public static Renderer2D get2DRenderer() {
        return renderer2D;
    }

    public static WalksyLibLogger getLogger() {
        return LOGGER;
    }
}
