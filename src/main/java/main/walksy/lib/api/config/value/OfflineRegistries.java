package main.walksy.lib.api.config.value;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.platform.WLRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OfflineRegistries {
    private volatile HolderLookup.Provider provider;
    private volatile Runnable pendingTags;
    private boolean tagsBound;
    private volatile Map<Identifier, Set<Identifier>> enchantmentTags = Map.of();
    private boolean componentsBound;
    private Thread warming;
    private boolean failed;

    private boolean disconnected() {
        final Minecraft client = Minecraft.getInstance();
        return client == null || (client.getConnection() == null && client.level == null);
    }

    public void warm() {
        if (this.provider != null || this.failed || this.warming != null || !this.disconnected()) {
            return;
        }
        this.warming = new Thread(() -> {
            try {
                this.provider = VanillaRegistries.createLookup();
            } catch (final Throwable e) {
                this.failed = true;
                WalksyLib.getLogger().err("Could not build offline registries: " + e);
                return;
            }
            try {
                this.pendingTags = this.loadTags();
            } catch (final Throwable e) {
                WalksyLib.getLogger().err("Could not read vanilla tags for offline editing (" + e + "); enchantment filtering will not work and WalksyLib will blow up in T-minus 10 seconds (teehee)");
            }
        }, "walksylib-offline-registries");
        this.warming.setPriority(Thread.MIN_PRIORITY);
        this.warming.setDaemon(true);
        this.warming.start();
    }

    public HolderLookup.Provider provider() {
        if (this.failed) {
            return null;
        }
        if (this.provider == null) {
            final Thread building = this.warming;
            if (building != null) {
                try {
                    building.join();
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            if (this.failed) {
                return null;
            }
        }
        if (this.provider == null) {
            try {
                this.provider = VanillaRegistries.createLookup();
            } catch (final Throwable e) {
                this.failed = true;
                WalksyLib.getLogger().err("Could not build offline registries: " + e);
                return null;
            }
        }
        this.bindComponents();
        this.ensureTags();
        return this.provider;
    }

    private void ensureTags() {
        if (this.tagsBound) {
            return;
        }
        if (this.pendingTags == null) {
            try {
                this.pendingTags = this.loadTags();
            } catch (final Throwable e) {
                this.tagsBound = true;
                WalksyLib.getLogger().err("Could not read vanilla tags for offline editing (" + e + ")");
                return;
            }
        }
        this.applyTags();
    }

    public void bindIfReady() {
        if (this.failed || !this.disconnected()) {
            return;
        }
        if (!this.componentsBound && this.provider != null) {
            this.bindComponents();
        }
        if (!this.tagsBound && this.pendingTags != null) {
            this.applyTags();
        }
    }

    private Runnable loadTags() {
        try (final CloseableResourceManager resources = WLRegistries.vanillaServerData()) {
            final Runnable pending = WLRegistries.loadVanillaTags(resources);
            this.enchantmentTags = this.loadEnchantmentTags(resources);
            return pending;
        }
    }

    private Map<Identifier, Set<Identifier>> loadEnchantmentTags(final ResourceManager resources) {
        final HolderLookup.Provider built = this.provider;
        if (built == null) {
            return Map.of();
        }
        final HolderLookup.RegistryLookup<Enchantment> lookup = built.lookup(Registries.ENCHANTMENT).orElse(null);
        if (lookup == null) {
            return Map.of();
        }
        final Map<TagKey<Enchantment>, List<Holder<Enchantment>>> loaded = WLRegistries.loadEnchantmentTags(resources, lookup);
        final Map<Identifier, Set<Identifier>> byId = new HashMap<>(loaded.size());
        loaded.forEach((tag, members) -> {
            final Set<Identifier> ids = new HashSet<>(members.size());
            for (final Holder<Enchantment> member : members) {
                member.unwrapKey().ifPresent(key -> ids.add(key.identifier()));
            }
            byId.put(tag.location(), ids);
        });
        return Map.copyOf(byId);
    }

    public Set<Identifier> enchantmentsInTag(final TagKey<Enchantment> tag) {
        if (tag == null) {
            return Set.of();
        }
        return this.enchantmentTags.getOrDefault(tag.location(), Set.of());
    }

    private void applyTags() {
        final Runnable pending = this.pendingTags;
        if (pending == null) {
            return;
        }
        this.tagsBound = true;
        try {
            pending.run();
        } catch (final Throwable e) {
            WalksyLib.getLogger().err("Could not bind offline tags: " + e);
        } finally {
            this.pendingTags = null;
        }
    }

    private void bindComponents() {
        if (this.componentsBound) {
            return;
        }
        try {
            WLRegistries.bindComponents(this.provider);
            this.componentsBound = true;
        } catch (final Throwable e) {
            this.failed = true;
            WalksyLib.getLogger().err("Could not bind offline item components: " + e);
        }
    }
}
