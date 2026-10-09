package main.walksy.lib.api.config.value;

import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.platform.WLModels;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public interface ListEntries {

    List<String> values();

    default String label(final String value) {
        return value;
    }

    @Nullable
    default ListIcon icon(final String value) {
        return null;
    }

    default boolean allowsCustom() {
        return false;
    }

    static ListEntries items() {
        return new RegistryEntries<>(BuiltInRegistries.ITEM, item -> Component.translatable(item.getDescriptionId()).getString(), ListEntries::itemIcon);
    }

    static ListEntries blocks() {
        return new RegistryEntries<>(BuiltInRegistries.BLOCK, block -> block.getName().getString(), ListEntries::blockIcon);
    }

    static ListEntries sounds() {
        return new RegistryEntries<>(BuiltInRegistries.SOUND_EVENT, null, null);
    }

    @SuppressWarnings("deprecation")
    static ListEntries particles() {
        return new RegistryEntries<>(BuiltInRegistries.PARTICLE_TYPE, null, id -> ListIcon.sprite(TextureAtlas.LOCATION_PARTICLES, id.withPrefix("particle/")));
    }

    static ListEntries onlinePlayers() {
        return new ListEntries() {
            @Override
            public List<String> values() {
                return WLClient.onlinePlayerNames();
            }

            @Override
            public boolean allowsCustom() {
                return true;
            }
        };
    }

    static ListEntries entities() {
        return new RegistryEntries<>(BuiltInRegistries.ENTITY_TYPE, type -> type.getDescription().getString(), null);
    }

    static ListEntries of(final Collection<String> values) {
        final List<String> snapshot = List.copyOf(values);
        return () -> snapshot;
    }

    static ListEntries of(final Collection<String> values, final Function<String, String> label, @Nullable final Function<String, ListIcon> icon) {
        final List<String> snapshot = List.copyOf(values);
        return new ListEntries() {
            @Override
            public List<String> values() {
                return snapshot;
            }

            @Override
            public String label(final String value) {
                return label.apply(value);
            }

            @Override
            public ListIcon icon(final String value) {
                return icon == null ? null : icon.apply(value);
            }
        };
    }

    @SuppressWarnings("deprecation")
    private static ListIcon itemIcon(final Identifier id) {
        return ListIcon.sprite(TextureAtlas.LOCATION_ITEMS, id.withPrefix("item/")).or(blockIcon(id));
    }

    @SuppressWarnings("deprecation")
    private static ListIcon blockIcon(final Identifier id) {
        return ListIcon.sprite(TextureAtlas.LOCATION_BLOCKS, id.withPrefix("block/")).or(ListIcon.resolved(() -> particleSprite(id)));
    }

    private static TextureAtlasSprite particleSprite(final Identifier id) {
        final Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return WLModels.particleSprite(block);
    }

    static String humanize(final Identifier id) {
        final StringBuilder out = new StringBuilder(id.getPath().length());
        for (final String word : id.getPath().replace('/', ' ').split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.isEmpty() ? id.toString() : out.toString();
    }

    final class RegistryEntries<T> implements ListEntries {

        private final Registry<T> registry;

        @Nullable
        private final Function<T, String> labeller;
        @Nullable
        private final Function<Identifier, ListIcon> iconFactory;

        private List<String> values;
        private Map<String, String> labels;

        private final Map<String, ListIcon> iconCache = new HashMap<>();

        RegistryEntries(final Registry<T> registry, @Nullable final Function<T, String> labeller, @Nullable final Function<Identifier, ListIcon> iconFactory) {
            this.registry = registry;
            this.labeller = labeller;
            this.iconFactory = iconFactory;
        }

        @Override
        public List<String> values() {
            if (this.values == null) {
                this.resolve();
            }
            return this.values;
        }

        @Override
        public String label(final String value) {
            if (this.labels == null) {
                this.resolve();
            }
            return this.labels.getOrDefault(value, value);
        }

        @Override
        public ListIcon icon(final String value) {
            if (this.iconFactory == null) {
                return null;
            }
            if (this.iconCache.containsKey(value)) {
                return this.iconCache.get(value);
            }
            final Identifier id = Identifier.tryParse(value);
            final ListIcon icon = id == null ? null : this.iconFactory.apply(id);
            this.iconCache.put(value, icon);
            return icon;
        }

        private void resolve() {
            final List<String> ids = new ArrayList<>();
            final Map<String, String> names = new HashMap<>();
            for (final T entry : this.registry) {
                final Identifier id = this.registry.getKey(entry);
                if (id == null) {
                    continue;
                }
                final String value = id.toString();
                ids.add(value);
                names.put(value, this.labeller == null ? humanize(id) : this.labeller.apply(entry));
            }
            ids.sort(Comparator.comparing(v -> names.get(v).toLowerCase(Locale.ROOT)));
            this.labels = names;
            this.values = List.copyOf(ids);
        }
    }
}
