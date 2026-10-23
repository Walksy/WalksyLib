package main.walksy.lib.core.hud;

import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.mods.Mod;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

record HudOptions(String owner, List<OptionGroup> groups) {

    static HudOptions of(final HudSpace space) {
        final List<String> wanted = space.element().hudOptions();
        for (final Mod mod : WalksyLib.getInstance().getModEntryPointList().getMods()) {
            if (!mod.hasConfig()) {
                continue;
            }
            final List<Category> categories = mod.getConfig().categories();
            for (final Category category : categories) {
                final List<OptionGroup> groups = groupsFor(space, category);
                if (groups != null) {
                    return new HudOptions(mod.getDisplayName(), wanted == null || wanted.isEmpty() ? groups : chosen(categories, Set.copyOf(wanted)));
                }
            }
        }
        return new HudOptions(ownerName(space), List.of());
    }

    private static List<OptionGroup> chosen(final List<Category> categories, final Set<String> keys) {
        final List<OptionGroup> groups = new ArrayList<>();
        for (final Category category : categories) {
            for (final OptionGroup group : category.groups()) {
                add(groups, group.getName(), group.getOptions(), keys);
            }
            add(groups, "", category.options(), keys);
        }
        return groups;
    }

    private static @Nullable List<OptionGroup> groupsFor(final HudSpace space, final Category category) {
        OptionGroup own = null;
        for (final OptionGroup group : category.groups()) {
            if (places(space, group.getOptions())) {
                own = group;
                break;
            }
        }
        final boolean loose = places(space, category.options());
        if (own == null && !loose) {
            return null;
        }
        final List<OptionGroup> groups = new ArrayList<>();
        if (own != null) {
            add(groups, own.getName(), own.getOptions(), null);
        } else {
            add(groups, "", category.options(), null);
        }
        for (final OptionGroup group : category.groups()) {
            if (group != own && !placesAny(group.getOptions())) {
                add(groups, group.getName(), group.getOptions(), null);
            }
        }
        if (!loose && !placesAny(category.options())) {
            add(groups, "", category.options(), null);
        }
        return groups;
    }

    private static boolean places(final HudSpace space, final List<Option<?>> options) {
        for (final Option<?> option : options) {
            if (option.getHudSpace() == space) {
                return true;
            }
        }
        return false;
    }

    private static boolean placesAny(final List<Option<?>> options) {
        for (final Option<?> option : options) {
            if (option.getHudSpace() != null || option.getHudElement() != null) {
                return true;
            }
        }
        return false;
    }

    private static void add(final List<OptionGroup> groups, final String name, final List<Option<?>> options, final @Nullable Set<String> keys) {
        final OptionGroup.Builder builder = OptionGroup.builder(name);
        boolean any = false;
        for (final Option<?> option : options) {
            if (option.getType() == Runnable.class && option.getHudElement() != null) {
                continue;
            }
            if (keys != null && !keys.contains(option.getSaveKey())) {
                continue;
            }
            builder.option(option);
            any = true;
        }
        if (any) {
            groups.add(builder.build());
        }
    }

    private static String ownerName(final HudSpace space) {
        final String id = space.id();
        if (id == null) {
            return "";
        }
        final int colon = id.indexOf(':');
        final String modId = colon < 0 ? id : id.substring(0, colon);
        return FabricLoader.getInstance().getModContainer(modId).map(container -> container.getMetadata().getName()).orElse(modId);
    }
}
