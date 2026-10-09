package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class WLModels {
    private static final RandomSource RANDOM = RandomSource.create();

    private WLModels() {
    }

    public static @Nullable TextureAtlasSprite particleSprite(final Block block) {
        final Minecraft client = Minecraft.getInstance();
        if (client == null || client.getModelManager() == null) {
            return null;
        }
        try {
            final BlockModelShaper shaper = client.getModelManager().getBlockModelShaper();
            final BlockState state = block.defaultBlockState();
            final BakedModel model = shaper.getBlockModel(state);
            if (model == null || !hasBakedGeometry(model, state)) {
                return null;
            }
            final TextureAtlasSprite sprite = shaper.getParticleIcon(state);
            if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                return null;
            }
            return sprite;
        } catch (final RuntimeException notReady) {
            return null;
        }
    }

    private static boolean hasBakedGeometry(final BakedModel model, final BlockState state) {
        if (!model.getQuads(state, null, RANDOM).isEmpty()) {
            return true;
        }
        for (final Direction direction : Direction.values()) {
            if (!model.getQuads(state, direction, RANDOM).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
