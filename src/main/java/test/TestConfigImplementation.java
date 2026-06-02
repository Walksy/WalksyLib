package test;

import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.core.config.impl.ModConfig;

public class TestConfigImplementation implements WalksyLibApi {

    @Override
    public ModConfig getConfig() {
        return new TestConfig().getOrCreateConfig();
    }
}
