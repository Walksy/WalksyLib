package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.utils.IdentifierWrapper;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;

public class IdentifierWrapperAdapter implements JsonSerializer<IdentifierWrapper>, JsonDeserializer<IdentifierWrapper> {

    @Override
    public JsonElement serialize(final IdentifierWrapper src, final Type typeOfSrc, final JsonSerializationContext extractor) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("id", src.getIdentifier().toString());
        obj.addProperty("fileName", src.getFileName());
        return obj;
    }

    @Override
    public IdentifierWrapper deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext extractor) throws JsonParseException {
        final JsonObject obj = json.getAsJsonObject();

        final Identifier id = Identifier.tryParse(obj.get("id").getAsString());
        final String fileName = obj.has("fileName") ? obj.get("fileName").getAsString() : "";

        return new IdentifierWrapper(id, fileName);
    }
}
