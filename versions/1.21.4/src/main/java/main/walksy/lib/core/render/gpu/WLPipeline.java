package main.walksy.lib.core.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class WLPipeline {
    private final WLPipelineSpec spec;
    private final Map<String, Integer> samplerUnits = new HashMap<>();
    private int program;
    private VulkanBackend.Program vulkan;

    public WLPipeline(final WLPipelineSpec spec) {
        this.spec = spec;
    }

    WLPipelineSpec spec() {
        return this.spec;
    }

    int program() {
        if (this.program == 0) {
            this.program = this.link();
        }
        return this.program;
    }

    VulkanBackend.Program vulkan() {
        if (this.vulkan == null) {
            this.vulkan = VulkanBackend.compile(this.spec);
        }
        return this.vulkan;
    }

    int samplerUnit(final String name) {
        if (VulkanBackend.active()) {
            return this.spec.samplers().indexOf(name);
        }
        return this.samplerUnits.getOrDefault(name, -1);
    }

    private int link() {
        final int vertex = compile(GL20.GL_VERTEX_SHADER, "shaders/" + this.spec.vertexShader() + ".vsh");
        final int fragment = compile(GL20.GL_FRAGMENT_SHADER, "shaders/" + this.spec.fragmentShader() + ".fsh");
        final int linked = GL20.glCreateProgram();
        GL20.glAttachShader(linked, vertex);
        GL20.glAttachShader(linked, fragment);
        GL20.glBindAttribLocation(linked, 0, "Position");
        GL20.glBindAttribLocation(linked, 1, "UV0");
        GL20.glBindAttribLocation(linked, 2, "Color");
        GL20.glLinkProgram(linked);
        GL20.glDetachShader(linked, vertex);
        GL20.glDetachShader(linked, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            final String log = GL20.glGetProgramInfoLog(linked);
            GL20.glDeleteProgram(linked);
            throw new IllegalStateException("walksylib pipeline " + this.spec.name() + " failed to link: " + log);
        }
        GlStateManager._glUseProgram(linked);
        final List<String> samplers = this.spec.samplers();
        for (int unit = 0; unit < samplers.size(); unit++) {
            final int location = GL20.glGetUniformLocation(linked, samplers.get(unit));
            if (location >= 0) {
                GL20.glUniform1i(location, unit);
            }
            this.samplerUnits.put(samplers.get(unit), unit);
        }
        GlStateManager._glUseProgram(0);
        WLUniformBlocks.configure(linked, this.spec.uniforms());
        return linked;
    }

    private static int compile(final int type, final String path) {
        final Identifier location = Identifier.fromNamespaceAndPath("walksylib", path);
        final String source;
        try (final BufferedReader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location).openAsReader()) {
            source = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            final String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("walksylib shader " + path + " failed to compile: " + log);
        }
        return shader;
    }
}
