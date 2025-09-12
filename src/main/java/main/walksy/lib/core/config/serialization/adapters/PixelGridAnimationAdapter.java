package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;

import java.awt.*;
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
        obj.addProperty("animationSpeed", src.getAnimationSpeed());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", src.getRelativeX());
        pos.addProperty("y", src.getRelativeY());
        obj.add("position", pos);

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

        PixelGridAnimation animation = new PixelGridAnimation(frames);

        if (obj.has("animationSpeed")) {
            int speed = obj.get("animationSpeed").getAsInt();
            animation.setAnimationSpeed(speed);
        }

        JsonObject posObj = obj.getAsJsonObject("position");
        float x = posObj.get("x").getAsFloat();
        float y = posObj.get("y").getAsFloat();
        animation.setRelativePosition(x, y);

        return animation;
    }
}
