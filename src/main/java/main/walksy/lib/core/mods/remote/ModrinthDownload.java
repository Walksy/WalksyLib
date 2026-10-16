package main.walksy.lib.core.mods.remote;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;

public final class ModrinthDownload {

    private ModrinthDownload() {
    }

    public static byte[] fetchVerified(final String url, final @Nullable String sha1) throws IOException, InterruptedException {
        if (!isModrinthCdn(url)) {
            throw new IOException("Refusing to download from an unexpected host");
        }
        final byte[] bytes = ModrinthApi.bytes(url);
        if (sha1 != null && !sha1.isEmpty() && !sha1.equalsIgnoreCase(sha1(bytes))) {
            throw new IOException("Downloaded file did not match its published checksum");
        }
        return bytes;
    }

    public static Path modsDirectory() throws IOException {
        final Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
        Files.createDirectories(mods);
        return mods;
    }

    public static Path writeToMods(final byte[] bytes, final String fileName) throws IOException {
        return writeStaged(modsDirectory().resolve(safeFileName(fileName)), bytes);
    }

    public static Path writeStaged(final Path target, final byte[] bytes) throws IOException {
        final Path part = target.resolveSibling(target.getFileName() + ".part");
        try {
            Files.write(part, bytes);
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            try {
                Files.deleteIfExists(part);
            } catch (final IOException ignored) {
            }
            throw e;
        }
        return target;
    }

    public static String safeFileName(final String raw) {
        final String name = Path.of(raw).getFileName().toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".jar") ? name : name + ".jar";
    }

    private static boolean isModrinthCdn(final String url) {
        try {
            final String host = URI.create(url).getHost();
            return host != null && (host.equalsIgnoreCase("cdn.modrinth.com") || host.toLowerCase(Locale.ROOT).endsWith(".modrinth.com"));
        } catch (final RuntimeException e) {
            return false;
        }
    }

    static String sha1(final byte[] bytes) throws IOException {
        final byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-1").digest(bytes);
        } catch (final Exception e) {
            throw new IOException("SHA-1 is unavailable on this JVM", e);
        }
        final StringBuilder hex = new StringBuilder(digest.length * 2);
        for (final byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
