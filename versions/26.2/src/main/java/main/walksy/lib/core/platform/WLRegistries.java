package main.walksy.lib.core.platform;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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

import java.util.List;
import java.util.Map;

public final class WLRegistries {
    private WLRegistries() {
    }

    public static void bindComponents(final HolderLookup.Provider provider) {
        for (final var pending : BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(provider)) {
            pending.apply();
        }
    }

    public static CloseableResourceManager vanillaServerData() {
        return new MultiPackResourceManager(PackType.SERVER_DATA, List.<PackResources>of(ServerPacksSource.createVanillaPackSource()));
    }

    public static Runnable loadVanillaTags(final ResourceManager resources) {
        final List<Registry.PendingTags<?>> pending = TagLoader.loadTagsForExistingRegistries(resources, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        return () -> {
            for (final Registry.PendingTags<?> tags : pending) {
                tags.apply();
            }
        };
    }

    public static Map<TagKey<Enchantment>, List<Holder<Enchantment>>> loadEnchantmentTags(final ResourceManager resources, final HolderLookup.RegistryLookup<Enchantment> lookup) {
        return TagLoader.loadTagsForRegistry(resources, Registries.ENCHANTMENT, TagLoader.ElementLookup.fromGetters(Registries.ENCHANTMENT, lookup, lookup));
    }

    public static boolean componentsBound(final Holder.Reference<Item> holder) {
        return holder.areComponentsBound();
    }

    public static DataComponentMap components(final Holder.Reference<Item> holder) {
        return holder.components();
    }

    public static Identifier soundId(final SoundEvent sound) {
        return sound.location();
    }
}
