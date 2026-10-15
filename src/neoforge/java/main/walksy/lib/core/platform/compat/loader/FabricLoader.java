package main.walksy.lib.core.platform.compat.loader;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private final Map<String, ModContainer> containers = new ConcurrentHashMap<>();
    private Boolean development;

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public boolean isModLoaded(final String id) {
        try {
            final ModList list = ModList.get();
            if (list != null) {
                return list.isLoaded(id);
            }
        } catch (final Throwable ignored) {
        }
        return false;
    }

    public boolean isDevelopmentEnvironment() {
        if (this.development == null) {
            boolean directory = false;
            try {
                final CodeSource source = FabricLoader.class.getProtectionDomain().getCodeSource();
                if (source != null && source.getLocation() != null) {
                    directory = Files.isDirectory(Path.of(source.getLocation().toURI()));
                }
            } catch (final Exception ignored) {
                directory = false;
            }
            this.development = directory;
        }
        return this.development;
    }

    public Optional<ModContainer> getModContainer(final String id) {
        final ModList list = ModList.get();
        if (list == null) {
            return Optional.empty();
        }
        final ModContainer cached = this.containers.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        for (final IModInfo info : list.getMods()) {
            if (info.getModId().equals(id)) {
                final ModContainer created = new ModContainer(info);
                this.containers.put(id, created);
                return Optional.of(created);
            }
        }
        return Optional.empty();
    }

    public Collection<ModContainer> getAllMods() {
        final List<ModContainer> mods = new ArrayList<>();
        final ModList list = ModList.get();
        if (list != null) {
            for (final IModInfo info : list.getMods()) {
                this.getModContainer(info.getModId()).ifPresent(mods::add);
            }
        }
        return mods;
    }

    public <T> List<EntrypointContainer<T>> getEntrypointContainers(final String key, final Class<T> type) {
        final List<EntrypointContainer<T>> found = new ArrayList<>();
        final ModList list = ModList.get();
        if (list == null) {
            return found;
        }
        for (final IModInfo info : list.getMods()) {
            final Object className = info.getModProperties().get(key);
            if (className == null) {
                continue;
            }
            try {
                final Class<?> loaded = Class.forName(String.valueOf(className), true, FabricLoader.class.getClassLoader());
                final Object instance = loaded.getDeclaredConstructor().newInstance();
                final ModContainer container = this.getModContainer(info.getModId()).orElse(null);
                if (container != null && type.isInstance(instance)) {
                    found.add(new EntrypointContainer<>(type.cast(instance), container));
                }
            } catch (final ReflectiveOperationException | LinkageError e) {
                throw new IllegalStateException("Could not create the " + key + " entrypoint " + className + " of " + info.getModId(), e);
            }
        }
        return found;
    }
}
