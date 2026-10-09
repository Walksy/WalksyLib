package main.walksy.lib.core.platform;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class WLNetworking {
    private WLNetworking() {
    }

    public static <T extends CustomPacketPayload> void registerClientbound(final CustomPacketPayload.Type<T> type, final StreamCodec<FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    public static <T extends CustomPacketPayload> void registerServerbound(final CustomPacketPayload.Type<T> type, final StreamCodec<FriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
    }
}
