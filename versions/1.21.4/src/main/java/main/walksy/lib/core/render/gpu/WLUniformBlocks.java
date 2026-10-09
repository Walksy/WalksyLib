package main.walksy.lib.core.render.gpu;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class WLUniformBlocks {
    private static final Map<String, Integer> BINDINGS = new HashMap<>();

    private WLUniformBlocks() {
    }

    static int binding(final String name) {
        final Integer existing = BINDINGS.get(name);
        if (existing != null) {
            return existing;
        }
        final int assigned = BINDINGS.size() + 1;
        BINDINGS.put(name, assigned);
        return assigned;
    }

    static void configure(final int program, final List<String> names) {
        for (final String name : names) {
            final int index = GL31.glGetUniformBlockIndex(program, name);
            if (index != GL31.GL_INVALID_INDEX) {
                GL31.glUniformBlockBinding(program, index, binding(name));
            }
        }
    }

    static void bind(final String name, final int buffer) {
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, binding(name), buffer);
    }
}
