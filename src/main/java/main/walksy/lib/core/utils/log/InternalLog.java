package main.walksy.lib.core.utils.log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class InternalLog {

    public int revision;
    public String time;
    public InternalLog()
    {
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
