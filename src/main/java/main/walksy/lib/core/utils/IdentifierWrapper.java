package main.walksy.lib.core.utils;

import net.minecraft.util.Identifier;

import java.util.Objects;

public class IdentifierWrapper {

    private Identifier identifier;
    private String fileName;

    public IdentifierWrapper(Identifier identifier)
    {
        this(identifier, "");
    }

    public IdentifierWrapper(Identifier identifier, String fileName)
    {
        this.identifier = identifier;
        this.fileName = fileName;
    }

    public Identifier getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(Identifier identifier)
    {
        this.identifier = identifier;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        IdentifierWrapper other = (IdentifierWrapper) obj;

        if ((!Objects.equals(other.identifier.getPath(), identifier.getPath())) || (!Objects.equals(other.identifier.getNamespace(), identifier.getNamespace()))) return false;
        return other.fileName.equals(fileName);
    }
}
