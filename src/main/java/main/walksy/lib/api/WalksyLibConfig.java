package main.walksy.lib.api;

import main.walksy.lib.api.config.Config;
import main.walksy.lib.core.config.ConfigRegistry;

public interface WalksyLibConfig {

    Config define();

    default Config getOrCreateConfig()   {
        return ConfigRegistry.getOrCreate(this);
    }
}
