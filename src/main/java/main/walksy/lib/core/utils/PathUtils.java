package main.walksy.lib.core.utils;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PathUtils {

    public static Path ofConfigDir(String fileName)
    {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName + ".json");
    }
}
