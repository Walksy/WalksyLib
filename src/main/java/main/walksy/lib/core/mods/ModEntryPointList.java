package main.walksy.lib.core.mods;

import main.walksy.lib.api.WalksyLibApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;

public final class ModEntryPointList {
    private final ArrayList<Mod> entries;

    public ModEntryPointList() {
        this.entries = new ArrayList<>();
    }

    public void retrieve() {
        this.entries.clear();
        FabricLoader.getInstance().getEntrypointContainers("walksylib", WalksyLibApi.class).forEach(entry -> {
            final WalksyLibApi api = entry.getEntrypoint();
            this.entries.add(new Mod(entry.getProvider(), api.getConfig()));
        });
    }

    public ArrayList<Mod> getMods() {
        return this.entries;
    }

    public boolean contains(final String modId) {
        for (final Mod mod : this.entries) {
            if (mod.getContainer().getMetadata().getId().equals(modId)) {
                return true;
            }
        }
        return false;
    }

    public void loadModConfigs() {
        for (final Mod mod : this.entries) {
            if (mod.hasConfig()) {
                mod.getConfig().load();
            }
        }
    }

    public void saveModConfigs() {
        for (final Mod mod : this.entries) {
            if (mod.hasConfig()) {
                mod.getConfig().save();
            }
        }
    }
}
