package main.walksy.lib.api.config.value;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ListIcon(List<Ref> candidates) {

    public sealed interface Ref {

        record Sprite(Identifier atlas, Identifier texture) implements Ref {
        }

        record Texture(Identifier texture) implements Ref {
        }

        record Resolved(Supplier<TextureAtlasSprite> sprite) implements Ref {
        }
    }

    public static ListIcon sprite(final Identifier atlas, final Identifier texture) {
        return new ListIcon(List.of(new Ref.Sprite(atlas, texture)));
    }

    public static ListIcon texture(final Identifier texture) {
        return new ListIcon(List.of(new Ref.Texture(texture)));
    }

    public static ListIcon resolved(final Supplier<TextureAtlasSprite> sprite) {
        return new ListIcon(List.of(new Ref.Resolved(sprite)));
    }

    public ListIcon or(final ListIcon other) {
        final List<Ref> merged = new ArrayList<>(this.candidates);
        merged.addAll(other.candidates());
        return new ListIcon(List.copyOf(merged));
    }
}
