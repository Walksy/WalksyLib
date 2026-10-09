package main.walksy.lib.core.mods.data;

import main.walksy.lib.api.data.ShieldState;
import main.walksy.lib.api.event.WalksyLibEvents;
import main.walksy.lib.api.sound.SoundAttributor;
import main.walksy.lib.core.platform.WLRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ShieldStateDataTracker implements ShieldState {
    private static final int ATTACK_ENTRY_TTL_TICKS = 20;
    private static final int COOLDOWN_TICKS = 100;
    private static final int BLOCKING_TICKS = 3;
    private static final int USING_TICKS = 5;
    private static final int LOCAL_COOLDOWN_GRACE_TICKS = 2;
    private static final double SOUND_MATCH_RADIUS = 6.0;
    private static final byte SHIELD_DISABLE_STATUS = 30;
    private final Map<UUID, Integer> shieldUseTicks = new HashMap<>();
    private final Map<UUID, Integer> cooldowns = new HashMap<>();
    private final SoundAttributor<Boolean> shieldBreaks;
    private int localShieldCooldownTicks;
    private int currentTick;
    private @Nullable PendingBreak pendingBreak;

    private record PendingBreak(UUID target, int createdTick) {
    }

    public ShieldStateDataTracker() {
        this.shieldBreaks = SoundAttributor.<Boolean>builder()
            .sounds(sound -> WLRegistries.soundId(sound.value()).toString().toLowerCase(Locale.ROOT).contains("shield.break"))
            .radius(SOUND_MATCH_RADIUS)
            .expireAfterTicks(ATTACK_ENTRY_TTL_TICKS)
            .candidateFilter((player, blocking) -> Boolean.TRUE.equals(blocking) && player != Minecraft.getInstance().player)
            .intercept(this::interceptLocalBreak)
            .onAttributed(this::onShieldBreak)
            .register();
    }

    public void clear() {
        this.shieldUseTicks.clear();
        this.cooldowns.clear();
        this.localShieldCooldownTicks = 0;
        this.currentTick = 0;
        this.pendingBreak = null;
    }

    public void disable(@Nullable final Player player) {
        if (player == null) {
            return;
        }
        this.cooldowns.put(player.getUUID(), COOLDOWN_TICKS);
        if (this.shieldBreaks.isRecorded(player)) {
            WalksyLibEvents.SHIELD_DISABLE.invoker().disable(player);
        }
    }

    public void tick() {
        final Minecraft client = Minecraft.getInstance();
        final ClientLevel level = client.level;
        if (level == null) {
            return;
        }
        this.currentTick++;
        final Set<UUID> present = new HashSet<>();
        for (final Player player : level.players()) {
            final UUID uuid = player.getUUID();
            present.add(uuid);
            if (this.isHoldingUsableShield(player) && player.isUsingItem()) {
                this.shieldUseTicks.merge(uuid, 1, Integer::sum);
            } else {
                this.shieldUseTicks.remove(uuid);
            }
        }
        this.shieldUseTicks.keySet().retainAll(present);
        this.localShieldCooldownTicks = client.player != null && localShieldOnCooldown(client.player) ? this.localShieldCooldownTicks + 1 : 0;
        if (this.pendingBreak != null && this.currentTick - this.pendingBreak.createdTick() >= 1) {
            if (client.player == null || !localShieldOnCooldown(client.player)) {
                this.disable(level.getPlayerByUUID(this.pendingBreak.target()));
            }
            this.pendingBreak = null;
        }
        this.cooldowns.replaceAll((uuid, ticks) -> ticks - 1);
        this.cooldowns.values().removeIf(ticks -> ticks <= 0);
    }

    private boolean interceptLocalBreak(final SoundAttributor.SoundHeard heard) {
        final LocalPlayer local = Minecraft.getInstance().player;
        if (local == null) {
            return true;
        }
        if (this.localShieldCooldownTicks > LOCAL_COOLDOWN_GRACE_TICKS) {
            this.disable(heard.nearestPlayer(player -> player != local));
            return true;
        }
        return false;
    }

    private void onShieldBreak(final SoundAttributor.Attribution<Boolean> attribution) {
        final LocalPlayer local = Minecraft.getInstance().player;
        if (local != null && attribution.distanceSq() < local.position().distanceToSqr(attribution.soundPosition())) {
            this.disable(attribution.player());
        } else {
            this.pendingBreak = new PendingBreak(attribution.player().getUUID(), this.currentTick);
        }
    }

    public void handleEntityStatus(final Player player, final byte status) {
        if (status == SHIELD_DISABLE_STATUS && player != Minecraft.getInstance().player) {
            this.disable(player);
        }
    }

    public void handlePlayerAttack(final Player target) {
        final LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || !this.disablesShield(local)) {
            return;
        }
        this.shieldBreaks.record(target, this.shieldUseTicks(target) >= BLOCKING_TICKS);
    }

    @Override
    public boolean isCoolingDown(@Nullable final Player player) {
        if (player == null) {
            return false;
        }
        final LocalPlayer local = Minecraft.getInstance().player;
        if (player == local) {
            return localShieldOnCooldown(local);
        }
        return this.cooldowns.containsKey(player.getUUID());
    }

    @Override
    public float getCooldownProgress(@Nullable final Player player) {
        if (player == null) {
            return 0f;
        }
        final LocalPlayer local = Minecraft.getInstance().player;
        if (player == local) {
            return local.getCooldowns().getCooldownPercent(new ItemStack(Items.SHIELD), 0);
        }
        final int remaining = this.cooldowns.getOrDefault(player.getUUID(), 0);
        return Math.max(0f, Math.min(1f, remaining / (float) COOLDOWN_TICKS));
    }

    @Override
    public boolean isUsingShield(final Player player) {
        return this.isUsingShield(player, true);
    }

    @Override
    public boolean isUsingShield(final Player player, final boolean factorDelay) {
        if (!factorDelay) {
            return player.isUsingItem() && this.isHoldingUsableShield(player);
        }
        return this.shieldUseTicks(player) >= USING_TICKS;
    }

    @Override
    public int shieldUseTicks(final Player player) {
        return this.shieldUseTicks.getOrDefault(player.getUUID(), 0);
    }

    @Override
    public boolean isHoldingUsableShield(final Player player) {
        final ItemStack mainHand = player.getMainHandItem();
        final boolean holdingShield = mainHand.is(Items.SHIELD) || player.getOffhandItem().is(Items.SHIELD);
        return holdingShield && (mainHand.is(Items.SHIELD) || mainHand.getUseDuration(player) == 0);
    }

    public boolean disablesShield(final Player player) {
        return player.getWeaponItem().is(ItemTags.AXES);
    }

    private static boolean localShieldOnCooldown(final LocalPlayer player) {
        return player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
    }

}
