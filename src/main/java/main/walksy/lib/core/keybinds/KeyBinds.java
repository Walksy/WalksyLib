package main.walksy.lib.core.keybinds;

import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.api.config.PathUtils;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.platform.WLKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KeyBinds {
    public static final String CATEGORY = "Key Binds";
    private final List<WLKeyMapping> registry = new ArrayList<>();
    private Config config;
    private int revision;
    private int builtRevision = -1;

    public void register(final WLKeyMapping keymapping) {
        this.registry.add(keymapping);
        this.revision++;
    }

    public void unregister(final WLKeyMapping keymapping) {
        if (this.registry.remove(keymapping)) {
            this.revision++;
        }
    }

    public boolean hasAny() {
        return !this.registry.isEmpty();
    }

    public List<String> conflictsFor(final WLKeyMapping keymapping) {
        if (keymapping == null || keymapping.isUnbound()) {
            return List.of();
        }
        final InputConstants.Key key = keymapping.currentKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return List.of();
        }
        final Set<String> names = new LinkedHashSet<>();
        for (final WLKeyMapping other : this.registry) {
            if (other != keymapping && !other.isUnbound() && key.equals(other.currentKey())) {
                names.add(other.name());
            }
        }
        final Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null) {
            for (final KeyMapping mapping : client.options.keyMappings) {
                if (mapping == null || mapping.isUnbound() || !key.equals(WLKeys.boundKey(mapping)) || this.isOurs(mapping)) {
                    continue;
                }
                names.add(Component.translatable(mapping.getName()).getString());
            }
        }
        return List.copyOf(names);
    }

    private boolean isOurs(final KeyMapping mapping) {
        for (final WLKeyMapping keymapping : this.registry) {
            if (keymapping.wraps(mapping)) {
                return true;
            }
        }
        return false;
    }

    public Config config() {
        if (this.config != null && this.builtRevision == this.revision) {
            return this.config;
        }
        if (this.config != null) {
            this.config.save();
        }
        final Config rebuilt = this.build();
        rebuilt.load();
        this.config = rebuilt;
        this.builtRevision = this.revision;
        return this.config;
    }

    public void save() {
        if (this.config != null) {
            this.config().save();
        }
    }

    private Config build() {
        final Category.Builder category = Category.builder(CATEGORY).library();
        final Set<String> categories = new LinkedHashSet<>();
        for (final WLKeyMapping keymapping : this.registry) {
            categories.add(keymapping.category());
        }
        for (final String groupName : categories) {
            final OptionGroup.Builder group = OptionGroup.builder(groupName);
            for (final WLKeyMapping keymapping : this.registry) {
                if (keymapping.category().equals(groupName)) {
                    group.option(Options.ofKeybind(keymapping).build());
                }
            }
            category.group(group.build());
        }
        return Config.builder().path(PathUtils.ofLibraryDir("walksylib")).sharedFile().category(category.build()).build();
    }
}
