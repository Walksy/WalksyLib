package main.walksy.lib.api.config.option;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.option.builder.BooleanOptionBuilder;
import main.walksy.lib.api.config.option.builder.ColorOptionBuilder;
import main.walksy.lib.api.config.option.builder.CroppedTextureOptionBuilder;
import main.walksy.lib.api.config.option.builder.NumberOptionBuilder;
import main.walksy.lib.api.config.option.builder.OptionPageBuilder;
import main.walksy.lib.api.config.option.builder.PageListOptionBuilder;
import main.walksy.lib.api.config.option.builder.SimpleOptionBuilder;
import main.walksy.lib.api.config.option.builder.StringListOptionBuilder;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.ModLinks;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.api.config.value.Preview;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.core.hud.HudSpace;
import main.walksy.lib.core.teams.TeamOption;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Options {
    private Options() {
    }

    public static BooleanOptionBuilder ofBoolean(final String name, final Supplier<Boolean> getter, final boolean defaultValue, final Consumer<Boolean> setter) {
        return new BooleanOptionBuilder(name, getter, defaultValue, setter);
    }

    public static <T extends Number> NumberOptionBuilder<T> ofNumber(final String name, final Supplier<T> getter, final T defaultValue, final Consumer<T> setter) {
        return new NumberOptionBuilder<>(name, getter, defaultValue, setter);
    }

    public static SimpleOptionBuilder<String> ofString(final String name, final Supplier<String> getter, final String defaultValue, final Consumer<String> setter) {
        return new SimpleOptionBuilder<>(name, getter, defaultValue, setter, String.class);
    }

    public static StringListOptionBuilder ofStringList(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        return new StringListOptionBuilder(name, getter, defaultValue, setter);
    }

    public static StringListOptionBuilder ofSelectionList(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter, final ListEntries entries) {
        return new StringListOptionBuilder(name, getter, defaultValue, setter).entries(entries);
    }

    public static ColorOptionBuilder ofColor(final String name, final Supplier<WLColor> getter, final WLColor defaultValue, final Consumer<WLColor> setter) {
        return new ColorOptionBuilder(name, getter, defaultValue, setter);
    }

    public static <E extends Enum<E>> SimpleOptionBuilder<E> ofEnum(final String name, final Supplier<E> getter, final E defaultValue, final Consumer<E> setter, final Class<E> enumClass) {
        return new SimpleOptionBuilder<>(name, getter, defaultValue, setter, enumClass);
    }

    public static SimpleOptionBuilder<Runnable> ofButton(final String name, final Runnable action) {
        return new SimpleOptionBuilder<>(name, () -> action, action, null, Runnable.class);
    }

    public static SimpleOptionBuilder<Runnable> ofHudSpace(final String name, final HudElement element) {
        final Runnable noop = () -> { };
        return new SimpleOptionBuilder<Runnable>(name, () -> noop, noop, null, Runnable.class).hudSpace(name, element);
    }

    public static SimpleOptionBuilder<Runnable> ofHudSpace(final HudSpace space) {
        final Runnable noop = () -> { };
        return new SimpleOptionBuilder<Runnable>(space.name(), () -> noop, noop, null, Runnable.class).hudSpace(space);
    }

    public static SimpleOptionBuilder<Identifier> ofTexture(final String name, final Supplier<Identifier> getter, final Identifier defaultValue, final Consumer<Identifier> setter) {
        return new SimpleOptionBuilder<>(name, getter, defaultValue, setter, Identifier.class);
    }

    public static CroppedTextureOptionBuilder ofCroppedTexture(final String name, final Supplier<CroppedTexture> getter, final CroppedTexture defaultValue, final Consumer<CroppedTexture> setter) {
        return new CroppedTextureOptionBuilder(name, getter, defaultValue, setter);
    }

    public static SimpleOptionBuilder<ItemInventory> ofInventory(final String name, final Supplier<ItemInventory> getter, final ItemInventory defaultValue, final Consumer<ItemInventory> setter) {
        return new SimpleOptionBuilder<>(name, getter, defaultValue, setter, ItemInventory.class);
    }

    public static SimpleOptionBuilder<PixelGridAnimation> ofPixelGridAnimation(final String name, final Supplier<PixelGridAnimation> getter, final PixelGridAnimation defaultValue, final Consumer<PixelGridAnimation> setter) {
        return new SimpleOptionBuilder<>(name, getter, defaultValue, setter, PixelGridAnimation.class);
    }

    public static OptionPageBuilder ofPage(final String name, final Supplier<String> getter, final String defaultValue, final Consumer<String> setter) {
        return new OptionPageBuilder(name, getter, defaultValue, setter);
    }

    public static PageListOptionBuilder ofPageList(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        return new PageListOptionBuilder(name, getter, defaultValue, setter);
    }

    public static PageListOptionBuilder ofTeams(final Function<String, List<Option<?>>> perTeamOptions) {
        return new PageListOptionBuilder("Teams", () -> WalksyLib.getInstance().getGlobalTeams().names(), List.of(), null)
            .readOnlyNames()
            .options(perTeamOptions);
    }

    public static PageListOptionBuilder ofTeamGroups(final Function<String, List<OptionGroup>> perTeamGroups) {
        return new PageListOptionBuilder("Teams", () -> WalksyLib.getInstance().getGlobalTeams().names(), List.of(), null)
            .readOnlyNames()
            .groups(perTeamGroups);
    }

    public static PageListOptionBuilder ofTeams(final TeamOption<?>... perTeam) {
        final List<TeamOption<?>> settings = List.of(perTeam);
        return new PageListOptionBuilder("Teams", () -> WalksyLib.getInstance().getGlobalTeams().names(), List.of(), null).readOnlyNames().options(team -> {
            final List<Option<?>> built = new ArrayList<>(settings.size());
            for (final TeamOption<?> setting : settings) {
                built.add(setting.optionFor(team));
            }
            return built;
        }).onRenamed((from, to) -> settings.forEach(setting -> setting.renameTeam(from, to))).onRemoved(team -> settings.forEach(setting -> setting.removeTeam(team)));
    }

    public static SimpleOptionBuilder<Preview> ofPreview(final String name, final Preview preview) {
        return new SimpleOptionBuilder<>(name, () -> preview, preview, null, Preview.class);
    }

    public static SimpleOptionBuilder<ModLinks> ofModLinks(final String name) {
        return new SimpleOptionBuilder<>(name, () -> ModLinks.INSTANCE, ModLinks.INSTANCE, null, ModLinks.class);
    }

    public static SimpleOptionBuilder<WLKeyMapping> ofKeybind(final WLKeyMapping keymapping) {
        return ofKeybind(keymapping.name(), keymapping);
    }

    public static SimpleOptionBuilder<WLKeyMapping> ofKeybind(final String name, final WLKeyMapping keymapping) {
        final Consumer<WLKeyMapping> setter = keymapping.isVanilla() ? null : loaded -> keymapping.rebind(loaded.currentKey());
        return new SimpleOptionBuilder<>(name, () -> keymapping, keymapping, setter, WLKeyMapping.class);
    }
}
