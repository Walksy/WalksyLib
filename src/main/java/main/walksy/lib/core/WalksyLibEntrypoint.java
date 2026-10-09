package main.walksy.lib.core;

import net.fabricmc.api.ClientModInitializer;

public final class WalksyLibEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WalksyLib.getInstance().onInitializeClient();
    }
}
