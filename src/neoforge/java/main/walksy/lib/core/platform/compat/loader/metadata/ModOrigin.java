package main.walksy.lib.core.platform.compat.loader.metadata;

import java.nio.file.Path;
import java.util.List;

public final class ModOrigin {
    public enum Kind {
        PATH,
        NESTED,
        UNKNOWN
    }

    private final Kind kind;
    private final List<Path> paths;

    public ModOrigin(final Kind kind, final List<Path> paths) {
        this.kind = kind;
        this.paths = paths;
    }

    public Kind getKind() {
        return this.kind;
    }

    public List<Path> getPaths() {
        return this.paths;
    }
}
