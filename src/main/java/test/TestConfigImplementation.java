package test;

import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.core.config.impl.LocalConfig;

public class TestConfigImplementation implements WalksyLibApi {

    @Override
    public LocalConfig getConfig() {
        return new TestConfig().getOrCreateConfig();
    }
}
