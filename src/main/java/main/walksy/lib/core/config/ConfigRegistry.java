package main.walksy.lib.core.config;

import main.walksy.lib.api.WalksyLibConfig;
import main.walksy.lib.api.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigRegistry {
    private static final Map<Class<?>, Config> CONFIGS = new ConcurrentHashMap<>();

    private ConfigRegistry() {
    }

    public static Config getOrCreate(final WalksyLibConfig definition) {
        return CONFIGS.computeIfAbsent(definition.getClass(), ignored -> definition.define());
    }

    public static Config get(final Class<? extends WalksyLibConfig> type) {
        return CONFIGS.computeIfAbsent(type, ConfigRegistry::build);
    }

    private static Config build(final Class<?> type) {
        try {
            final var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            final WalksyLibConfig definition = (WalksyLibConfig) constructor.newInstance();
            return definition.define();
        } catch (final ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException(type.getName() + " must have a no-argument constructor to be looked up by class", e);
        }
    }
}
