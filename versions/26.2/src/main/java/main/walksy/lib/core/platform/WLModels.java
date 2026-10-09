package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WLModels {
    private static final List<BlockStateModelPart> PARTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private WLModels() {
    }

    public static @Nullable TextureAtlasSprite particleSprite(final Block block) {
        final Minecraft client = Minecraft.getInstance();
        if (client == null || client.getModelManager() == null) {
            return null;
        }
        try {
            final var models = client.getModelManager().getBlockStateModelSet();
            final var state = block.defaultBlockState();
            final BlockStateModel model = models.get(state);
            if (model == null || !hasBakedGeometry(model)) {
                return null;
            }
            final var material = models.getParticleMaterial(state);
            final TextureAtlasSprite sprite = material == null ? null : material.sprite();
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
