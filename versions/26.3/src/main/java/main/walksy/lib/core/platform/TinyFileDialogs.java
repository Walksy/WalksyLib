package main.walksy.lib.core.platform;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class TinyFileDialogs {
    private static final String PATTERN_SEPARATOR = ";";
    private static final String WILDCARD_PREFIX = "*.";

    private TinyFileDialogs() {
    }

    public static void openFile(final String title, final String[] patterns, final String description, final Consumer<String> onPicked) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ByteBuffer name = MemoryUtil.memUTF8(description);
        final ByteBuffer pattern = MemoryUtil.memUTF8(joinPatterns(patterns));
        final SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.malloc(1);
        filters.get(0).name(name).pattern(pattern);
        final SDL_DialogFileCallback[] callback = new SDL_DialogFileCallback[1];
        final Runnable release = () -> {
            callback[0].free();
            filters.free();
            MemoryUtil.memFree(name);
            MemoryUtil.memFree(pattern);
        };
        callback[0] = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
            final String picked = firstPath(filelist);
            minecraft.execute(() -> {
                release.run();
                if (picked != null) {
                    onPicked.accept(picked);
                }
            });
        });
        try {
            SDLDialog.SDL_ShowOpenFileDialog(callback[0], 0L, minecraft.getWindow().handle(), filters, (CharSequence) null, false);
        } catch (final Throwable e) {
            release.run();
        }
    }

    private static String firstPath(final long filelist) {
        if (filelist == 0L) {
            return null;
        }
        final long first = MemoryUtil.memGetAddress(filelist);
        return first == 0L ? null : MemoryUtil.memUTF8(first);
    }

    private static String joinPatterns(final String[] patterns) {
        final StringBuilder joined = new StringBuilder();
        for (final String raw : patterns) {
            if (!joined.isEmpty()) {
                joined.append(PATTERN_SEPARATOR);
            }
            joined.append(raw.startsWith(WILDCARD_PREFIX) ? raw.substring(WILDCARD_PREFIX.length()) : raw);
        }
        return joined.toString();
    }
}
