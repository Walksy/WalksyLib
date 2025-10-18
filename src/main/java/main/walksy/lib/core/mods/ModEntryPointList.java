package main.walksy.lib.core.mods;

import main.walksy.lib.api.WalksyLibApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;

public class ModEntryPointList {

    private final ArrayList<Mod> entries;

    public ModEntryPointList()
    {
        this.entries = new ArrayList<>();
    }

    public void retrieve()
    {
        this.entries.clear();
        FabricLoader.getInstance().getEntrypointContainers("walksylib", WalksyLibApi.class)
                .forEach(entry -> {
                    if (entry.getEntrypoint().getConfig() != null) {
                        entries.add(new Mod(entry.getProvider(), entry.getEntrypoint().getConfig()));
                    }
                });
    }

    public ArrayList<Mod> get()
    {
        return entries;
    }
}
