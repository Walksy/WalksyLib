package main.walksy.lib.core.config;

import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.value.Tickable;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.ModEntryPointList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class TickableOptions {
    private final List<Supplier<Config>> sources = new ArrayList<>();
    private final Set<Tickable> ticked = Collections.newSetFromMap(new IdentityHashMap<>());

    public void collect(final ModEntryPointList modEntryPointList) {
        this.sources.clear();
        for (final Mod mod : modEntryPointList.getMods()) {
            this.sources.add(mod::getConfig);
        }
    }

    public void add(final Supplier<Config> source) {
        this.sources.add(source);
    }

    public void tick() {
        this.ticked.clear();
        for (final Supplier<Config> source : this.sources) {
            final Config config = source.get();
            if (config == null) {
                continue;
            }
            config.options().forEach(option -> {
                if (option.getValue() instanceof final Tickable tickable && this.ticked.add(tickable)) {
                    tickable.tick();
                }
            });
        }
    }
}
