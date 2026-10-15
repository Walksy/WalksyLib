package main.walksy.lib.core.platform;

public final class WLLoader {
    private static final boolean NEOFORGE = detectNeoForge();

    private WLLoader() {
    }

    private static boolean detectNeoForge() {
        try {
            Class.forName("net.neoforged.fml.ModList", false, WLLoader.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static boolean isNeoForge() {
        return NEOFORGE;
    }

    public static String modrinthLoader() {
        return NEOFORGE ? "neoforge" : "fabric";
    }
}
