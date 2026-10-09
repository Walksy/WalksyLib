package main.walksy.lib.api.sound;

import main.walksy.lib.core.WalksyLib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SoundAttributor<T> {
    private final Predicate<Holder<SoundEvent>> sounds;
    private final double radiusSq;
    private final int expireAfterTicks;
    private final BiPredicate<Player, T> candidateFilter;
    private final @Nullable Predicate<SoundHeard> intercept;
    private final @Nullable Predicate<Player> fallback;
    private final Consumer<Attribution<T>> onAttributed;
    private final Map<UUID, Entry<T>> entries = new HashMap<>();
    private int currentTick;

    private record Entry<T>(Vec3 position, int tick, @Nullable T data) {
    }

    public record Candidate<T>(Player player, Vec3 recordedPosition, int ageTicks, @Nullable T data) {
    }

    public record Attribution<T>(Player player, @Nullable T data, Holder<SoundEvent> sound, Vec3 soundPosition, double distanceSq, boolean fallback) {
    }

    public record SoundHeard(Holder<SoundEvent> sound, Vec3 position) {
        public @Nullable Player nearestPlayer(final Predicate<Player> filter) {
            return this.nearestPlayer(filter, Double.POSITIVE_INFINITY);
        }

        public @Nullable Player nearestPlayer(final Predicate<Player> filter, final double maxDistanceSq) {
            final ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            Player nearest = null;
            double nearestDistSq = maxDistanceSq;
            for (final Player player : level.players()) {
                final double distSq = player.position().distanceToSqr(this.position);
                if (distSq <= nearestDistSq && filter.test(player)) {
                    nearestDistSq = distSq;
                    nearest = player;
                }
            }
            return nearest;
        }
    }

    private SoundAttributor(final Builder<T> builder) {
        this.sounds = builder.sounds;
        this.radiusSq = builder.radius * builder.radius;
        this.expireAfterTicks = builder.expireAfterTicks;
        this.candidateFilter = builder.candidateFilter;
        this.intercept = builder.intercept;
        this.fallback = builder.fallback;
        this.onAttributed = builder.onAttributed;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public void record(final Player player, @Nullable final T data) {
        this.entries.put(player.getUUID(), new Entry<>(player.position(), this.currentTick, data));
    }

    public boolean isRecorded(@Nullable final Player player) {
        return player != null && this.entries.containsKey(player.getUUID());
    }

    public void forget(@Nullable final Player player) {
        if (player != null) {
            this.entries.remove(player.getUUID());
        }
    }

    public List<Candidate<T>> candidates() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        final List<Candidate<T>> out = new ArrayList<>(this.entries.size());
        for (final Map.Entry<UUID, Entry<T>> entry : this.entries.entrySet()) {
            final Player player = level.getPlayerByUUID(entry.getKey());
            if (player != null) {
                final Entry<T> value = entry.getValue();
                out.add(new Candidate<>(player, value.position(), this.currentTick - value.tick(), value.data()));
            }
        }
        return out;
    }

    public void tick() {
        this.currentTick++;
        this.entries.values().removeIf(this::isExpired);
    }

    public void clear() {
        this.entries.clear();
    }

    public void onSound(final Holder<SoundEvent> sound, final Vec3 position) {
        if (!this.sounds.test(sound)) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        final ClientLevel level = client.level;
        if (level == null) {
            return;
        }
        final SoundHeard heard = new SoundHeard(sound, position);
        if (this.intercept != null && this.intercept.test(heard)) {
            return;
        }
        UUID bestId = null;
        Player best = null;
        T bestData = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (final Map.Entry<UUID, Entry<T>> entry : this.entries.entrySet()) {
            final Entry<T> value = entry.getValue();
            if (this.isExpired(value)) {
                continue;
            }
            final Player player = level.getPlayerByUUID(entry.getKey());
            if (player == null || !this.candidateFilter.test(player, value.data())) {
                continue;
            }
            final double distSq = Math.min(player.position().distanceToSqr(position), value.position().distanceToSqr(position));
            if (distSq > this.radiusSq) {
                continue;
            }
            if (distSq < bestDistSq || (distSq == bestDistSq && player == client.player)) {
                bestId = entry.getKey();
                best = player;
                bestData = value.data();
                bestDistSq = distSq;
            }
        }
        if (best != null) {
            this.onAttributed.accept(new Attribution<>(best, bestData, sound, position, bestDistSq, false));
            this.entries.remove(bestId);
            return;
        }
        if (this.fallback == null) {
            return;
        }
        final Player nearest = heard.nearestPlayer(player -> true, this.radiusSq);
        if (nearest != null && this.fallback.test(nearest)) {
            this.onAttributed.accept(new Attribution<>(nearest, null, sound, position, nearest.position().distanceToSqr(position), true));
        }
    }

    private boolean isExpired(final Entry<T> entry) {
        return this.currentTick - entry.tick() > this.expireAfterTicks;
    }

    public static final class Builder<T> {
        private Predicate<Holder<SoundEvent>> sounds = sound -> false;
        private double radius = 6.0;
        private int expireAfterTicks = 20;
        private BiPredicate<Player, T> candidateFilter = (player, data) -> true;
        private @Nullable Predicate<SoundHeard> intercept;
        private @Nullable Predicate<Player> fallback;
        private Consumer<Attribution<T>> onAttributed = attribution -> { };

        private Builder() {
        }

        @SafeVarargs
        public final Builder<T> sounds(final Holder<SoundEvent>... sounds) {
            final Set<Holder<SoundEvent>> wanted = Set.of(sounds);
            this.sounds = wanted::contains;
            return this;
        }

        public Builder<T> sounds(final Predicate<Holder<SoundEvent>> matcher) {
            this.sounds = Objects.requireNonNull(matcher, "matcher");
            return this;
        }

        public Builder<T> radius(final double blocks) {
            this.radius = Math.max(0.0, blocks);
            return this;
        }

        public Builder<T> expireAfterTicks(final int ticks) {
            this.expireAfterTicks = Math.max(0, ticks);
            return this;
        }

        public Builder<T> candidateFilter(final BiPredicate<Player, T> filter) {
            this.candidateFilter = Objects.requireNonNull(filter, "filter");
            return this;
        }

        public Builder<T> intercept(final Predicate<SoundHeard> intercept) {
            this.intercept = intercept;
            return this;
        }

        public Builder<T> fallbackToNearest(final Predicate<Player> accept) {
            this.fallback = accept;
            return this;
        }

        public Builder<T> onAttributed(final Consumer<Attribution<T>> onAttributed) {
            this.onAttributed = Objects.requireNonNull(onAttributed, "onAttributed");
            return this;
        }

        public SoundAttributor<T> build() {
            return new SoundAttributor<>(this);
        }

        public SoundAttributor<T> register() {
            final SoundAttributor<T> attributor = this.build();
            WalksyLib.getInstance().getSoundAttributors().register(attributor);
            return attributor;
        }
    }
}
