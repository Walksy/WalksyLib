package main.walksy.lib.core.platform.compat.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayNetworking {
    private static final Map<Object, PlayPayloadHandler<?>> RECEIVERS = new ConcurrentHashMap<>();

    private ClientPlayNetworking() {
    }

    @FunctionalInterface
    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface PacketSender {
        void sendPacket(CustomPacketPayload payload);
    }

    public interface Context {
        Minecraft client();

        LocalPlayer player();

        PacketSender responseSender();
    }

    public static <T extends CustomPacketPayload> boolean registerGlobalReceiver(final CustomPacketPayload.Type<T> type, final PlayPayloadHandler<T> handler) {
        return RECEIVERS.putIfAbsent(type.id(), handler) == null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void dispatch(final T payload) {
        final PlayPayloadHandler<T> handler = (PlayPayloadHandler<T>) RECEIVERS.get(payload.type().id());
        if (handler == null) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        handler.receive(payload, new Context() {
            @Override
            public Minecraft client() {
                return client;
            }

            @Override
            public LocalPlayer player() {
                return client.player;
            }

            @Override
            public PacketSender responseSender() {
                return ClientPlayNetworking::send;
            }
        });
    }

    public static void send(final CustomPacketPayload payload) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
