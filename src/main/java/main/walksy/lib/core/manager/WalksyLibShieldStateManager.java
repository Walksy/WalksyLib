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
    private final Minecraft client;
    private final ShieldCooldownManager cooldownManager;
    private final HashMap<String, Consumer<Player>> onDisableEvents;
    private int localShieldCooldownTicks;
    private int currentTick;
    private PendingBreak pendingBreak;

    public WalksyLibShieldStateManager() {
        this.shieldUseTicks = new HashMap<>();
        this.attackedPlayerEntries = new HashMap<>();
        this.client = Minecraft.getInstance();
        this.cooldownManager = new ShieldCooldownManager();
        this.onDisableEvents = new HashMap<>();
        this.localShieldCooldownTicks = 0;
        this.currentTick = 0;
        this.pendingBreak = null;
    }

    public void disable(Player player) {
        if (player == null) {
            return;
        }
        this.cooldownManager.setCooldown(player);
        if (this.attackedPlayerEntries.containsKey(player)) {
            this.onDisableEvents.forEach((_, callback) -> callback.accept(player));
        }
    }

    public void addDisableCallback(String id, Consumer<Player> callback) {
        this.onDisableEvents.put(id, callback);
    }

    public void removeDisableCallback(String id) {
        this.onDisableEvents.remove(id);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (this.client.level == null) return;
        this.currentTick++;
        for (Player player : this.client.level.players()) {
            if (this.isHoldingUsableShield(player) && player.isUsingItem()) {
                int current = this.shieldUseTicks.getOrDefault(player, 0);
                this.shieldUseTicks.put(player, current + 1);
            } else {
                this.shieldUseTicks.put(player, 0);
            }
        }
        if (this.client.player != null && this.client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
            this.localShieldCooldownTicks++;
        } else {
            this.localShieldCooldownTicks = 0;
        }
        if (this.pendingBreak != null && this.currentTick - this.pendingBreak.createdTick() >= 1) {
            if (this.client.player == null || !this.client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) {
                this.disable(this.pendingBreak.target());
            }
            this.pendingBreak = null;
        }
        this.attackedPlayerEntries.entrySet().removeIf(e -> now - e.getValue().time > this.ATTACK_ENTRY_TTL_MS);
        this.cooldownManager.tick();
    }

    public void handleSoundPacket(double x, double y, double z) {
        if (this.client.level == null || this.client.player == null) return;
        LocalPlayer local = this.client.player;
        long now = System.currentTimeMillis();
        if (this.localShieldCooldownTicks > 2) {
            this.disable(this.nearestPlayerToSound(x, y, z, local));
            return;
        }
        final double matchRadiusSq = 36.0;
        Player best = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (Map.Entry<Player, AttackEntry> e : this.attackedPlayerEntries.entrySet()) {
            Player candidate = e.getKey();
            AttackEntry ae = e.getValue();
            if (candidate == null || candidate == local) continue;
            if (now - ae.time > this.ATTACK_ENTRY_TTL_MS) continue;
            if (!ae.wasBlocking) continue;

            double dx = candidate.getX() - x;
            double dy = candidate.getY() - y;
            double dz = candidate.getZ() - z;
            double currentDistSq = dx * dx + dy * dy + dz * dz;

            double sdx = ae.targetPos.x - x;
            double sdy = ae.targetPos.y - y;
            double sdz = ae.targetPos.z - z;
            double storedDistSq = sdx * sdx + sdy * sdy + sdz * sdz;

            double effectiveDistSq = Math.min(currentDistSq, storedDistSq);
            if (effectiveDistSq > matchRadiusSq) continue;

            if (effectiveDistSq < bestDistSq) {
                bestDistSq = effectiveDistSq;
                best = candidate;
            }
        }
        double selfDx = local.getX() - x;
        double selfDy = local.getY() - y;
        double selfDz = local.getZ() - z;
        double selfDistSq = selfDx * selfDx + selfDy * selfDy + selfDz * selfDz;
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

    private Player nearestPlayerToSound(double x, double y, double z, LocalPlayer local) {
        Player nearest = null;
        double nearestDistSq = Double.POSITIVE_INFINITY;
        for (Player player : this.client.level.players()) {
            if (player == local) continue;
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    public void handleEntityStatus(Player player, byte status) {
        if (status == SHIELD_DISABLE_STATUS && player != this.client.player) {
            this.disable(player);
        }
    }

    public void handlePlayerAttack(Player target) {
        boolean estBlocking = this.shieldUseTicks.getOrDefault(target, 0) >= 3;
        if (this.client.player == null) return;
        if (this.disablesShield(this.client.player)) {
            this.attackedPlayerEntries.put(
                    target,
                    new AttackEntry(
                            this.client.player.position(),
                            target.position(),
                            System.currentTimeMillis(),
                            estBlocking
                    )
            );
        }
    }

    public boolean isCoolingDown(Player player) {
        if (player == this.client.player) {
            return this.client.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
        }
        return this.cooldownManager.isCoolingDown(player);
    }

    public float getCooldownProgress(Player player) {
        if (player == null) return 0.0f;
        if (player == this.client.player) {
            return this.client.player.getCooldowns().getCooldownPercent(new ItemStack(Items.SHIELD), 0);
        }
        int remaining = this.cooldownManager.getRemainingTicks(player);
        if (remaining <= 0) return 0.0f;
        float frac = remaining / (float) 100;
        return Math.max(0f, Math.min(1f, frac));
    }

    public boolean isUsingShield(Player player) {
        return this.shieldUseTicks.getOrDefault(player, 0) >= 5;
    }

    public boolean isHoldingUsableShield(Player entity) {
        return (entity.getMainHandItem().is(Items.SHIELD)
                || entity.getOffhandItem().is(Items.SHIELD))
                && !this.isHoldingAnimationItemMainHand(entity);
    }

    private boolean isHoldingAnimationItemMainHand(Player entity) {
        return entity.getMainHandItem().getUseDuration(entity) != 0
                && !entity.getOffhandItem().is(Items.SHIELD);
    }

    public boolean disablesShield(Player player) {
        return player.getWeaponItem().getItem() instanceof AxeItem;
    }

    private record AttackEntry(Vec3 attackPos, Vec3 targetPos, long time, boolean wasBlocking) {

    }

    private record PendingBreak(Player target, int createdTick) {

    }

    private static class ShieldCooldownManager {

        private final Map<UUID, ShieldCooldown> cooldowns = new ConcurrentHashMap<>();

        public void setCooldown(Player player) {
            setCooldown(player.getUUID(), 100);
        }

        public void setCooldown(Player player, int ticks) {
            setCooldown(player.getUUID(), ticks);
        }

        public void setCooldown(UUID playerUuid) {
            setCooldown(playerUuid, 100);
        }

        public void setCooldown(UUID playerUuid, int ticks) {
            if (playerUuid == null) return;
            cooldowns.put(playerUuid, new ShieldCooldown(Math.max(0, ticks)));
        }

        public boolean isCoolingDown(Player player) {
            if (player == null) return false;
            return isCoolingDown(player.getUUID());
        }

        public boolean isCoolingDown(UUID playerUuid) {
            if (playerUuid == null) return false;
            ShieldCooldown cd = cooldowns.get(playerUuid);
            return cd != null && cd.getTicks() > 0;
        }

        public int getRemainingTicks(Player player) {
            if (player == null) return 0;
            return getRemainingTicks(player.getUUID());
        }

        public int getRemainingTicks(UUID playerUuid) {
            if (playerUuid == null) return 0;
            ShieldCooldown cd = cooldowns.get(playerUuid);
            return cd == null ? 0 : Math.max(0, cd.getTicks());
        }

        public void remove(Player player) {
            if (player == null) return;
            remove(player.getUUID());
        }

        public void remove(UUID playerUuid) {
            if (playerUuid == null) return;
            cooldowns.remove(playerUuid);
        }

        public void clear() {
            cooldowns.clear();
        }

        public void tick() {
            Iterator<Map.Entry<UUID, ShieldCooldown>> it = cooldowns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ShieldCooldown> e = it.next();
                ShieldCooldown cd = e.getValue();
                cd.tick();
                if (cd.isDone()) it.remove();
            }
        }

        private static final class ShieldCooldown {
            private int ticks;

            public ShieldCooldown(int ticks) {
                this.ticks = Math.max(0, ticks);
            }

            public int getTicks() {
                return ticks;
            }

            public void tick() {
                if (ticks > 0) ticks--;
            }

            public boolean isDone() {
                return ticks <= 0;
            }
        }
    }

}
