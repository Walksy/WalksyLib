package main.walksy.lib.core.mods;

import main.walksy.lib.api.WalksyLibApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;

public class ModEntryPointList {

    private final ArrayList<Mod> entries;

    public ModEntryPointList() {
        this.entries = new ArrayList<>();
    }

    public void retrieve() {
        this.entries.clear();
        FabricLoader.getInstance().getEntrypointContainers("walksylib", WalksyLibApi.class)
                .forEach(entry -> {
                    final WalksyLibApi api = entry.getEntrypoint();
                    this.entries.add(new Mod(entry.getProvider(), api.getConfig(), api::getOverridableScreen, api.getConflictedConfigButtonTitles()));
                });
    }

    public ArrayList<Mod> get() {
        return this.entries;
    }

    public void loadModConfigs() {
        for (final Mod mod : this.entries) {
            if (mod.hasConfig()) {
                mod.getConfig().onLoad();
            }
        }
    }
}
