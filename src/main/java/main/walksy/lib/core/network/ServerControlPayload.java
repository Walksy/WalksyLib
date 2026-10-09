package main.walksy.lib.core.network;

import io.netty.buffer.Unpooled;
import main.walksy.lib.core.WalksyLibConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ServerControlPayload(boolean readable, List<Entry> entries, List<Lifted> lifted) implements CustomPacketPayload {
    public static final int PROTOCOL = 4;
    public static final Type<ServerControlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, "control"));
    public static final StreamCodec<FriendlyByteBuf, ServerControlPayload> CODEC = StreamCodec.ofMember(ServerControlPayload::write, ServerControlPayload::read);
    static final int MAX_ENTRIES = 512;
    static final int MAX_ID_LENGTH = 128;
    static final int MAX_REASON_LENGTH = 512;
    private static final ServerControlPayload UNREADABLE = new ServerControlPayload(false, List.of(), List.of());

    public ServerControlPayload {
        entries = List.copyOf(entries);
        lifted = List.copyOf(lifted);
    }

    public record Entry(String modId, boolean disableMod, String reason, List<Feature> features) {

        public Entry {
            features = List.copyOf(features);
        }
    }

    public record Feature(String id, String reason) {
    }

    public record Lifted(String modId, String feature, String reason) {
    }

    @Override
    public Type<ServerControlPayload> type() {
        return TYPE;
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(PROTOCOL);
        buf.writeVarInt(this.entries.size());
        for (final Entry entry : this.entries) {
            writeRecord(buf, body -> {
                body.writeUtf(entry.modId());
                body.writeBoolean(entry.disableMod());
                body.writeUtf(entry.reason());
                body.writeVarInt(entry.features().size());
                for (final Feature feature : entry.features()) {
                    writeRecord(body, inner -> {
                        inner.writeUtf(feature.id());
                        inner.writeUtf(feature.reason());
                    });
                }
            });
        }
        buf.writeVarInt(this.lifted.size());
        for (final Lifted lifted : this.lifted) {
            writeRecord(buf, body -> {
                body.writeUtf(lifted.modId());
                body.writeUtf(lifted.feature());
                body.writeUtf(lifted.reason());
            });
        }
    }

    private static ServerControlPayload read(final FriendlyByteBuf wire) {
        final byte[] bytes = new byte[wire.readableBytes()];
        wire.readBytes(bytes);
        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            if (buf.readVarInt() != PROTOCOL) {
                return UNREADABLE;
            }
            final int count = boundedCount(buf);
            final List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                final FriendlyByteBuf mod = readRecord(buf);
                final String modId = mod.readUtf(MAX_ID_LENGTH);
                final boolean disableMod = mod.readBoolean();
                final String reason = mod.readUtf(MAX_REASON_LENGTH);
                final int featureCount = boundedCount(mod);
                final List<Feature> features = new ArrayList<>(featureCount);
                for (int j = 0; j < featureCount; j++) {
                    final FriendlyByteBuf feature = readRecord(mod);
                    features.add(new Feature(feature.readUtf(MAX_ID_LENGTH), feature.readUtf(MAX_REASON_LENGTH)));
                }
                entries.add(new Entry(modId, disableMod, reason, features));
            }
            final List<Lifted> lifted = new ArrayList<>();
            if (buf.isReadable()) {
                final int liftedCount = boundedCount(buf);
                for (int i = 0; i < liftedCount; i++) {
                    final FriendlyByteBuf entry = readRecord(buf);
                    lifted.add(new Lifted(entry.readUtf(MAX_ID_LENGTH), entry.readUtf(MAX_ID_LENGTH), entry.readUtf(MAX_REASON_LENGTH)));
                }
            }
            return new ServerControlPayload(true, entries, lifted);
        } catch (final RuntimeException e) {
            return UNREADABLE;
        } finally {
            buf.release();
        }
    }

    static int boundedCount(final FriendlyByteBuf buf) {
        final int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("count " + count + " out of range");
        }
        return count;
    }

    static FriendlyByteBuf readRecord(final FriendlyByteBuf buf) {
        final int length = buf.readVarInt();
        if (length < 0 || length > buf.readableBytes()) {
            throw new IllegalArgumentException("record length " + length + " out of range");
        }
        return new FriendlyByteBuf(buf.readSlice(length));
    }

    static void writeRecord(final FriendlyByteBuf buf, final Consumer<FriendlyByteBuf> body) {
        final FriendlyByteBuf inner = new FriendlyByteBuf(Unpooled.buffer());
        try {
            body.accept(inner);
            buf.writeVarInt(inner.readableBytes());
            buf.writeBytes(inner);
        } finally {
            inner.release();
        }
    }
}
