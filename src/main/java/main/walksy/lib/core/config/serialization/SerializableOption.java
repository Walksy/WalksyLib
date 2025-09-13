package main.walksy.lib.core.config.serialization;

import com.google.gson.JsonElement;
import main.walksy.lib.core.config.local.Option;

public class SerializableOption {
    public String name;
    public String type;
    public JsonElement value;

    public JsonElement min;
    public JsonElement max;
    public JsonElement increment;
}
