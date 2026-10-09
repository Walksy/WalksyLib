package main.walksy.lib.api.data;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface ShieldState {
    boolean isUsingShield(Player player);

    boolean isUsingShield(Player player, boolean factorDelay);

    int shieldUseTicks(Player player);

    boolean isHoldingUsableShield(Player player);

    boolean isCoolingDown(@Nullable Player player);

    float getCooldownProgress(@Nullable Player player);
}
