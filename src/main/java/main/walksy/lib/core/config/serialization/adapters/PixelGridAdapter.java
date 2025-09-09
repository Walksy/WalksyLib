package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.config.local.options.type.PixelGrid;

import java.lang.reflect.Type;

public class PixelGridAdapter implements JsonSerializer<PixelGrid>, JsonDeserializer<PixelGrid> {

    @Override
    public JsonElement serialize(PixelGrid grid, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("width", grid.getWidth());
        obj.addProperty("height", grid.getHeight());

        JsonArray rows = new JsonArray();
        for (boolean[] row : grid.getPixels()) {
            JsonArray jsonRow = new JsonArray();
            for (boolean pixel : row) {
                jsonRow.add(pixel);
            }
            rows.add(jsonRow);
        }
        obj.add("pixels", rows);
        return obj;
    }

    @Override
    public PixelGrid deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int width = obj.get("width").getAsInt();
        int height = obj.get("height").getAsInt();

        JsonArray rows = obj.getAsJsonArray("pixels");
        boolean[][] pixels = new boolean[height][width];
        for (int y = 0; y < rows.size(); y++) {
            JsonArray row = rows.get(y).getAsJsonArray();
            for (int x = 0; x < row.size(); x++) {
                pixels[y][x] = row.get(x).getAsBoolean();
            }
        }

        return new PixelGrid(width, height, pixels);
    }
}

