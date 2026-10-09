package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import main.walksy.lib.api.config.value.CroppedTexture;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;

public final class CroppedTextureAdapter implements JsonSerializer<CroppedTexture>, JsonDeserializer<CroppedTexture> {
    private static final String KEY_TEXTURE = "texture";
    private static final String KEY_CROP = "crop";

    @Override
    public JsonElement serialize(final CroppedTexture src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        if (src.texture() != null) {
            object.addProperty(KEY_TEXTURE, src.texture().toString());
        }
        final JsonArray crop = new JsonArray();
        crop.add(src.u0());
        crop.add(src.v0());
        crop.add(src.u1());
        crop.add(src.v1());
        object.add(KEY_CROP, crop);
        return object;
    }

    @Override
    public CroppedTexture deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Cropped texture must be an object");
        }
        final JsonObject object = json.getAsJsonObject();
        final Identifier texture = object.has(KEY_TEXTURE) && !object.get(KEY_TEXTURE).isJsonNull() ? Identifier.tryParse(object.get(KEY_TEXTURE).getAsString()) : null;
        final JsonElement crop = object.get(KEY_CROP);
        if (crop == null || !crop.isJsonArray() || crop.getAsJsonArray().size() != 4) {
            return CroppedTexture.of(texture);
        }
        final JsonArray values = crop.getAsJsonArray();
        return new CroppedTexture(texture, values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat(), values.get(3).getAsFloat());
    }
}
