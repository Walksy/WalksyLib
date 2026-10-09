package main.walksy.lib.core.config.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.Option;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.core.config.serialization.adapters.CroppedTextureAdapter;
import main.walksy.lib.core.config.serialization.adapters.WLColorAdapter;
import main.walksy.lib.core.config.serialization.adapters.IdentifierAdapter;
import main.walksy.lib.core.config.serialization.adapters.IdentifierWrapperAdapter;
import main.walksy.lib.core.config.serialization.adapters.ItemInventoryAdapter;
import main.walksy.lib.core.config.serialization.adapters.PixelGridAdapter;
import main.walksy.lib.core.config.serialization.adapters.PixelGridAnimationAdapter;
import main.walksy.lib.core.config.serialization.adapters.WLKeyMappingAdapter;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ModLinks;
import main.walksy.lib.api.config.value.Preview;
import main.walksy.lib.api.config.value.PixelGrid;
import main.walksy.lib.api.config.value.PixelGridAnimation;
import main.walksy.lib.api.config.value.WLColor;
import main.walksy.lib.api.config.value.WLKeyMapping;
import main.walksy.lib.api.render.CachedTextures;
import main.walksy.lib.core.utils.IdentifierWrapper;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigSerializer {
    public static final int FORMAT_VERSION = 2;
    private static final String KEY_VALUE = "value";
    private static final String KEY_PAGES = "pages";
    private static final String GROUPED_KEY_SEPARATOR = "\0";
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(WLColor.class, new WLColorAdapter())
        .registerTypeAdapter(PixelGrid.class, new PixelGridAdapter())
        .registerTypeAdapter(PixelGridAnimation.class, new PixelGridAnimationAdapter())
        .registerTypeAdapter(ItemInventory.class, new ItemInventoryAdapter())
        .registerTypeAdapter(IdentifierWrapper.class, new IdentifierWrapperAdapter())
        .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
        .registerTypeAdapter(CroppedTexture.class, new CroppedTextureAdapter())
        .registerTypeAdapter(WLKeyMapping.class, new WLKeyMappingAdapter())
        .setPrettyPrinting()
        .create();

    private ConfigSerializer() {
    }

    public static JsonObject serialize(final Config config) {
        final JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT_VERSION);
        final JsonObject categories = new JsonObject();
        for (final Category category : config.categories()) {
            categories.add(category.name(), serializeCategory(category));
        }
        root.add("categories", categories);
        return root;
    }

    private static JsonObject serializeCategory(final Category category) {
        final JsonObject categoryObj = new JsonObject();
        final JsonObject options = serializeOptions(category.options());
        if (!options.isEmpty()) {
            categoryObj.add("options", options);
        }
        final JsonObject groups = new JsonObject();
        for (final OptionGroup group : category.groups()) {
            final JsonObject groupObj = new JsonObject();
            groupObj.addProperty("expanded", group.isExpanded());
            groupObj.add("options", serializeOptions(group.getOptions()));
            groups.add(group.getName(), groupObj);
        }
        categoryObj.add("groups", groups);
        return categoryObj;
    }

    private static JsonObject serializeOptions(final Iterable<Option<?>> options) {
        final JsonObject result = new JsonObject();
        for (final Option<?> option : options) {
            if (!isPersistent(option)) {
                continue;
            }
            final String key = option.getSaveKey();
            if (result.has(key)) {
                WalksyLib.getLogger().err("Two options are saved under the key '" + key + "' ('" + option.getName() + "' is the second). One of them will not persist -" + " give it its own OptionBuilder#saveKey.");
                continue;
            }
            result.add(key, serializeOption(option));
        }
        return result;
    }

    private static JsonElement serializeOption(final Option<?> option) {
        final JsonElement value = GSON.toJsonTree(option.getValue(), option.getType());
        if (!option.hasPages()) {
            return value;
        }
        final JsonObject envelope = new JsonObject();
        envelope.add(KEY_VALUE, value);
        final JsonObject pages = serializePages(option);
        if (!pages.isEmpty()) {
            envelope.add(KEY_PAGES, pages);
        }
        return envelope;
    }

    private static JsonObject serializePages(final Option<?> option) {
        final JsonObject pages = new JsonObject();
        for (final var entry : option.getPages().entrySet()) {
            final JsonObject pageOptions = serializeOptions(entry.getValue());
            if (!pageOptions.isEmpty()) {
                pages.add(entry.getKey(), pageOptions);
            }
        }
        return pages;
    }

    public static void apply(final Config config, final JsonObject root) {
        WalksyLib.getInstance().getOptionChangeLogger().suppress(() -> applyAll(config, root));
    }

    private static void applyAll(final Config config, final JsonObject root) {
        final JsonObject categories = asObject(root.get("categories"));
        if (categories == null) {
            return;
        }
        final Map<String, JsonElement> moved = uniqueSavedValues(categories);
        for (final Category category : config.categories()) {
            final JsonObject categoryObj = asObject(categories.get(category.name()));
            applyOptions(category.options(), categoryObj == null ? null : asObject(categoryObj.get("options")), moved, null);
            final JsonObject groups = categoryObj == null ? null : asObject(categoryObj.get("groups"));
            for (final OptionGroup group : category.groups()) {
                final JsonObject groupObj = groups == null ? null : asObject(groups.get(group.getName()));
                if (groupObj != null && groupObj.has("expanded")) {
                    group.setExpanded(groupObj.get("expanded").getAsBoolean());
                }
                applyOptions(group.getOptions(), groupObj == null ? null : asObject(groupObj.get("options")), moved, group.getName());
            }
            category.allOptions().forEach(ConfigSerializer::restoreSpriteTexture);
        }
    }

    private static Map<String, JsonElement> uniqueSavedValues(final JsonObject categories) {
        final Map<String, JsonElement> found = new HashMap<>();
        final Set<String> repeated = new HashSet<>();
        for (final String categoryName : categories.keySet()) {
            final JsonObject categoryObj = asObject(categories.get(categoryName));
            if (categoryObj == null) {
                continue;
            }
            collectSavedValues(asObject(categoryObj.get("options")), null, found, repeated);
            final JsonObject groups = asObject(categoryObj.get("groups"));
            if (groups == null) {
                continue;
            }
            for (final String groupName : groups.keySet()) {
                final JsonObject groupObj = asObject(groups.get(groupName));
                collectSavedValues(groupObj == null ? null : asObject(groupObj.get("options")), groupName, found, repeated);
            }
        }
        repeated.forEach(found::remove);
        return found;
    }

    private static void collectSavedValues(final JsonObject values, final String groupName, final Map<String, JsonElement> found, final Set<String> repeated) {
        if (values == null) {
            return;
        }
        for (final String key : values.keySet()) {
            remember(key, values.get(key), found, repeated);
            if (groupName != null) {
                remember(groupedKey(groupName, key), values.get(key), found, repeated);
            }
        }
    }

    private static void remember(final String key, final JsonElement value, final Map<String, JsonElement> found, final Set<String> repeated) {
        if (found.putIfAbsent(key, value) != null) {
            repeated.add(key);
        }
    }

    private static String groupedKey(final String groupName, final String key) {
        return groupName + GROUPED_KEY_SEPARATOR + key;
    }

    private static void applyOptions(final Iterable<Option<?>> options, final JsonObject values) {
        applyOptions(options, values, Map.of(), null);
    }

    private static void applyOptions(final Iterable<Option<?>> options, final JsonObject values, final Map<String, JsonElement> moved, final String groupName) {
        for (final Option<?> option : options) {
            if (!isPersistent(option)) {
                continue;
            }
            JsonElement element = values == null ? null : values.get(option.getSaveKey());
            if (element == null && groupName != null) {
                element = moved.get(groupedKey(groupName, option.getSaveKey()));
            }
            if (element == null) {
                element = moved.get(option.getSaveKey());
            }
            if (element == null || element.isJsonNull()) {
                continue;
            }
            applyOption(option, element);
        }
    }

    private static void applyOption(final Option<?> option, final JsonElement element) {
        final JsonObject envelope = asEnvelope(option, element);
        if (envelope == null) {
            applyValue(option, element);
            return;
        }
        final JsonElement value = envelope.get(KEY_VALUE);
        if (value != null && !value.isJsonNull()) {
            applyValue(option, value);
        }
        if (!option.hasPages()) {
            return;
        }
        final JsonObject pages = asObject(envelope.get(KEY_PAGES));
        if (pages == null) {
            return;
        }
        for (final var entry : option.getPages().entrySet()) {
            applyOptions(entry.getValue(), asObject(pages.get(entry.getKey())));
        }
    }

    private static JsonObject asEnvelope(final Option<?> option, final JsonElement element) {
        final JsonObject object = asObject(element);
        if (object == null || !object.has(KEY_VALUE)) {
            return null;
        }
        return option.hasPages() || object.has(KEY_PAGES) ? object : null;
    }

    private static void applyValue(final Option<?> option, final JsonElement element) {
        try {
            final Object value = readValue(option, element);
            if (value != null) {
                option.setValue(value);
                preloadTexture(value);
            }
        } catch (final RuntimeException e) {
            WalksyLib.getLogger().err("Failed to deserialize value for option '" + option.getName() + "' as " + option.getType().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void preloadTexture(final Object value) {
        if (value instanceof final Identifier id) {
            CachedTextures.ensureLoaded(id);
        } else if (value instanceof final CroppedTexture cropped) {
            CachedTextures.ensureLoaded(cropped.texture());
        }
    }

    private static Object readValue(final Option<?> option, final JsonElement element) {
        if (option.getType() == List.class) {
            return readStringList(element);
        }
        return GSON.fromJson(element, option.getType());
    }

    private static List<String> readStringList(final JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        final JsonArray array = element.getAsJsonArray();
        final List<String> values = new ArrayList<>(array.size());
        for (final JsonElement item : array) {
            if (item == null || item.isJsonNull() || !item.isJsonPrimitive()) {
                continue;
            }
            values.add(item.getAsString());
        }
        return values;
    }

    private static void restoreSpriteTexture(final Option<?> option) {
        if (!(option.getValue() instanceof IdentifierWrapper wrapper)) {
            return;
        }
        final String fileName = wrapper.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        final Identifier identifier = SpriteCache.loadTexture(fileName);
        if (identifier != null) {
            wrapper.setIdentifier(identifier);
        }
    }

    private static boolean isPersistent(final Option<?> option) {
        if (option.getType() == Runnable.class || option.getType() == ModLinks.class || option.getType() == Preview.class) {
            return false;
        }
        if (option.getType() == WLKeyMapping.class) {
            return !((WLKeyMapping) option.getValue()).isVanilla();
        }
        return true;
    }

    private static JsonObject asObject(final JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
