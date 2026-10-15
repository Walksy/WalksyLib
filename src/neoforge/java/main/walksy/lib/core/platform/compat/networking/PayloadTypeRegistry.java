package main.walksy.lib.core.platform.compat.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public final class PayloadTypeRegistry<B extends FriendlyByteBuf> {
    private static final PayloadTypeRegistry<RegistryFriendlyByteBuf> CLIENTBOUND = new PayloadTypeRegistry<>(true);
    private static final PayloadTypeRegistry<RegistryFriendlyByteBuf> SERVERBOUND = new PayloadTypeRegistry<>(false);

    public record Registration<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, boolean clientbound) {
    }

    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();

    private final boolean clientbound;

    private PayloadTypeRegistry(final boolean clientbound) {
        this.clientbound = clientbound;
    }

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> playS2C() {
        return CLIENTBOUND;
    }

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> playC2S() {
        return SERVERBOUND;
    }

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> clientboundPlay() {
        return CLIENTBOUND;
    }

    public static PayloadTypeRegistry<RegistryFriendlyByteBuf> serverboundPlay() {
        return SERVERBOUND;
    }

    public <T extends CustomPacketPayload> void register(final CustomPacketPayload.Type<T> type, final StreamCodec<? super B, T> codec) {
        synchronized (REGISTRATIONS) {
            REGISTRATIONS.add(new Registration<>(type, castCodec(codec), this.clientbound));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<? super RegistryFriendlyByteBuf, T> castCodec(final StreamCodec<?, T> codec) {
        return (StreamCodec<? super RegistryFriendlyByteBuf, T>) codec;
    }

    public static List<Registration<?>> registrations() {
        synchronized (REGISTRATIONS) {
            return List.copyOf(REGISTRATIONS);
        }
    }
}
