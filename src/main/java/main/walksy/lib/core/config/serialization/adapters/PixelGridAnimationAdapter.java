package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PixelGridAnimationAdapter implements JsonSerializer<PixelGridAnimation>, JsonDeserializer<PixelGridAnimation> {

    @Override
    public JsonElement serialize(PixelGridAnimation src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        JsonArray frames = new JsonArray();
        for (PixelGrid frame : src.getFrames()) {
            frames.add(context.serialize(frame));
        }

        obj.add("frames", frames);
        return obj;
    }

    @Override
    public PixelGridAnimation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonArray frameArray = obj.getAsJsonArray("frames");

        List<PixelGrid> frames = new ArrayList<>();
        for (JsonElement elem : frameArray) {
            frames.add(context.deserialize(elem, PixelGrid.class));
        }

        return new PixelGridAnimation(frames);
    }
}

