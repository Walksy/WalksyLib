package main.walksy.lib.core.gui.widget.options;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.internal.ChangelogConfig;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.gui.WLScreen;
import main.walksy.lib.core.gui.widget.ResetButton;
import main.walksy.lib.core.gui.widget.WLWidget;
import main.walksy.lib.core.gui.widget.options.control.ControlLayers;
import main.walksy.lib.core.gui.widget.options.control.PillSelectorWidget;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.mods.remote.Changelog;
import main.walksy.lib.core.mods.remote.ModUpdate;
import main.walksy.lib.core.mods.remote.ModrinthApi;
import main.walksy.lib.core.platform.WLClient;
import main.walksy.lib.core.render.Cursors;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.font.Fonts;
import main.walksy.lib.core.render.font.msdf.MsdfFont;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChangelogPaneWidget extends WLWidget {
    private static final float RAIL_X = 7f;
    private static final float RAIL_WIDTH = 1.5f;
    private static final float DOT_RADIUS = 4.5f;
    private static final float RING_RADIUS = 8f;
    private static final float CARD_LEFT = 24f;
    private static final float CARD_GAP = 12f;
    private static final float CARD_CORNER = 12f;
    private static final float PAD_X = 14f;
    private static final float PAD_Y = 12f;
    private static final float BULLET_INDENT = 11f;
    private static final float BULLET_RADIUS = 1.6f;
    private static final float HEADING_GAP = 5f;
    private static final float LINE_SPACING = 1.45f;
    private static final float PILL_PAD_X = 6f;
    private static final float PILL_GAP = 5f;
    private static final float PILL_HEIGHT = 1.55f;
    private static final float PILL_FILL_ALPHA = 0.18f;
    private static final float HERO_HEIGHT = 30f;
    private static final float HERO_GAP = 14f;
    private static final float STATE_HEIGHT = 120f;
    private static final float FILTER_GAP = 8f;
    private static final float HOVER_SPEED = 14f;
    private static final int MAX_NOTES = 12;
    private static final String ALL_VERSIONS_LABEL = "All versions";
    private static final int MAX_VERSIONS_LISTED = 3;
    private static final int FILTER_VISIBLE_ROWS = 5;
    private static final int RELEASE = 3;
    private static final int BETA = 1;
    private static final int ALPHA = 0;
    private final float uiScale = WLScreen.uiScaleFactor();
    private final GlassStyle cardStyle = ResetButton.knobStyle(ControlLayers.BASE);
    private final Changelog.State state;
    private final String installed;
    private final boolean sinceInstalled;
    private final List<Changelog.Release> releases;
    private final List<String> gameVersions;
    private final Changelog.Release latestForGame;
    private final boolean installedForGame;
    private final PillSelectorWidget<String> filterPill;
    private final Runnable onResize;
    private final List<Block> blocks = new ArrayList<>();
    private float[] hoverAnim = new float[0];
    private float linkX0, linkY0, linkX1, linkY1;
    private float linkHover;

    private enum Kind { HEADING, BULLET, BULLET_CONTINUED, TEXT }

    private record Line(Kind kind, String text, float offset) {}

    private record Block(Changelog.Release release, String type, int typeColor, boolean installed, boolean latest, String date, String versions, List<Line> lines, int hidden, float offset, float height) {}

    public ChangelogPaneWidget(final int x, final int y, final int width, final RowContext ctx, final Runnable onResize) {
        this(x, y, width, false, ctx, onResize);
    }

    public ChangelogPaneWidget(final int x, final int y, final int width, final boolean sinceInstalled) {
        this(x, y, width, sinceInstalled, null, null);
    }

    private ChangelogPaneWidget(final int x, final int y, final int width, final boolean sinceInstalled, final RowContext ctx, final Runnable onResize) {
        super(x, y, width, 1);
        this.state = WalksyLib.getInstance().getChangelog().state();
        this.installed = WalksyLib.getInstance().getChangelogPage().installedVersion();
        this.sinceInstalled = sinceInstalled;
        this.onResize = onResize;
        this.releases = WalksyLib.getInstance().getChangelog().releases();
        final List<Changelog.Release> forThisGame = WalksyLib.getInstance().getChangelogPage().forGameVersion(this.releases, ModrinthApi.gameVersion());
        this.latestForGame = forThisGame.isEmpty() ? (this.releases.isEmpty() ? null : this.releases.get(0)) : forThisGame.get(0);
        this.installedForGame = forThisGame.stream().anyMatch(release -> release.versionNumber().equals(this.installed));
        this.gameVersions = WalksyLib.getInstance().getChangelogPage().gameVersions(this.releases);
        final List<String> filters = new ArrayList<>();
        filters.add(ChangelogConfig.ALL_VERSIONS);
        filters.addAll(this.gameVersions);
        final boolean filterable = !sinceInstalled && ctx != null && ctx.onOpenPopoverAt() != null && filters.size() > 1;
        this.filterPill = filterable ? new PillSelectorWidget<>(this::filter, this::applyFilter, filters, ChangelogPaneWidget::filterLabel, PillSelectorWidget.defaultStyle().fadeKey(0xC4A9E104L), this.uiScale, PopoverHost.PANEL_LAYER, ctx.onOpenPopoverAt(), ctx.onClosePopover()).maxVisibleRows(FILTER_VISIBLE_ROWS) : null;
        final List<Changelog.Release> shown = sinceInstalled ? this.newerThanInstalled(this.releases) : this.filtered();
        if (this.state == Changelog.State.LOADED && !shown.isEmpty()) {
            this.layout(shown);
        } else {
            this.setHeight(Math.round(STATE_HEIGHT * this.uiScale));
        }
    }

    private static String filterLabel(final String version) {
        return version.equals(ChangelogConfig.ALL_VERSIONS) ? ALL_VERSIONS_LABEL : version;
    }

    private String filter() {
        return WalksyLib.getInstance().getChangelogPage().versionFilter(this.gameVersions);
    }

    private List<Changelog.Release> filtered() {
        final List<Changelog.Release> matching = WalksyLib.getInstance().getChangelogPage().forGameVersion(this.releases, this.filter());
        return matching.subList(0, Math.min(ChangelogConfig.MAX_RELEASES, matching.size()));
    }

    private void applyFilter(final String version) {
        if (version.equals(this.filter())) {
            return;
        }
        WalksyLib.getInstance().getChangelogPage().setVersionFilter(version);
        this.blocks.clear();
        this.layout(this.filtered());
        if (this.onResize != null) {
            this.onResize.run();
        }
    }

    private List<Changelog.Release> newerThanInstalled(final List<Changelog.Release> releases) {
        final String game = ModrinthApi.gameVersion();
        final List<Changelog.Release> forThisGame = releases.stream().filter(release -> release.gameVersions().isEmpty() || release.gameVersions().contains(game)).toList();
        final Version current = ModUpdate.parse(this.installed);
        final List<Changelog.Release> newer = current == null ? List.of() : forThisGame.stream().filter(release -> {
            final Version version = ModUpdate.parse(release.versionNumber());
            return version != null && version.compareTo(current) > 0;
        }).toList();
        if (!newer.isEmpty()) {
            return newer.subList(0, Math.min(ChangelogConfig.MAX_RELEASES, newer.size()));
        }
        if (!forThisGame.isEmpty()) {
            return forThisGame.subList(0, 1);
        }
        return releases.isEmpty() ? releases : releases.subList(0, 1);
    }

    private float s(final float v) {
        return v * this.uiScale;
    }

    private float cardX() {
        return this.getX() + this.s(CARD_LEFT);
    }

    private float cardW() {
        return Math.max(1f, this.getWidth() - this.s(CARD_LEFT));
    }

    private float versionSize() {
        return Type.TITLE3.size() * this.uiScale;
    }

    private float metaSize() {
        return Type.CAPTION1.size() * this.uiScale;
    }

    private float bodySize() {
        return Type.SUBHEADLINE.size() * this.uiScale;
    }

    private float headerHeight() {
        return this.versionSize() * 1.3f + this.metaSize() * 1.6f + this.s(6f);
    }

    private float footerHeight() {
        return this.metaSize() * 2.2f;
    }

    private float lineHeight() {
        return this.bodySize() * LINE_SPACING;
    }

    private void layout(final List<Changelog.Release> releases) {
        final float textW = this.cardW() - this.s(PAD_X) * 2f;
        final float bodySize = this.bodySize();
        float offset = this.sinceInstalled ? 0f : this.s(HERO_HEIGHT + HERO_GAP);
        for (int r = 0; r < releases.size(); r++) {
            final Changelog.Release release = releases.get(r);
            final List<ChangelogConfig.Note> notes = WalksyLib.getInstance().getChangelogPage().notes(release.changelog());
            final List<Line> lines = new ArrayList<>();
            float y = 0f;
            final int shown = Math.min(notes.size(), MAX_NOTES);
            for (int i = 0; i < shown; i++) {
                final ChangelogConfig.Note note = notes.get(i);
                if (note.heading()) {
                    if (i > 0) {
                        y += this.s(HEADING_GAP);
                    }
                    for (final String part : Fonts.SFPRO_DISPLAY_BOLD.wrap(note.text(), bodySize, textW)) {
                        lines.add(new Line(Kind.HEADING, part, y));
                        y += this.lineHeight();
                    }
                    continue;
                }
                final boolean bullet = note.bullet();
                final List<String> wrapped = Fonts.SFPRO_DISPLAY.wrap(note.text(), bodySize, bullet ? textW - this.s(BULLET_INDENT) : textW);
                for (int w = 0; w < wrapped.size(); w++) {
                    lines.add(new Line(bullet ? (w == 0 ? Kind.BULLET : Kind.BULLET_CONTINUED) : Kind.TEXT, wrapped.get(w), y));
                    y += this.lineHeight();
                }
            }
            if (notes.isEmpty()) {
                lines.add(new Line(Kind.TEXT, "No notes were written for this version.", y));
                y += this.lineHeight();
            }
            final int typeIndex = typeIndex(release.type());
            final float height = this.s(PAD_Y) * 2f + this.headerHeight() + y + this.footerHeight();
            this.blocks.add(new Block(release, WalksyLib.getInstance().getChangelogPage().typeName(release.type()), Theme.get().systemPalette()[typeIndex], release.versionNumber().equals(this.installed), r == 0, WalksyLib.getInstance().getChangelogPage().formatDate(release), versions(release.gameVersions()), lines, Math.max(0, notes.size() - shown), offset, height));
            offset += height + this.s(CARD_GAP);
        }
        this.hoverAnim = new float[this.blocks.size()];
        this.setHeight(Math.round(offset - this.s(CARD_GAP)));
    }

    private static int typeIndex(final String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "beta" -> BETA;
            case "alpha" -> ALPHA;
            default -> RELEASE;
        };
    }

    private static String versions(final List<String> versions) {
        if (versions.isEmpty()) {
            return "";
        }
        if (versions.size() <= MAX_VERSIONS_LISTED) {
            return "Minecraft " + String.join(", ", versions);
        }
        return "Minecraft " + versions.get(0) + " - " + versions.get(versions.size() - 1) + " (" + versions.size() + " versions)";
    }

    @Override
    protected void extract(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float delta) {
        final float alpha = Math.max(0f, Math.min(1f, this.getAlpha()));
        if (alpha <= 0.001f) {
            return;
        }
        if (this.blocks.isEmpty()) {
            this.extractState(graphics, mouseX, mouseY, alpha);
            return;
        }
        final float dt = WLRenderSystem.frameDeltaSeconds();
        if (!this.sinceInstalled) {
            this.extractHero(graphics, mouseX, mouseY, dt, alpha);
        }
        this.extractRail(graphics, alpha);
        for (int i = 0; i < this.blocks.size(); i++) {
            final Block block = this.blocks.get(i);
            final float x = this.cardX();
            final float y = this.getY() + block.offset();
            final float w = this.cardW();
            final boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + block.height();
            WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
            this.hoverAnim[i] = MathUtils.approach(this.hoverAnim[i], hovered ? 1f : 0f, HOVER_SPEED, dt);
            graphics.glass()
                .dimensions(x, y, w, block.height())
                .cornerRadius(this.s(CARD_CORNER))
                .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0xC4A9E106L + i))
                .hover(this.hoverAnim[i])
                .extract();
            final int index = i;
            ResetButton.lift(false, () -> this.extractCard(graphics, block, x, y, w, alpha, this.hoverAnim[index]));
        }
    }

    private void extractHero(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float dt, final float alpha) {
        final float y = this.getY();
        final float h = this.s(HERO_HEIGHT);
        final float centerY = y + h / 2f;
        final float size = Type.HEADLINE.size() * this.uiScale;
        final String label = this.installed.isEmpty() ? "WalksyLib" : "Installed " + this.installed;
        final float textX = this.getX();
        drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, label, textX, centerY, size, ColorUtils.withAlpha(Theme.get().label(), alpha));
        if (this.installedForGame && this.latestForGame != null) {
            final boolean current = this.latestForGame.versionNumber().equals(this.installed);
            final int color = Theme.get().systemPalette()[current ? RELEASE : BETA];
            this.pill(graphics, current ? "Up to date" : "Update available: " + this.latestForGame.versionNumber(), textX + Fonts.SFPRO_DISPLAY_BOLD.width(label, size) + this.s(PILL_GAP * 2f), centerY, this.metaSize(), color, alpha);
        }
        final String link = "Full changelog";
        final float linkSize = this.metaSize();
        final float linkW = Fonts.SFPRO_DISPLAY_MEDIUM.width(link, linkSize) + this.s(PILL_PAD_X * 2f + 4f);
        final float linkH = linkSize * 2.2f;
        this.linkX1 = this.getX() + this.getWidth();
        this.linkX0 = this.linkX1 - linkW;
        this.linkY0 = centerY - linkH / 2f;
        this.linkY1 = centerY + linkH / 2f;
        final boolean hovered = mouseX >= this.linkX0 && mouseX < this.linkX1 && mouseY >= this.linkY0 && mouseY < this.linkY1;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        this.linkHover = MathUtils.approach(this.linkHover, hovered ? 1f : 0f, HOVER_SPEED, dt);
        graphics.glass()
            .dimensions(this.linkX0, this.linkY0, linkW, linkH)
            .cornerRadius(linkH / 2f)
            .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0xC4A9E105L))
            .hover(this.linkHover)
            .extract();
        ResetButton.lift(false, () -> drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, link, this.linkX0 + (linkW - Fonts.SFPRO_DISPLAY_MEDIUM.width(link, linkSize)) / 2f, centerY, linkSize, ColorUtils.withAlpha(Theme.get().accent(), alpha)));
        if (this.filterPill != null) {
            this.filterPill.extract(graphics, alpha, this.linkX0 - this.s(FILTER_GAP), y, h, mouseX, mouseY, dt * 20f);
        }
    }

    private void extractRail(final WLGraphics2D graphics, final float alpha) {
        final float railX = this.getX() + this.s(RAIL_X);
        final float first = this.dotY(this.blocks.get(0));
        final float last = this.dotY(this.blocks.get(this.blocks.size() - 1));
        final float half = this.s(RAIL_WIDTH) / 2f;
        if (last > first) {
            graphics.fill(railX - half, first, railX + half, last, ColorUtils.withAlpha(Theme.get().separator(), alpha), null);
        }
        for (final Block block : this.blocks) {
            final float cy = this.dotY(block);
            if (block.installed()) {
                final float ring = this.s(RING_RADIUS);
                final int ringColor = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.30f), alpha);
                graphics.fillRoundedGradient(railX - ring, cy - ring, railX + ring, cy + ring, ringColor, ringColor, ringColor, ringColor, ring);
            }
            final float dot = this.s(DOT_RADIUS);
            final int dotColor = ColorUtils.withAlpha(block.installed() ? Theme.get().accent() : block.typeColor(), alpha);
            graphics.fillRoundedGradient(railX - dot, cy - dot, railX + dot, cy + dot, dotColor, dotColor, dotColor, dotColor, dot);
        }
    }

    private float dotY(final Block block) {
        return this.getY() + block.offset() + this.s(PAD_Y) + this.versionSize() * 0.65f;
    }

    private void extractCard(final WLGraphics2D graphics, final Block block, final float x, final float y, final float w, final float alpha, final float hover) {
        final float padX = this.s(PAD_X);
        final float padY = this.s(PAD_Y);
        final float left = x + padX;
        final float right = x + w - padX;
        final float versionSize = this.versionSize();
        final float metaSize = this.metaSize();
        final float bodySize = this.bodySize();
        final float versionCenter = y + padY + versionSize * 0.65f;
        drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, block.release().versionNumber(), left, versionCenter, versionSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        float pillX = left + Fonts.SFPRO_DISPLAY_BOLD.width(block.release().versionNumber(), versionSize) + this.s(PILL_GAP * 2f);
        pillX += this.pill(graphics, block.type(), pillX, versionCenter, metaSize, block.typeColor(), alpha) + this.s(PILL_GAP);
        if (block.installed()) {
            this.pill(graphics, "Installed", pillX, versionCenter, metaSize, Theme.get().accent(), alpha);
        } else if (block.latest()) {
            this.pill(graphics, "Latest", pillX, versionCenter, metaSize, Theme.get().systemPalette()[RELEASE], alpha);
        }
        drawText(graphics, Fonts.SFPRO_DISPLAY, block.date(), right - Fonts.SFPRO_DISPLAY.width(block.date(), metaSize), versionCenter, metaSize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
        if (!block.versions().isEmpty()) {
            final float versionsCenter = y + padY + versionSize * 1.3f + metaSize * 0.8f;
            drawText(graphics, Fonts.SFPRO_DISPLAY, Fonts.SFPRO_DISPLAY.truncate(block.versions(), metaSize, right - left), left, versionsCenter, metaSize, ColorUtils.withAlpha(Theme.get().tertiaryLabel(), alpha));
        }
        final float bodyTop = y + padY + this.headerHeight();
        final float lineH = this.lineHeight();
        final int bodyColor = ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha);
        final int headingColor = ColorUtils.withAlpha(Theme.get().label(), alpha);
        final float indent = this.s(BULLET_INDENT);
        for (final Line line : block.lines()) {
            final float center = bodyTop + line.offset() + lineH / 2f;
            switch (line.kind()) {
                case HEADING -> drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, line.text(), left, center, bodySize, headingColor);
                case BULLET -> {
                    final float dot = this.s(BULLET_RADIUS);
                    final float dotX = left + indent * 0.35f;
                    final int dotColor = ColorUtils.withAlpha(block.typeColor(), alpha);
                    graphics.fillRoundedGradient(dotX - dot, center - dot, dotX + dot, center + dot, dotColor, dotColor, dotColor, dotColor, dot);
                    drawText(graphics, Fonts.SFPRO_DISPLAY, line.text(), left + indent, center, bodySize, bodyColor);
                }
                case BULLET_CONTINUED -> drawText(graphics, Fonts.SFPRO_DISPLAY, line.text(), left + indent, center, bodySize, bodyColor);
                case TEXT -> drawText(graphics, Fonts.SFPRO_DISPLAY, line.text(), left, center, bodySize, bodyColor);
            }
        }
        final String footer = block.hidden() > 0 ? block.hidden() + " more " + (block.hidden() == 1 ? "line" : "lines") + " - Open on Modrinth" : "Open on Modrinth";
        final float footerCenter = y + block.height() - padY - this.footerHeight() / 2f + metaSize * 0.3f;
        final int footerColor = ColorUtils.withAlpha(ColorUtils.withAlpha(Theme.get().accent(), 0.75f + 0.25f * hover), alpha);
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, footer, left, footerCenter, metaSize, footerColor);
    }

    private float pill(final WLGraphics2D graphics, final String text, final float x, final float centerY, final float size, final int color, final float alpha) {
        final float textW = Fonts.SFPRO_DISPLAY_MEDIUM.width(text, size);
        final float w = textW + this.s(PILL_PAD_X) * 2f;
        final float h = size * PILL_HEIGHT;
        final int fill = ColorUtils.withAlpha(ColorUtils.withAlpha(color, PILL_FILL_ALPHA), alpha);
        graphics.fillRoundedGradient(x, centerY - h / 2f, x + w, centerY + h / 2f, fill, fill, fill, fill, h / 2f);
        drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, text, x + this.s(PILL_PAD_X), centerY, size, ColorUtils.withAlpha(color, alpha));
        return w;
    }

    private static void drawText(final WLGraphics2D graphics, final MsdfFont font, final String text, final float x, final float centerY, final float size, final int color) {
        font.extract(graphics, text, x, font.topForBaseline(centerY + size * 0.59043f * 0.5f, size), size, color);
    }

    private void extractState(final WLGraphics2D graphics, final int mouseX, final int mouseY, final float alpha) {
        final float centerX = this.getX() + this.getWidth() / 2f;
        final float centerY = this.getY() + this.getHeight() / 2f;
        final float titleSize = Type.HEADLINE.size() * this.uiScale;
        final float bodySize = this.bodySize();
        final String title = switch (this.state) {
            case FAILED -> "Could not reach Modrinth";
            case LOADED -> "No updates yet";
            default -> "Fetching updates...";
        };
        final String body = switch (this.state) {
            case FAILED -> WalksyLib.getInstance().getChangelog().error();
            case LOADED -> "WalksyLib has not published anything on Modrinth.";
            default -> "Looking up WalksyLib's recent releases.";
        };
        drawText(graphics, Fonts.SFPRO_DISPLAY_BOLD, title, centerX - Fonts.SFPRO_DISPLAY_BOLD.width(title, titleSize) / 2f, centerY - titleSize, titleSize, ColorUtils.withAlpha(Theme.get().label(), alpha));
        final String shownBody = Fonts.SFPRO_DISPLAY.truncate(body == null ? "" : body, bodySize, this.getWidth());
        drawText(graphics, Fonts.SFPRO_DISPLAY, shownBody, centerX - Fonts.SFPRO_DISPLAY.width(shownBody, bodySize) / 2f, centerY + bodySize * 0.4f, bodySize, ColorUtils.withAlpha(Theme.get().secondaryLabel(), alpha));
        if (this.state != Changelog.State.FAILED) {
            this.linkX0 = this.linkX1 = 0f;
            return;
        }
        final String retry = "Try Again";
        final float size = this.metaSize();
        final float w = Fonts.SFPRO_DISPLAY_MEDIUM.width(retry, size) + this.s(PILL_PAD_X * 2f + 6f);
        final float h = size * 2.2f;
        this.linkX0 = centerX - w / 2f;
        this.linkX1 = centerX + w / 2f;
        this.linkY0 = centerY + bodySize * 1.8f;
        this.linkY1 = this.linkY0 + h;
        final boolean hovered = mouseX >= this.linkX0 && mouseX < this.linkX1 && mouseY >= this.linkY0 && mouseY < this.linkY1;
        WLRenderSystem.cursors().requestIf(hovered, Cursors.Cursor.POINTER);
        this.linkHover = MathUtils.approach(this.linkHover, hovered ? 1f : 0f, HOVER_SPEED, WLRenderSystem.frameDeltaSeconds());
        graphics.glass()
            .dimensions(this.linkX0, this.linkY0, w, h)
            .cornerRadius(h / 2f)
            .style(this.cardStyle.scaled(this.uiScale).opacity(alpha).fadeKey(0xC4A9E105L))
            .hover(this.linkHover)
            .extract();
        ResetButton.lift(false, () -> drawText(graphics, Fonts.SFPRO_DISPLAY_MEDIUM, retry, centerX - Fonts.SFPRO_DISPLAY_MEDIUM.width(retry, size) / 2f, this.linkY0 + h / 2f, size, ColorUtils.withAlpha(Theme.get().accent(), alpha)));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final double mx = event.x();
        final double my = event.y();
        if (this.filterPill != null && !this.blocks.isEmpty() && !this.filterPill.isOpen() && this.filterPill.isOverPill(mx, my)) {
            this.filterPill.open();
            return true;
        }
        if (this.linkX1 > this.linkX0 && mx >= this.linkX0 && mx < this.linkX1 && my >= this.linkY0 && my < this.linkY1) {
            if (this.blocks.isEmpty()) {
                WalksyLib.getInstance().getChangelog().refresh();
            } else {
                WLClient.openUri(WalksyLib.getInstance().getChangelog().changelogUrl());
            }
            return true;
        }
        for (final Block block : this.blocks) {
            final float x = this.cardX();
            final float y = this.getY() + block.offset();
            if (mx >= x && mx < x + this.cardW() && my >= y && my < y + block.height()) {
                WLClient.openUri(WalksyLib.getInstance().getChangelog().versionUrl(block.release()));
                return true;
            }
        }
        return false;
    }
}
