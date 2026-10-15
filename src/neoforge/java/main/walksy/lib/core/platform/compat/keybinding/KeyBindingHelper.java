package main.walksy.lib.core.platform.compat.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.platform.compat.NeoForgeBootstrap;
import net.minecraft.client.KeyMapping;

public final class KeyBindingHelper {
    private KeyBindingHelper() {
    }

    public static KeyMapping registerKeyBinding(final KeyMapping mapping) {
        NeoForgeBootstrap.addKeyMapping(mapping);
        return mapping;
    }

    public static InputConstants.Key getBoundKeyOf(final KeyMapping mapping) {
        return mapping.getKey();
    }
}
