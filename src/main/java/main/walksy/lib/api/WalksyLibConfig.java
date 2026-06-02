package main.walksy.lib.api;

import main.walksy.lib.core.config.impl.ModConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface WalksyLibConfig {

    Map<Class<? extends WalksyLibConfig>, ModConfig> CACHE = new ConcurrentHashMap<>();

    ModConfig define();

    default ModConfig getOrCreateConfig() {
        return CACHE.computeIfAbsent(this.getClass(), clazz -> this.define());
    }
}
