package main.walksy.lib.core.network;

import main.walksy.lib.core.WalksyLibConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ServerHelloPayload(List<Entry> mods) implements CustomPacketPayload {
    public static final Type<ServerHelloPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, "hello"));
    public static final StreamCodec<FriendlyByteBuf, ServerHelloPayload> CODEC = StreamCodec.ofMember(ServerHelloPayload::write, ServerHelloPayload::read);

    public ServerHelloPayload {
        mods = List.copyOf(mods);
    }

    public record Entry(String modId, String displayName, boolean wholeMod, List<String> features) {

        public Entry {
            features = List.copyOf(features);
        }
    }

    @Override
    public Type<ServerHelloPayload> type() {
        return TYPE;
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(ServerControlPayload.PROTOCOL);
        buf.writeVarInt(this.mods.size());
        for (final Entry mod : this.mods) {
            ServerControlPayload.writeRecord(buf, body -> {
                body.writeUtf(mod.modId());
                body.writeUtf(mod.displayName());
                body.writeBoolean(mod.wholeMod());
                body.writeVarInt(mod.features().size());
                for (final String feature : mod.features()) {
                    ServerControlPayload.writeRecord(body, inner -> inner.writeUtf(feature));
                }
            });
        }
    }

    private static ServerHelloPayload read(final FriendlyByteBuf buf) {
        buf.readVarInt();
        final int count = ServerControlPayload.boundedCount(buf);
        final List<Entry> mods = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final FriendlyByteBuf mod = ServerControlPayload.readRecord(buf);
            final String modId = mod.readUtf(ServerControlPayload.MAX_ID_LENGTH);
            final String displayName = mod.readUtf(ServerControlPayload.MAX_REASON_LENGTH);
            final boolean wholeMod = mod.readBoolean();
            final int featureCount = ServerControlPayload.boundedCount(mod);
            final List<String> features = new ArrayList<>(featureCount);
            for (int j = 0; j < featureCount; j++) {
                features.add(ServerControlPayload.readRecord(mod).readUtf(ServerControlPayload.MAX_ID_LENGTH));
            }
            mods.add(new Entry(modId, displayName, wholeMod, features));
        }
        buf.skipBytes(buf.readableBytes());
        return new ServerHelloPayload(mods);
    }
}
