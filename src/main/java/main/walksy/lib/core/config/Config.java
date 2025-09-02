package main.walksy.lib.core.config;

import java.nio.file.Path;

public interface Config {
    void save();
    void load();
    Path path();
}
