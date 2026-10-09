package main.walksy.lib.core.platform;

import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.core.WalksyLibConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class WLKeys {
    private static final Map<String, KeyMapping.Category> CATEGORIES = new HashMap<>();
    private static final Map<KeyMapping.Category, String> CATEGORY_NAMES = new HashMap<>();

    private WLKeys() {
    }

    public static KeyMapping register(final String name, final int defaultKey, final String category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYBOARD, defaultKey, categoryFor(category)));
    }

    public static InputConstants.Key boundKey(final KeyMapping mapping) {
        return KeyMappingHelper.getBoundKeyOf(mapping);
    }

    public static InputConstants.Key keyboardKey(final int code) {
        return InputConstants.Type.KEYBOARD.getOrCreate(code);
    }

    public static InputConstants.Key keyOf(final int code, final int keycode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(code == InputConstants.UNKNOWN.getValue() ? keycode : code);
    }

    public static boolean isKeyDown(final int key) {
        return InputConstants.isKeyDown(key);
    }

    public static String categoryName(final KeyMapping.Category category) {
        return CATEGORY_NAMES.get(category);
    }

    private static KeyMapping.Category categoryFor(final String name) {
        final String sanitized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return CATEGORIES.computeIfAbsent(sanitized, path -> {
            final KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, path));
            CATEGORY_NAMES.put(category, name);
            return category;
        });
    }
}
