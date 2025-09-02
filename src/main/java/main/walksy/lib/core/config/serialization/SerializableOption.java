package main.walksy.lib.core.config.serialization;

import main.walksy.lib.core.config.local.Option;

public class SerializableOption {
    public String name;
    public String type;
    public Object value;

    public Object min;
    public Object max;
    public Object increment;

    public boolean rainbow;
    public float hue;
    public float saturation;
    public float brightness;
    public int alpha;
    public int rainbowSpeed;
    public int pulseSpeed;
    public Option.PulseValue pulseValue;
}
