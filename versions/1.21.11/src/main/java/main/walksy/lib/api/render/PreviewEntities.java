package main.walksy.lib.api.render;

import main.walksy.lib.core.render.preview.PreviewEntityBlit;
import main.walksy.lib.core.render.preview.PreviewPlayer;

import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PreviewEntities {
    private final Map<EntityType<?>, Entity> cache = new HashMap<>();
    private final Map<Object, PreviewEntityBlit> pending = new IdentityHashMap<>();
    private static final int FIRST_PREVIEW_ID = -1_000_000;
    private static final int MAX_PENDING = 8;
    private static final int FULL_BRIGHT = 15728880;
    private Level cachedLevel;
    private int nextId = FIRST_PREVIEW_ID;

    public @Nullable Entity of(final EntityType<?> type) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.cache.clear();
            this.cachedLevel = null;
            return null;
        }
        if (this.cachedLevel != minecraft.level) {
            this.cache.clear();
            this.cachedLevel = minecraft.level;
        }
        if (!this.cache.containsKey(type)) {
            this.cache.put(type, this.create(type, minecraft));
        }
        final Entity entity = this.cache.get(type);
        if (entity instanceof final PreviewPlayer player && minecraft.player != null) {
            player.copyModelParts(minecraft.player);
        }
        return entity;
    }

    public @Nullable Entity player() {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : this.of(player.getType());
    }

    private @Nullable Entity create(final EntityType<?> type, final Minecraft minecraft) {
        try {
            final Entity entity = minecraft.player != null && type == minecraft.player.getType()
                ? new PreviewPlayer(minecraft.level, minecraft.player.getGameProfile())
                : type.create(minecraft.level, EntitySpawnReason.LOAD);
            if (entity != null) {
                entity.setId(this.nextId--);
            }
            return entity;
        } catch (final RuntimeException broken) {
            return null;
        }
    }

    void forget(final Entity entity) {
        this.cache.replaceAll((type, cached) -> cached == entity ? null : cached);
    }

    public boolean capture(final Object state, final WLTexture texture) {
        final PreviewEntityBlit blit = this.pending.remove(state);
        if (blit == null) {
            return false;
        }
        blit.texture(texture);
        return true;
    }

    void render(final WLGraphics2D graphics, final Entity entity, final float centerX, final float centerY, final float scale, final Vector3f translation, final Quaternionf rotation, final float x, final float y, final float width, final float height, final float alpha) {
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final int x0 = Math.round(pose.m00() * x + pose.m20());
        final int y0 = Math.round(pose.m11() * y + pose.m21());
        final int x1 = Math.round(pose.m00() * (x + width) + pose.m20());
        final int y1 = Math.round(pose.m11() * (y + height) + pose.m21());
        final float posedScale = scale * pose.m00();
        final float offsetX = (pose.m00() * centerX + pose.m20() - (x0 + x1) / 2f) / posedScale;
        final float offsetY = (pose.m11() * centerY + pose.m21() - (y0 + y1) / 2f) / posedScale;
        final Vector3f shifted = new Vector3f(translation).add(offsetX, offsetY, 0f);
        final var extractor = graphics.context().extractor();
        final GuiEntityRenderState state = new GuiEntityRenderState(this.renderState(entity), shifted, rotation, null, x0, y0, x1, y1, posedScale, extractor.scissorStack.peek());
        if (this.layered()) {
            if (this.pending.size() >= MAX_PENDING) {
                this.pending.clear();
            }
            final PreviewEntityBlit blit = new PreviewEntityBlit(x0, y0, x1, y1, alpha, graphics.scissor());
            this.pending.put(state, blit);
            graphics.submit(blit);
        }
        extractor.guiRenderState.submitPicturesInPictureState(state);
    }

    private boolean layered() {
        final var stratum = WLRenderSystem.contentStratum();
        return stratum.isActive() || stratum.isPopoverActive() || stratum.isTopActive() || stratum.isDevActive();
    }

    private EntityRenderState renderState(final Entity entity) {
        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        final EntityRenderState state = dispatcher.getRenderer(entity).createRenderState(entity, 1.0F);
        state.lightCoords = FULL_BRIGHT;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        if (state instanceof final LivingEntityRenderState living) {
            living.bodyRot = 0f;
            living.yRot = 0f;
            living.xRot = 0f;
        }
        return state;
    }
}
