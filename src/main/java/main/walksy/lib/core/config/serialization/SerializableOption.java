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

    public boolean rainbow;
    public float hue;
    public float saturation;
    public float brightness;
    public int alpha;
    public int rainbowSpeed;
    public int pulseSpeed;
    public boolean pulse;
}
