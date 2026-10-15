package main.walksy.lib.core.platform.compat.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.platform.compat.NeoForgeBootstrap;
import net.minecraft.client.KeyMapping;

public final class KeyMappingHelper {
    private KeyMappingHelper() {
    }

    public static KeyMapping registerKeyMapping(final KeyMapping mapping) {
        NeoForgeBootstrap.addKeyMapping(mapping);
        return mapping;
    }

    public static InputConstants.Key getBoundKeyOf(final KeyMapping mapping) {
        return mapping.getKey();
    }
}
