package main.walksy.lib.api;

import main.walksy.lib.api.config.Config;

public interface WalksyLibApi extends WalksyLibConfig {

    default Config getConfig() {
        return this.getOrCreateConfig();
    }
}
