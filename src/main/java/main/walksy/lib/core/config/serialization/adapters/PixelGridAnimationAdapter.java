package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PixelGridAnimationAdapter implements JsonSerializer<PixelGridAnimation>, JsonDeserializer<PixelGridAnimation> {

    @Override
    public JsonElement serialize(final PixelGridAnimation src, final Type typeOfSrc, final JsonSerializationContext extractor) {
        final JsonObject obj = new JsonObject();
        final JsonArray frames = new JsonArray();

        for (final PixelGrid frame : src.getFrames()) {
            frames.add(extractor.serialize(frame));
        }

        obj.add("frames", frames);
        obj.addProperty("animationSpeed", src.getAnimationSpeed());
        obj.addProperty("size", src.getSize());

        final JsonObject pos = new JsonObject();
        pos.addProperty("x", src.getOffsetX());
        pos.addProperty("y", src.getOffsetY());
        obj.add("position", pos);

        return obj;
    }

    @Override
    public PixelGridAnimation deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext extractor) throws JsonParseException {
        final JsonObject obj = json.getAsJsonObject();
        final JsonArray frameArray = obj.getAsJsonArray("frames");

        final List<PixelGrid> frames = new ArrayList<>();
        for (final JsonElement elem : frameArray) {
            frames.add(extractor.deserialize(elem, PixelGrid.class));
        }

        final PixelGridAnimation animation = new PixelGridAnimation(frames);

        if (obj.has("animationSpeed")) {
            animation.setAnimationSpeed(obj.get("animationSpeed").getAsInt());
        }

        if (obj.has("size")) {
            animation.setSize(obj.get("size").getAsFloat());
        }

        if (obj.has("position")) {
            final JsonObject posObj = obj.getAsJsonObject("position");
            animation.setOffset(posObj.get("x").getAsDouble(), posObj.get("y").getAsDouble());
        }

        return animation;
    }
}
