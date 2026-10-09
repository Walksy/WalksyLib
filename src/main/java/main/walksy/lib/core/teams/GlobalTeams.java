package main.walksy.lib.core.teams;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.OptionDescription;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.core.mods.Mod;
import main.walksy.lib.api.config.PathUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GlobalTeams {
    public static final String CATEGORY = "Teams";
    private final Map<String, List<String>> members = new LinkedHashMap<>();
    private List<String> names = new ArrayList<>();
    private Config config;

    public List<String> names() {
        return this.names;
    }

    public void setNames(final List<String> updated) {
        this.names = updated == null ? new ArrayList<>() : new ArrayList<>(updated);
    }

    public List<String> members(final String team) {
        return this.members.getOrDefault(team, List.of());
    }

    public String teamOf(final String player) {
        if (player == null) {
            return null;
        }
        final String wanted = player.trim().toLowerCase(Locale.ROOT);
        for (final String team : this.names) {
            for (final String member : this.members(team)) {
                if (member.trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                    return team;
                }
            }
        }
        return null;
    }

    public Config config() {
        if (this.config == null) {
            this.config = Config.builder()
                .path(PathUtils.ofGlobalDir("walksylib"))
                .sharedFile()
                .seedFrom(PathUtils.ofLibraryDir("walksylib"))
                .movedFrom(PathUtils.ofConfigDir("walksylib"))
                .category(Category.builder(CATEGORY)
                    .library()
                    .group(OptionGroup.builder("Teams")
                        .option(Options.ofPageList("Teams", this::names, List.of(), this::setNames)
                            .newEntryName("Team")
                            .options(team -> List.of(Options.ofSelectionList("Players", () -> this.members(team), List.of(), players -> this.members.put(team, new ArrayList<>(players)), ListEntries.onlinePlayers())
                                .description(OptionDescription.ofText(() -> "Pick from the players who are online, or type any username."))
                                .build()))
                            .description(OptionDescription.ofText(() -> "Teams are shared by every mod that uses them."))
                            .onRenamed(this::rename)
                            .onRemoved(this::remove)
                            .build())
                        .option(Options.ofModLinks("Used by").build())
                        .build())
                    .build())
                .build();
            this.config.load();
        }
        return this.config;
    }

    public void save() {
        if (this.config != null) {
            this.config.save();
        }
    }

    public void rename(final String from, final String to) {
        final List<String> moved = this.members.remove(from);
        if (moved != null) {
            this.members.put(to, moved);
        }
        for (final Option<?> option : this.teamOptions()) {
            option.renamePage(from, to);
        }
    }

    public void remove(final String name) {
        this.members.remove(name);
        for (final Option<?> option : this.teamOptions()) {
            option.removePage(name);
        }
    }

    public Map<Mod, Option<?>> usingMods() {
        final Map<Mod, Option<?>> found = new LinkedHashMap<>();
        for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            if (!mod.hasConfig()) {
                continue;
            }
            mod.getConfig().options().filter(Option::isTeamList).findFirst().ifPresent(option -> found.put(mod, option));
        }
        return found;
    }

    public String categoryOf(final Mod mod) {
        if (mod == null || !mod.hasConfig()) {
            return null;
        }
        for (final Category category : mod.getConfig().categories()) {
            if (category.allOptions().anyMatch(Option::isTeamList)) {
                return category.name();
            }
        }
        return null;
    }

    private List<Option<?>> teamOptions() {
        return new ArrayList<>(this.usingMods().values());
    }
}
