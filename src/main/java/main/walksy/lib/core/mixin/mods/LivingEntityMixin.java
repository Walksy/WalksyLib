package main.walksy.lib.core.mixin.mods;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "handleEntityEvent",
            at = @At("HEAD"))
    public void onHandleStatusUpdate(final byte status, final CallbackInfo ci) {
        final LivingEntity entity = LivingEntity.class.cast(this);
        if (entity instanceof Player player) {
            WalksyLib.getInstance().getShieldStateManager().handleEntityStatus(player, status);
        }
    }
}
