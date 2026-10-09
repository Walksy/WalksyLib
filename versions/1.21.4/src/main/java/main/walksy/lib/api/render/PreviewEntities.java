package main.walksy.lib.api.render;

import main.walksy.lib.core.render.preview.PreviewEntityBlit;
import main.walksy.lib.core.render.preview.PreviewPlayer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class PreviewEntities {
    private final Map<EntityType<?>, Entity> cache = new HashMap<>();
    private static final int FIRST_PREVIEW_ID = -1_000_000;
    private static final float ENTITY_Z = 50f;
    private static final int FULL_BRIGHT = 15728880;
    private Level cachedLevel;
    private int nextId = FIRST_PREVIEW_ID;
    private @Nullable TextureTarget target;
    private boolean redirecting;

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

    public @Nullable RenderTarget redirectedTarget() {
        return this.redirecting ? target : null;
    }

    void render(final WLGraphics2D graphics, final Entity entity, final float centerX, final float centerY, final float scale, final Vector3f translation, final Quaternionf rotation, final float x, final float y, final float width, final float height, final float alpha) {
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget main = minecraft.getMainRenderTarget();
        final TextureTarget offscreen = this.offscreen(main.width, main.height);
        final GuiGraphics guiGraphics = graphics.context().extractor();
        final Matrix3x2f flat = new Matrix3x2f(graphics.pose());
        guiGraphics.flush();
        offscreen.clear();
        final PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.last().pose().m00(flat.m00()).m01(flat.m01()).m10(flat.m10()).m11(flat.m11()).m30(flat.m20()).m31(flat.m21());
        pose.translate(centerX, centerY, ENTITY_Z);
        pose.scale(scale, scale, -scale);
        pose.translate(translation.x, translation.y, translation.z);
        pose.mulPose(rotation);
        Lighting.setupForEntityInInventory();
        final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        final Facing facing = Facing.face(entity);
        dispatcher.setRenderShadow(false);
        this.redirecting = true;
        offscreen.bindWrite(false);
        try {
            guiGraphics.drawSpecial(buffers -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 1.0F, pose, buffers, FULL_BRIGHT));
        } finally {
            this.redirecting = false;
            main.bindWrite(false);
            dispatcher.setRenderShadow(true);
            facing.restore(entity);
            pose.popPose();
            Lighting.setupFor3DItems();
        }
        this.submit(graphics, WLTexture.wrap(offscreen.getColorTextureId()), flat, x, y, width, height, offscreen, alpha);
    }

    private void submit(final WLGraphics2D graphics, final WLTexture texture, final Matrix3x2f flat, final float x, final float y, final float width, final float height, final RenderTarget offscreen, final float alpha) {
        final float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        final float x0 = flat.m00() * x + flat.m20();
        final float y0 = flat.m11() * y + flat.m21();
        final float x1 = flat.m00() * (x + width) + flat.m20();
        final float y1 = flat.m11() * (y + height) + flat.m21();
        final float u0 = x0 * guiScale / offscreen.width;
        final float u1 = x1 * guiScale / offscreen.width;
        final float v0 = 1f - y0 * guiScale / offscreen.height;
        final float v1 = 1f - y1 * guiScale / offscreen.height;
        final PreviewEntityBlit blit = new PreviewEntityBlit(x0, y0, x1, y1, u0, v0, u1, v1, alpha, graphics.scissor());
        blit.texture(texture);
        graphics.submit(blit);
    }

    private TextureTarget offscreen(final int width, final int height) {
        if (this.target == null) {
            this.target = new TextureTarget(width, height, true);
            this.target.setClearColor(0f, 0f, 0f, 0f);
        } else if (this.target.width != width || this.target.height != height) {
            this.target.resize(width, height);
        }
        return this.target;
    }

    private record Facing(float yRot, float xRot, float bodyRot, float headRot, float headRotO) {
        private static Facing face(final Entity entity) {
            final Facing facing = entity instanceof final LivingEntity living
                ? new Facing(entity.getYRot(), entity.getXRot(), living.yBodyRot, living.yHeadRot, living.yHeadRotO)
                : new Facing(entity.getYRot(), entity.getXRot(), 0f, 0f, 0f);
            entity.setYRot(0f);
            entity.setXRot(0f);
            if (entity instanceof final LivingEntity living) {
                living.yBodyRot = 0f;
                living.yHeadRot = 0f;
                living.yHeadRotO = 0f;
            }
            return facing;
        }

        private void restore(final Entity entity) {
            entity.setYRot(this.yRot);
            entity.setXRot(this.xRot);
            if (entity instanceof final LivingEntity living) {
                living.yBodyRot = this.bodyRot;
                living.yHeadRot = this.headRot;
                living.yHeadRotO = this.headRotO;
            }
        }
    }
}
