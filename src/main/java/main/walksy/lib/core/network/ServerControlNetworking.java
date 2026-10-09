package main.walksy.lib.core.network;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.platform.WLNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ServerControlNetworking {
    private final ServerOptOuts optOuts;
    private boolean helloSent;
    private boolean warnedUnreadable;

    public ServerControlNetworking(final ServerOptOuts optOuts) {
        this.optOuts = optOuts;
    }

    public void register() {
        if (!this.optOuts.any()) {
            return;
        }
        WLNetworking.registerClientbound(ServerControlPayload.TYPE, ServerControlPayload.CODEC);
        WLNetworking.registerServerbound(ServerHelloPayload.TYPE, ServerHelloPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ServerControlPayload.TYPE, this::receive);
    }

    public void tick() {
        this.optOuts.tick();
    }

    public void onJoinedServer() {
        this.reset();
    }

    public void onLeftServer() {
        this.reset();
    }

    private void receive(final ServerControlPayload payload, final ClientPlayNetworking.Context context) {
        if (!this.helloSent) {
            this.helloSent = true;
            context.responseSender().sendPacket(this.optOuts.hello());
        }
        if (!payload.readable()) {
            if (!this.warnedUnreadable) {
                this.warnedUnreadable = true;
                WalksyLib.getLogger().warn("The server sent a walksylib:control message this version cannot " + "read (newer protocol, or malformed)");
            }
            return;
        }
        this.optOuts.apply(payload);
    }

    private void reset() {
        this.helloSent = false;
        this.warnedUnreadable = false;
        this.optOuts.reset();
    }
}
