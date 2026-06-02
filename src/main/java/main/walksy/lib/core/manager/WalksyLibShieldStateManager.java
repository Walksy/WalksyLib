package main.walksy.lib.core.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WalksyLibShieldStateManager {
    private static final long ATTACK_ENTRY_TTL_MS = 1000;
    private static final byte SHIELD_DISABLE_STATUS = 30;

    private final Map<Player, Integer> shieldUseTicks;
    private final Map<Player, AttackEntry> attackedPlayerEntries;
    private final ShieldCooldownManager cooldownManager;
    private final HashMap<String, Consumer<Player>> onDisableEvents;
    private int localShieldCooldownTicks;
    private int currentTick;
    private PendingBreak pendingBreak;

    public WalksyLibShieldStateManager() {
        this.shieldUseTicks = new HashMap<>();
        this.attackedPlayerEntries = new HashMap<>();
        this.cooldownManager = new ShieldCooldownManager();
        this.onDisableEvents = new HashMap<>();
        this.localShieldCooldownTicks = 0;
        this.currentTick = 0;
        this.pendingBreak = null;
    }

    public void disable(final Player player) {
        if (player == null) {
            return;
        }
        this.cooldownManager.setCooldown(player);
        if (this.attackedPlayerEntries.containsKey(player)) {
            this.onDisableEvents.forEach((_, callback) -> callback.accept(player));
        }
    }

    public void addDisableCallback(final String id, final Consumer<Player> callback) {
        this.onDisableEvents.put(id, callback);
    }

    public void removeDisableCallback(final String id) {
        this.onDisableEvents.remove(id);
    }

    public void tick() {
        final long now = System.currentTimeMillis();
        final Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        this.currentTick++;
        for (final Player player : client.level.players()) {
            if (this.isHoldingUsableShield(player) && player.isUsingItem()) {
                final int current = this.shieldUseTicks.getOrDefault(player, 0);
                this.shieldUseTicks.put(player, current + 1);
            } else {
                this.shieldUseTicks.put(player, 0);
            }
        }
        if (client.player != null && client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
            this.localShieldCooldownTicks++;
        } else {
            this.localShieldCooldownTicks = 0;
        }
        if (this.pendingBreak != null && this.currentTick - this.pendingBreak.createdTick() >= 1) {
            if (client.player == null || !client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
                this.disable(this.pendingBreak.target());
            }
            this.pendingBreak = null;
        }
        this.attackedPlayerEntries.entrySet().removeIf(e -> now - e.getValue().time > ATTACK_ENTRY_TTL_MS);
        this.cooldownManager.tick();
    }

    public void handleSoundPacket(final double x, final double y, final double z) {
        final Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        final LocalPlayer local = client.player;
        final long now = System.currentTimeMillis();
        if (this.localShieldCooldownTicks > 2) {
            this.disable(this.nearestPlayerToSound(x, y, z, local));
            return;
        }
        final double matchRadiusSq = 36.0;
        Player best = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (final Map.Entry<Player, AttackEntry> e : this.attackedPlayerEntries.entrySet()) {
            final Player candidate = e.getKey();
            final AttackEntry ae = e.getValue();
            if (candidate == null || candidate == local) continue;
            if (now - ae.time > ATTACK_ENTRY_TTL_MS) continue;
            if (!ae.wasBlocking) continue;

            final double dx = candidate.getX() - x;
            final double dy = candidate.getY() - y;
            final double dz = candidate.getZ() - z;
            final double currentDistSq = dx * dx + dy * dy + dz * dz;

            final double sdx = ae.targetPos.x - x;
            final double sdy = ae.targetPos.y - y;
            final double sdz = ae.targetPos.z - z;
            final double storedDistSq = sdx * sdx + sdy * sdy + sdz * sdz;

            final double effectiveDistSq = Math.min(currentDistSq, storedDistSq);
            if (effectiveDistSq > matchRadiusSq) continue;

            if (effectiveDistSq < bestDistSq) {
                bestDistSq = effectiveDistSq;
                best = candidate;
            }
        }
        final double selfDx = local.getX() - x;
        final double selfDy = local.getY() - y;
        final double selfDz = local.getZ() - z;
        final double selfDistSq = selfDx * selfDx + selfDy * selfDy + selfDz * selfDz;
        if (best != null && bestDistSq < selfDistSq) {
            this.disable(best);
            this.attackedPlayerEntries.remove(best);
            return;
        }
        if (best == null) {
            return;
        }
        this.attackedPlayerEntries.remove(best);
        this.pendingBreak = new PendingBreak(best, this.currentTick);
    }

    private Player nearestPlayerToSound(final double x, final double y, final double z, final LocalPlayer local) {
        final Minecraft client = Minecraft.getInstance();
        Player nearest = null;
        double nearestDistSq = Double.POSITIVE_INFINITY;
        for (final Player player : client.level.players()) {
            if (player == local) continue;
            final double dx = player.getX() - x;
            final double dy = player.getY() - y;
            final double dz = player.getZ() - z;
            final double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    public void handleEntityStatus(final Player player, final byte status) {
        final Minecraft client = Minecraft.getInstance();
        if (status == SHIELD_DISABLE_STATUS && player != client.player) {
            this.disable(player);
        }
    }

    public void handlePlayerAttack(final Player target) {
        final Minecraft client = Minecraft.getInstance();
        final boolean estBlocking = this.shieldUseTicks.getOrDefault(target, 0) >= 3;
        if (client.player == null) return;
        if (this.disablesShield(client.player)) {
            this.attackedPlayerEntries.put(
                    target,
                    new AttackEntry(
                            client.player.position(),
                            target.position(),
                            System.currentTimeMillis(),
                            estBlocking
                    )
            );
        }
    }

    public boolean isCoolingDown(final Player player) {
        final Minecraft client = Minecraft.getInstance();
        if (player == client.player) {
            return client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
        }
        return this.cooldownManager.isCoolingDown(player);
    }

    public float getCooldownProgress(final Player player) {
        if (player == null) {
            return 0.0f;
        }
        final Minecraft client = Minecraft.getInstance();
        if (player == client.player) {
            return client.player.getCooldowns().getCooldownPercent(new ItemStack(Items.SHIELD), 0);
        }
        final int remaining = this.cooldownManager.getRemainingTicks(player);
        if (remaining <= 0) return 0.0f;
        final float frac = remaining / (float) 100;
        return Math.max(0f, Math.min(1f, frac));
    }

    public boolean isUsingShield(final Player player) {
        return this.shieldUseTicks.getOrDefault(player, 0) >= 5;
    }

    public boolean isHoldingUsableShield(final Player entity) {
        return (entity.getMainHandItem().is(Items.SHIELD)
                || entity.getOffhandItem().is(Items.SHIELD))
                && !this.isHoldingAnimationItemMainHand(entity);
    }

    private boolean isHoldingAnimationItemMainHand(final Player entity) {
        return entity.getMainHandItem().getUseDuration(entity) != 0
                && !entity.getOffhandItem().is(Items.SHIELD);
    }

    public boolean disablesShield(final Player player) {
        return player.getWeaponItem().getItem() instanceof AxeItem;
    }

    private record AttackEntry(Vec3 attackPos, Vec3 targetPos, long time, boolean wasBlocking) {}

    private record PendingBreak(Player target, int createdTick) {}

    private static class ShieldCooldownManager {

        private final Map<UUID, ShieldCooldown> cooldowns = new ConcurrentHashMap<>();

        public void setCooldown(final Player player) {
            this.setCooldown(player.getUUID(), 100);
        }

        public void setCooldown(final Player player, final int ticks) {
            this.setCooldown(player.getUUID(), ticks);
        }

        public void setCooldown(final UUID playerUuid) {
            this.setCooldown(playerUuid, 100);
        }

        public void setCooldown(final UUID playerUuid, final int ticks) {
            if (playerUuid == null) return;
            this.cooldowns.put(playerUuid, new ShieldCooldown(Math.max(0, ticks)));
        }

        public boolean isCoolingDown(final Player player) {
            if (player == null) return false;
            return this.isCoolingDown(player.getUUID());
        }

        public boolean isCoolingDown(final UUID playerUuid) {
            if (playerUuid == null) return false;
            final ShieldCooldown cd = this.cooldowns.get(playerUuid);
            return cd != null && cd.getTicks() > 0;
        }

        public int getRemainingTicks(final Player player) {
            if (player == null) return 0;
            return this.getRemainingTicks(player.getUUID());
        }

        public int getRemainingTicks(final UUID playerUuid) {
            if (playerUuid == null) return 0;
            final ShieldCooldown cd = this.cooldowns.get(playerUuid);
            return cd == null ? 0 : Math.max(0, cd.getTicks());
        }

        public void remove(final Player player) {
            if (player == null) return;
            this.remove(player.getUUID());
        }

        public void remove(final UUID playerUuid) {
            if (playerUuid == null) return;
            this.cooldowns.remove(playerUuid);
        }

        public void clear() {
            this.cooldowns.clear();
        }

        public void tick() {
            final Iterator<Map.Entry<UUID, ShieldCooldown>> it = this.cooldowns.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<UUID, ShieldCooldown> e = it.next();
                final ShieldCooldown cd = e.getValue();
                cd.tick();
                if (cd.isDone()) it.remove();
            }
        }

        private static final class ShieldCooldown {
            private int ticks;

            public ShieldCooldown(final int ticks) {
                this.ticks = Math.max(0, ticks);
            }

            public int getTicks() {
                return this.ticks;
            }

            public void tick() {
                if (this.ticks > 0) this.ticks--;
            }

            public boolean isDone() {
                return this.ticks <= 0;
            }
        }
    }
}
