package main.walksy.lib.core.mixin.mods;

import main.walksy.lib.core.WalksyLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    public void onSound(final ClientboundSoundPacket packet, final CallbackInfo ci) {
        WalksyLib.getInstance().onSoundEvent(packet.getSound(), packet.getX(), packet.getY(), packet.getZ());
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"))
    public void onSoundEntity(final ClientboundSoundEntityPacket packet, final CallbackInfo ci) {
        final Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread() || client.level == null) {
            return;
        }
        final Entity entity = client.level.getEntity(packet.getId());
        if (entity != null) {
            WalksyLib.getInstance().onSoundEvent(packet.getSound(), entity.getX(), entity.getY(), entity.getZ());
        }
    }
}
