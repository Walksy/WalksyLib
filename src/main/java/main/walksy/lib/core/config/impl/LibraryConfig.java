package main.walksy.lib.core.config.impl;

import main.walksy.lib.core.config.Config;

import java.nio.file.Path;

public class LibraryConfig implements Config {

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public void onLoad() {}

    @Override
    public void onSave() {}
}
