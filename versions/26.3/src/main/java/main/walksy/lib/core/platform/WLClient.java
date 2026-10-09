package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLMisc;

import java.util.ArrayList;
import java.util.List;

public final class WLClient {
    private WLClient() {
    }

    public static @Nullable Screen screen() {
        return Minecraft.getInstance().gui.screen();
    }

    public static void setScreen(@Nullable final Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static @Nullable Overlay overlay() {
        return Minecraft.getInstance().gui.overlay();
    }

    public static void schedule(final Runnable task) {
        Minecraft.getInstance().schedule(task);
    }

    public static void openUri(final String uri) {
        if (uri == null || uri.isBlank()) {
            return;
        }
        SDLMisc.SDL_OpenURL(uri);
    }

    public static List<String> onlinePlayerNames() {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        final List<String> names = new ArrayList<>();
        for (final PlayerInfo info : connection.getOnlinePlayers()) {
            names.add(info.getProfile().name());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
