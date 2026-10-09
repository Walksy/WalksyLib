package main.walksy.lib.core.render.gpu;

import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;

final class VulkanModTextures {
    private VulkanModTextures() {
    }

    static @Nullable VulkanImage image(final int id) {
        final GlTexture texture = GlTexture.getTexture(id);
        return texture == null ? null : texture.getVulkanImage();
    }
}
