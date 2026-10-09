package main.walksy.lib.core.commands;

import main.walksy.lib.core.platform.WLClient;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.gui.WalksyLibPanelScreen;
import main.walksy.lib.core.hud.HudEditScreen;
import main.walksy.lib.core.network.ServerOptOut;
import main.walksy.lib.core.network.ServerOptOuts;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

public final class WalksyLibCommands {
    private static final String TITLE = "WalksyLib";
    private static final TextColor PASTEL_PINK = TextColor.fromRgb(0xFFB6C1);

    private WalksyLibCommands() {
    }

    public static void register() {
        WalksyLibClientCommands.register(WalksyLibConstants.NAMESPACE, root ->
                root.executes(WalksyLibCommands::openPanel)
                        .then(WalksyLibClientCommands.literal("server")
                                .executes(WalksyLibCommands::listServerOptOuts)
                        )
        );
    }

    private static int listServerOptOuts(final CommandContext<FabricClientCommandSource> context) {
        final List<ServerOptOuts.Row> rows = WalksyLib.getInstance().getServerOptOuts().all();
        if (rows.isEmpty()) {
            send(context, Component.literal("No installed mod lets servers switch it off").withStyle(ChatFormatting.WHITE));
            return Command.SINGLE_SUCCESS;
        }
        send(context, Component.literal("Servers can switch off:").withStyle(ChatFormatting.GRAY));
        for (final ServerOptOuts.Row row : rows) {
            final ServerOptOut optOut = row.optOut();
            final MutableComponent line = Component.literal("  " + row.displayName()).withStyle(ChatFormatting.WHITE);
            if (optOut.allowsWholeMod()) {
                line.append(Component.literal(" [whole mod: ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(stateLabel(optOut.isModDisabled()))
                    .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
                    .append(reasonLabel(optOut.modReason()));
            }
            send(context, line);
            for (final String feature : optOut.features()) {
                send(context, Component.literal("    " + feature + " ").withStyle(ChatFormatting.GRAY).append(stateLabel(optOut.isFeatureDisabled(feature))).append(reasonLabel(optOut.featureReason(feature))));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Component reasonLabel(final String reason) {
        return reason.isBlank() ? Component.empty() : Component.literal(" - " + reason).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component stateLabel(final boolean disabled) {
        return disabled ? Component.literal("off (server)")
            .withStyle(ChatFormatting.RED) : Component.literal("on")
            .withStyle(ChatFormatting.GREEN);
    }

    private static int openPanel(final CommandContext<FabricClientCommandSource> context) {
        WLClient.schedule(() -> {
            final Screen parent = WLClient.screen();
            final WalksyLibPanelScreen panel = new WalksyLibPanelScreen(parent, TITLE);
            panel.openOnAllMods();
            WLClient.setScreen(panel);
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int openHud(final CommandContext<FabricClientCommandSource> context) {
        if (!HudEditScreen.hasAnything()) {
            send(context, Component.literal("No mod has registered a HUD element to move.").withStyle(ChatFormatting.WHITE));
            return 0;
        }
        WLClient.schedule(() -> WLClient.setScreen(new HudEditScreen(WLClient.screen())));
        return Command.SINGLE_SUCCESS;
    }

    private static void send(final CommandContext<FabricClientCommandSource> context, final Component message) {
        context.getSource().sendFeedback(prefix().append(message));
    }

    private static MutableComponent prefix() {
        return Component.empty()
            .append(Component.literal(TITLE).withStyle(Style.EMPTY.withColor(PASTEL_PINK).withBold(true)))
            .append(Component.literal(" | ").withStyle(ChatFormatting.WHITE));
    }
}
