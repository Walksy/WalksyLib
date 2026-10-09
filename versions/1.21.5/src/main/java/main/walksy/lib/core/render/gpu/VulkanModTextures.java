package main.walksy.lib.core.render.gpu;

import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;

final class VulkanModTextures {
    private VulkanModTextures() {
    }

    static @Nullable VulkanImage image(final int id) {
        final VkGlTexture texture = VkGlTexture.getTexture(id);
        return texture == null ? null : texture.getVulkanImage();
    }
}
