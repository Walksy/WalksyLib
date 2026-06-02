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
    private List<Tickable> tickableOptions;

    public void setup() {
        this.shieldStateManager = new WalksyLibShieldStateManager();
        this.teamManager = new WalksyLibTeamManager();
        this.modEntryPointList = new ModEntryPointList();
        this.tickableOptions = new ArrayList<>();
    }

    public void load() {
        this.modEntryPointList.retrieve();
        this.modEntryPointList.loadModConfigs();
        this.teamManager.load();
        this.retrieveTickableOptions();
    }

    public void tick() {
        for (final Tickable tickable : this.tickableOptions) {
            tickable.tick();
        }
    }

    public void retrieveTickableOptions() {
        for (final Mod mod : this.modEntryPointList.get()) {
            if (!mod.hasConfig()) {
                continue;
            }
            for (final Category category : mod.getConfig().categories()) {
                for (final OptionGroup group : category.optionGroups()) {
                    for (final Option<?> option : group.getOptions()) {
                        if (option.getValue() instanceof Tickable tickable) {
                            this.tickableOptions.add(tickable);
                        }
                    }
                }
            }
        }
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

    private WalksyLib() {}
}
