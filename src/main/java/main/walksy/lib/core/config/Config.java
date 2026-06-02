package main.walksy.lib.core.config;

import java.nio.file.Path;

public interface Config {
    void onSave();
    void onLoad();
    Path getPath();
}
