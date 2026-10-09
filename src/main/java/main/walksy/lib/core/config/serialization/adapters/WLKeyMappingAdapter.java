package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.blaze3d.platform.InputConstants;
import main.walksy.lib.api.config.value.WLKeyMapping;

import java.lang.reflect.Type;

public final class WLKeyMappingAdapter implements JsonSerializer<WLKeyMapping>, JsonDeserializer<WLKeyMapping> {
    @Override
    public JsonElement serialize(final WLKeyMapping src, final Type typeOfSrc, final JsonSerializationContext context) {
        final InputConstants.Key key = src.currentKey();
        final JsonObject object = new JsonObject();
        object.addProperty("type", key.getType().name());
        object.addProperty("value", key.getValue());
        return object;
    }

    @Override
    public WLKeyMapping deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject object = json.getAsJsonObject();
        final InputConstants.Type type = InputConstants.Type.valueOf(object.get("type").getAsString());
        final int value = object.get("value").getAsInt();
        return WLKeyMapping.detached(type.getOrCreate(value));
    }
}
