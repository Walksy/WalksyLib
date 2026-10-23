package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WLClient {
    private WLClient() {
    }

    public static @Nullable Screen screen() {
        return Minecraft.getInstance().screen;
    }

    public static void setScreen(@Nullable final Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    public static @Nullable Overlay overlay() {
        return Minecraft.getInstance().getOverlay();
    }

    public static int refreshRate() {
        final int rate = Minecraft.getInstance().getWindow().getRefreshRate();
        return rate > 0 ? rate : 60;
    }

    public static void schedule(final Runnable task) {
        Minecraft.getInstance().schedule(task);
    }

    public static void openUri(final String uri) {
        if (uri == null || uri.isBlank()) {
            return;
        }
        Util.getPlatform().openUri(uri);
    }

    public static List<String> onlinePlayerNames() {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        final List<String> names = new ArrayList<>();
        for (final PlayerInfo info : connection.getOnlinePlayers()) {
            names.add(info.getProfile().getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
