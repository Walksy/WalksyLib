package main.walksy.lib.core.platform.compat.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public interface FabricClientCommandSource {
    void sendFeedback(Component message);

    void sendError(Component error);

    Minecraft getClient();

    LocalPlayer getPlayer();
}
