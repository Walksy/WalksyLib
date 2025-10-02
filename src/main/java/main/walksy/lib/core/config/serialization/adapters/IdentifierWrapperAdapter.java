package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.utils.IdentifierWrapper;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class IdentifierWrapperAdapter implements JsonSerializer<IdentifierWrapper>, JsonDeserializer<IdentifierWrapper> {

    @Override
    public JsonElement serialize(IdentifierWrapper src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", src.getIdentifier().toString());
        obj.addProperty("fileName", src.getFileName());
        return obj;
    }

    @Override
    public IdentifierWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        Identifier id = Identifier.tryParse(obj.get("id").getAsString());
        String fileName = obj.has("fileName") ? obj.get("fileName").getAsString() : "";

        return new IdentifierWrapper(id, fileName);
    }
}
