package main.walksy.lib.core;

import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.Tickable;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.manager.WalksyLibShieldStateManager;
import main.walksy.lib.core.manager.WalksyLibTeamManager;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.core.mods.ModEntryPointList;

import java.util.ArrayList;
import java.util.List;

public class WalksyLib {
    static WalksyLib instance;
    private final WalksyLibShieldStateManager shieldStateManager;
    private final WalksyLibTeamManager teamManager;
    private final ModEntryPointList modEntryPointList;
    private Tickable[] tickableOptions = new Tickable[0];

    public WalksyLib() {
        if (instance == null) {
            instance = this;
        }
        this.shieldStateManager = new WalksyLibShieldStateManager();
        this.teamManager = new WalksyLibTeamManager();
        this.modEntryPointList = new ModEntryPointList();
        this.modEntryPointList.retrieve();
    }

    public void load() {
        this.modEntryPointList.get().forEach(mod -> mod.getConfig().load());
        this.teamManager.load();
    }

    public void tick() {
        this.retrieveTickableOptions();
        for (Tickable tickable : this.tickableOptions) {
            tickable.tick();
        }
    }

    public void retrieveTickableOptions() {
        List<Tickable> tickables = new ArrayList<>();

        for (Mod mod : this.modEntryPointList.get()) {
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
        return instance;
    }

    public WalksyLibShieldStateManager getShieldStateManager() {
        return this.shieldStateManager;
    }

    public WalksyLibTeamManager getTeamManager() {
        return this.teamManager;
    }
}
