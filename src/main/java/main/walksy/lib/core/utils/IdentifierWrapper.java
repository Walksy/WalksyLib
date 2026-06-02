package main.walksy.lib.core.utils;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public class IdentifierWrapper {

    private Identifier identifier;
    private String fileName;

    public IdentifierWrapper(final Identifier identifier) {
        this(identifier, "");
    }

    public IdentifierWrapper(final Identifier identifier, final String fileName) {
        this.identifier = identifier;
        this.fileName = fileName;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(final Identifier identifier) {
        this.identifier = identifier;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) return false;

        final IdentifierWrapper other = (IdentifierWrapper) obj;

        if ((!Objects.equals(other.identifier.getPath(), this.identifier.getPath())) || (!Objects.equals(other.identifier.getNamespace(), this.identifier.getNamespace()))) return false;
        return other.fileName.equals(this.fileName);
    }
}
