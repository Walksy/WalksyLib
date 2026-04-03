package main.walksy.lib.core.mixin.mods;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "attack",
            at = @At("HEAD"))
    public void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (target instanceof Player targetPlayer) {
            WalksyLib.getInstance().getShieldStateManager().handlePlayerAttack(targetPlayer);
        }
    }
}
