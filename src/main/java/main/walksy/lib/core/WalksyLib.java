package main.walksy.lib.core;

import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.Tickable;
import main.walksy.lib.core.manager.WalksyLibShieldStateManager;
import main.walksy.lib.core.manager.WalksyLibTeamManager;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.ModEntryPointList;

import java.util.ArrayList;
import java.util.List;

public class WalksyLib {
    private static WalksyLib instance;
    private WalksyLibShieldStateManager shieldStateManager;
    private WalksyLibTeamManager teamManager;
    private ModEntryPointList modEntryPointList;
    private Tickable[] tickableOptions = new Tickable[0];

    public void setup() {
        this.shieldStateManager = new WalksyLibShieldStateManager();
        this.teamManager = new WalksyLibTeamManager();
        this.modEntryPointList = new ModEntryPointList();
    }

    public void load() {
        this.modEntryPointList.retrieve();
        this.modEntryPointList.loadModConfigs();
        this.teamManager.load();
        this.retrieveTickableOptions();
    }

    public void tick() {
        for (Tickable tickable : this.tickableOptions) {
            tickable.tick();
        }
    }

    public void retrieveTickableOptions() {
        List<Tickable> tickables = new ArrayList<>();

        for (Mod mod : this.modEntryPointList.get()) {
            if (!mod.hasConfig()) {
                continue;
            }
            for (Category category : mod.getConfig().categories()) {
                for (OptionGroup group : category.optionGroups()) {
                    for (Option<?> option : group.getOptions()) {
                        if (option.getValue() instanceof Tickable tickable) {
                            tickables.add(tickable);
                        }
                    }
                }
            }
        }

        this.tickableOptions = tickables.toArray(new Tickable[0]);
    }

    public static WalksyLib getInstance() {
        if (instance == null) {
            instance = new WalksyLib();
        }
        return instance;
    }

    public WalksyLibShieldStateManager getShieldStateManager() {
        return this.shieldStateManager;
    }

    public WalksyLibTeamManager getTeamManager() {
        return this.teamManager;
    }

    private WalksyLib() {

    }
}
