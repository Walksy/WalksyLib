package main.walksy.lib.api;

import main.walksy.lib.core.config.impl.LocalConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface WalksyLibConfig {

    Map<Class<? extends WalksyLibConfig>, LocalConfig> CACHE = new ConcurrentHashMap<>();

    LocalConfig define();

    default LocalConfig getOrCreateConfig() {
        return CACHE.computeIfAbsent(this.getClass(), clazz -> this.define());
    }
}
