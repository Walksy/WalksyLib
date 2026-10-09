package main.walksy.lib.core.config.serialization.adapters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import main.walksy.lib.api.config.value.PixelGrid;

import java.lang.reflect.Type;

public final class PixelGridAdapter implements JsonSerializer<PixelGrid>, JsonDeserializer<PixelGrid> {
    @Override
    public JsonElement serialize(final PixelGrid grid, final Type type, final JsonSerializationContext jsonSerializationextractor) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("width", grid.getWidth());
        obj.addProperty("height", grid.getHeight());
        final JsonArray rows = new JsonArray();
        for (final int[] row : grid.getColors()) {
            final JsonArray jsonRow = new JsonArray();
            for (final int color : row) {
                jsonRow.add(color);
            }
            rows.add(jsonRow);
        }
        obj.add("pixels", rows);
        return obj;
    }

    @Override
    public PixelGrid deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationextractor) throws JsonParseException {
        final JsonObject obj = jsonElement.getAsJsonObject();
        final int width = obj.get("width").getAsInt();
        final int height = obj.get("height").getAsInt();
        final JsonArray rows = obj.getAsJsonArray("pixels");
        final int[][] colors = new int[height][width];
        for (int y = 0; y < rows.size() && y < height; y++) {
            final JsonArray row = rows.get(y).getAsJsonArray();
            for (int x = 0; x < row.size() && x < width; x++) {
                final JsonPrimitive cell = row.get(x).getAsJsonPrimitive();
                colors[y][x] = cell.isBoolean() ? (cell.getAsBoolean() ? PixelGrid.DEFAULT_COLOR : 0) : cell.getAsInt();
            }
        }
        return PixelGrid.migrate(width, height, colors);
    }
}
