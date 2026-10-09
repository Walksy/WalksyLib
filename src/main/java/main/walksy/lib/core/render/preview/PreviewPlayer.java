package main.walksy.lib.core.render.preview;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;

public final class PreviewPlayer extends RemotePlayer {
    public PreviewPlayer(final ClientLevel level, final GameProfile profile) {
        super(level, profile);
    }

    public void copyModelParts(final Player source) {
        final Byte parts = source.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION);
        if (!parts.equals(this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION))) {
            this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, parts);
        }
    }

    @Override
    public boolean isInvisibleTo(final Player player) {
        return true;
    }
}
