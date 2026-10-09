package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WLModels {
    private static final List<BlockModelPart> PARTS = new ArrayList<>();
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
            final var state = block.defaultBlockState();
            final BlockStateModel model = shaper.getBlockModel(state);
            if (model == null || !hasBakedGeometry(model)) {
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

    private static boolean hasBakedGeometry(final BlockStateModel model) {
        PARTS.clear();
        try {
            model.collectParts(RANDOM, PARTS);
            return !PARTS.isEmpty();
        } finally {
            PARTS.clear();
        }
    }
}
