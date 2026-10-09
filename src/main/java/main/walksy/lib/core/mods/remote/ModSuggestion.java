package main.walksy.lib.core.mods.remote;

import org.jetbrains.annotations.Nullable;

public final class ModSuggestion {
    private final String projectId;
    private final String slug;
    private final String title;
    private final String summary;
    private final @Nullable String iconUrl;
    private final Status status;
    private final @Nullable String versionNumber;
    private final @Nullable String fileName;
    private final @Nullable String downloadUrl;
    private final @Nullable String sha1;
    private final String reason;

    public enum Status {

        AVAILABLE,

        NOT_UPDATED
    }

    private ModSuggestion(final String projectId, final String slug, final String title, final String summary, final @Nullable String iconUrl, Status status, final @Nullable String versionNumber, final @Nullable String fileName, final @Nullable String downloadUrl, final @Nullable String sha1, final String reason) {
        this.projectId = projectId;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.iconUrl = iconUrl;
        this.status = status;
        this.versionNumber = versionNumber;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.sha1 = sha1;
        this.reason = reason;
    }

    static ModSuggestion available(final String projectId, final String slug, final String title, final String summary, final @Nullable String iconUrl, final String versionNumber, final String fileName, final String downloadUrl, final @Nullable String sha1) {
        return new ModSuggestion(projectId, slug, title, summary, iconUrl, Status.AVAILABLE, versionNumber, fileName, downloadUrl, sha1, "");
    }

    static ModSuggestion notUpdated(final String projectId, final String slug, final String title, final String summary, final @Nullable String iconUrl, final String reason) {
        return new ModSuggestion(projectId, slug, title, summary, iconUrl, Status.NOT_UPDATED, null, null, null, null, reason);
    }

    public String projectId() {
        return this.projectId;
    }

    public String slug() {
        return this.slug;
    }

    public String title() {
        return this.title;
    }

    public String summary() {
        return this.summary;
    }

    public @Nullable String iconUrl() {
        return this.iconUrl;
    }

    public Status status() {
        return this.status;
    }

    public boolean isAvailable() {
        return this.status == Status.AVAILABLE;
    }

    public @Nullable String versionNumber() {
        return this.versionNumber;
    }

    public @Nullable String fileName() {
        return this.fileName;
    }

    public @Nullable String downloadUrl() {
        return this.downloadUrl;
    }

    public @Nullable String sha1() {
        return this.sha1;
    }

    public String reason() {
        return this.reason;
    }

    public String pageUrl() {
        return "https://modrinth.com/mod/" + this.slug;
    }
}
