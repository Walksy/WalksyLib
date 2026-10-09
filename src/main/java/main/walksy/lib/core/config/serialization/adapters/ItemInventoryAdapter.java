package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import main.walksy.lib.api.config.value.ItemInventory;
import main.walksy.lib.api.config.value.ItemSpec;

import java.lang.reflect.Type;
import java.util.Map;

public final class ItemInventoryAdapter implements JsonSerializer<ItemInventory>, JsonDeserializer<ItemInventory> {
    private static final String KEY_SLOTS = "slots";
    private static final String KEY_ID = "id";
    private static final String KEY_COUNT = "count";
    private static final String KEY_DAMAGE = "damage";
    private static final String KEY_UNBREAKABLE = "unbreakable";
    private static final String KEY_NAME = "name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_ENCHANTMENTS = "enchantments";
    private static final String KEY_NBT = "nbt";

    @Override
    public JsonElement serialize(final ItemInventory src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject root = new JsonObject();
        final JsonObject slots = new JsonObject();
        for (int slot = 0; slot < ItemInventory.SIZE; slot++) {
            final ItemSpec spec = src.get(slot);
            if (spec != null) {
                slots.add(String.valueOf(slot), serializeSpec(spec));
            }
        }
        root.add(KEY_SLOTS, slots);
        return root;
    }

    private static JsonObject serializeSpec(final ItemSpec spec) {
        final JsonObject obj = new JsonObject();
        obj.addProperty(KEY_ID, spec.getItemId());
        if (spec.getCount() != 1) {
            obj.addProperty(KEY_COUNT, spec.getCount());
        }
        if (spec.getDamage() != 0) {
            obj.addProperty(KEY_DAMAGE, spec.getDamage());
        }
        if (spec.isUnbreakable()) {
            obj.addProperty(KEY_UNBREAKABLE, true);
        }
        if (spec.hasCustomName()) {
            obj.addProperty(KEY_NAME, spec.getCustomName());
        }
        if (!spec.getLore().isEmpty()) {
            final JsonArray lore = new JsonArray();
            for (final String line : spec.getLore()) {
                lore.add(line);
            }
            obj.add(KEY_LORE, lore);
        }
        if (!spec.getEnchantments().isEmpty()) {
            final JsonObject enchantments = new JsonObject();
            for (final Map.Entry<String, Integer> entry : spec.getEnchantments().entrySet()) {
                enchantments.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add(KEY_ENCHANTMENTS, enchantments);
        }
        if (spec.hasExtraNbt()) {
            obj.addProperty(KEY_NBT, spec.getExtraNbt());
        }
        return obj;
    }

    @Override
    public ItemInventory deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final ItemInventory inventory = new ItemInventory();
        if (json == null || !json.isJsonObject()) {
            return inventory;
        }
        final JsonElement slots = json.getAsJsonObject().get(KEY_SLOTS);
        if (slots == null || !slots.isJsonObject()) {
            return inventory;
        }
        for (final Map.Entry<String, JsonElement> entry : slots.getAsJsonObject().entrySet()) {
            final int slot = parseSlot(entry.getKey());
            if (!ItemInventory.isValidSlot(slot) || !entry.getValue().isJsonObject()) {
                continue;
            }
            final ItemSpec spec = deserializeSpec(entry.getValue().getAsJsonObject());
            if (spec != null) {
                inventory.set(slot, spec);
            }
        }
        return inventory;
    }

    private static int parseSlot(final String key) {
        try {
            return Integer.parseInt(key.trim());
        } catch (final NumberFormatException notASlot) {
            return -1;
        }
    }

    private static ItemSpec deserializeSpec(final JsonObject obj) {
        final JsonElement id = obj.get(KEY_ID);
        if (id == null || !id.isJsonPrimitive()) {
            return null;
        }
        final ItemSpec spec = new ItemSpec(id.getAsString(), readInt(obj, KEY_COUNT, 1));
        spec.setDamage(readInt(obj, KEY_DAMAGE, 0));
        spec.setUnbreakable(readBoolean(obj, KEY_UNBREAKABLE));
        final JsonElement name = obj.get(KEY_NAME);
        if (name != null && name.isJsonPrimitive()) {
            spec.setCustomName(name.getAsString());
        }
        final JsonElement lore = obj.get(KEY_LORE);
        if (lore != null && lore.isJsonArray()) {
            for (final JsonElement line : lore.getAsJsonArray()) {
                if (line != null && line.isJsonPrimitive()) {
                    spec.addLoreLine(line.getAsString());
                }
            }
        }
        final JsonElement enchantments = obj.get(KEY_ENCHANTMENTS);
        if (enchantments != null && enchantments.isJsonObject()) {
            for (final Map.Entry<String, JsonElement> entry : enchantments.getAsJsonObject().entrySet()) {
                if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                    spec.setEnchantment(entry.getKey(), readLevel(entry.getValue()));
                }
            }
        }
        final JsonElement nbt = obj.get(KEY_NBT);
        if (nbt != null && nbt.isJsonPrimitive()) {
            spec.setExtraNbt(nbt.getAsString());
        }
        return spec;
    }

    private static int readInt(final JsonObject obj, final String key, final int fallback) {
        final JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (final NumberFormatException notANumber) {
            return fallback;
        }
    }

    private static int readLevel(final JsonElement element) {
        try {
            return element.getAsInt();
        } catch (final NumberFormatException notANumber) {
            return 1;
        }
    }

    private static boolean readBoolean(final JsonObject obj, final String key) {
        final JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }
}
