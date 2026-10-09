package main.walksy.lib.core.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WLRegistries {
    private WLRegistries() {
    }

    public static void bindComponents(final HolderLookup.Provider provider) {
    }

    public static CloseableResourceManager vanillaServerData() {
        return new MultiPackResourceManager(PackType.SERVER_DATA, List.<PackResources>of(ServerPacksSource.createVanillaPackSource()));
    }

    public static Runnable loadVanillaTags(final ResourceManager resources) {
        final List<Runnable> binders = new ArrayList<>();
        for (final Registry<?> registry : BuiltInRegistries.REGISTRY) {
            binders.add(tagBinder(registry, resources));
        }
        return () -> binders.forEach(Runnable::run);
    }

    public static Map<TagKey<Enchantment>, List<Holder<Enchantment>>> loadEnchantmentTags(final ResourceManager resources, final HolderLookup.RegistryLookup<Enchantment> lookup) {
        final TagLoader<Holder<Enchantment>> loader = new TagLoader<>(id -> lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id)), Registries.tagsDirPath(Registries.ENCHANTMENT));
        final Map<Identifier, Collection<Holder<Enchantment>>> built = loader.build(loader.load(resources));
        final Map<TagKey<Enchantment>, List<Holder<Enchantment>>> tags = new HashMap<>(built.size());
        built.forEach((id, members) -> tags.put(TagKey.create(Registries.ENCHANTMENT, id), List.copyOf(members)));
        return tags;
    }

    private static <T> Runnable tagBinder(final Registry<T> registry, final ResourceManager resources) {
        final ResourceKey<? extends Registry<T>> key = registry.key();
        final TagLoader<Holder<T>> loader = new TagLoader<>(id -> registry.getHolder(ResourceKey.create(key, id)), Registries.tagsDirPath(key));
        final Map<Identifier, Collection<Holder<T>>> built = loader.loadAndBuild(resources);
        final Map<TagKey<T>, List<Holder<T>>> tags = new HashMap<>(built.size());
        built.forEach((id, members) -> tags.put(TagKey.create(key, id), List.copyOf(members)));
        return () -> registry.bindTags(tags);
    }

    public static boolean componentsBound(final Holder.Reference<Item> holder) {
        return true;
    }

    public static DataComponentMap components(final Holder.Reference<Item> holder) {
        return holder.value().components();
    }

    public static Identifier soundId(final SoundEvent sound) {
        return sound.getLocation();
    }
}
