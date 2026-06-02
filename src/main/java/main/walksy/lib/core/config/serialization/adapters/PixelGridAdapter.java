package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.*;
import main.walksy.lib.core.config.local.options.type.PixelGrid;

import java.lang.reflect.Type;

public class PixelGridAdapter implements JsonSerializer<PixelGrid>, JsonDeserializer<PixelGrid> {

    @Override
    public JsonElement serialize(final PixelGrid grid, final Type type, final JsonSerializationContext jsonSerializationContext) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("width", grid.getWidth());
        obj.addProperty("height", grid.getHeight());

        final JsonArray rows = new JsonArray();
        for (final boolean[] row : grid.getPixels()) {
            final JsonArray jsonRow = new JsonArray();
            for (final boolean pixel : row) {
                jsonRow.add(pixel);
            }
            rows.add(jsonRow);
        }
        obj.add("pixels", rows);
        return obj;
    }

    @Override
    public PixelGrid deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonObject obj = jsonElement.getAsJsonObject();
        final int width = obj.get("width").getAsInt();
        final int height = obj.get("height").getAsInt();

        final JsonArray rows = obj.getAsJsonArray("pixels");
        final boolean[][] pixels = new boolean[height][width];
        for (int y = 0; y < rows.size(); y++) {
            final JsonArray row = rows.get(y).getAsJsonArray();
            for (int x = 0; x < row.size(); x++) {
                pixels[y][x] = row.get(x).getAsBoolean();
            }
        }

        return new PixelGrid(width, height, pixels);
    }
}
