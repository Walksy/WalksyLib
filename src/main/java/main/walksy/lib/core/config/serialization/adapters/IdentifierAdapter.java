package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;

public final class IdentifierAdapter implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
    @Override
    public JsonElement serialize(final Identifier src, final Type typeOfSrc, final JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Identifier deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        return Identifier.tryParse(json.getAsString());
    }
}
