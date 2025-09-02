package main.walksy.lib.core;

import main.walksy.lib.core.config.WalksyLibConfig;
import main.walksy.lib.core.manager.WalksyLibScreenManager;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.renderer.Render2D;

import java.util.logging.Logger;

public class WalksyLib {
    //TODO Create own logger class to log to custom log page
    static WalksyLib instance;
    static Logger LOGGER = Logger.getLogger("WalksyLib");
    private final WalksyLibScreenManager screenManager;
    private final WalksyLibConfigManager configManager;
    private final Render2D renderer2D;

    private WalksyLib(WalksyLibConfig config)
    {
        try {
            this.screenManager = new WalksyLibScreenManager();
            this.configManager = new WalksyLibConfigManager(config.define());
            this.configManager.load(null);
            this.renderer2D = new Render2D();
        } finally {
            LOGGER.info("WalksyLib successfully initialized!");
        } //TODO catch
    }

    public static void onInitialize(WalksyLibConfig config)
    {
        instance = new WalksyLib(config);
    }

    public static WalksyLib getInstance()
    {
        return instance;
    }

    public Render2D get2DRenderer()
    {
        return this.renderer2D;
    }

    public WalksyLibScreenManager getScreenManager()
    {
        return this.screenManager;
    }

    public WalksyLibConfigManager getConfigManager()
    {
        return this.configManager;
    }
}
