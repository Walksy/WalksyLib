package main.walksy.lib.core.mixin.mods;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSoundEvent",
            at = @At("HEAD"))
    public void onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (packet.getSound().getRegisteredName().toLowerCase().contains("shield.break")) {
            WalksyLib.getInstance().getShieldStateManager().handleSoundPacket(packet.getX(), packet.getY(), packet.getZ());
        }
    }
}

