package main.walksy.lib.core.platform.compat.loader;

import main.walksy.lib.core.platform.compat.loader.metadata.ModMetadata;
import main.walksy.lib.core.platform.compat.loader.metadata.ModOrigin;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

public final class ModContainer {
    private final IModInfo info;
    private final ModMetadata metadata;

    ModContainer(final IModInfo info) {
        this.info = info;
        this.metadata = new ModMetadata(info);
    }

    public ModMetadata getMetadata() {
        return this.metadata;
    }

    public ModOrigin getOrigin() {
        try {
            final Path file = this.info.getOwningFile().getFile().getFilePath();
            return new ModOrigin(ModOrigin.Kind.PATH, List.of(file));
        } catch (final Throwable t) {
            return new ModOrigin(ModOrigin.Kind.UNKNOWN, List.of());
        }
    }

    public Optional<Path> findPath(final String file) {
        try {
            final Object modFile = this.info.getOwningFile().getFile();
            try {
                final Method find = modFile.getClass().getMethod("findResource", String[].class);
                final Object found = find.invoke(modFile, (Object) file.split("/"));
                if (found instanceof Path path && Files.exists(path)) {
                    return Optional.of(path);
                }
                return Optional.empty();
            } catch (final NoSuchMethodException ignored) {
            }
            final Object contents = modFile.getClass().getMethod("getContents").invoke(modFile);
            final Method open = contents.getClass().getMethod("openFile", String.class);
            try (InputStream in = (InputStream) open.invoke(contents, file)) {
                final String suffix = file.contains(".") ? file.substring(file.lastIndexOf('.')) : ".bin";
                final Path temp = Files.createTempFile("walksylib-" + this.info.getModId() + "-", suffix);
                temp.toFile().deleteOnExit();
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                return Optional.of(temp);
            }
        } catch (final IOException | ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
