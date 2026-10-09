package main.walksy.lib.api.event;

import main.walksy.lib.api.event.impl.*;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class WalksyLibEvents {
    public static final Event<ClientReadyEvent> CLIENT_READY = EventFactory.createArrayBacked(ClientReadyEvent.class, callbacks -> (minecraft) -> {
        for (final ClientReadyEvent callback : callbacks) {
            callback.ready(minecraft);
        }
    });
    public static final Event<ClientTickEvent> START_CLIENT_TICK = EventFactory.createArrayBacked(ClientTickEvent.class, callbacks -> (minecraft) -> {
        for (final ClientTickEvent callback : callbacks) {
            callback.tick(minecraft);
        }
    });
    public static final Event<ClientTickEvent> END_CLIENT_TICK = EventFactory.createArrayBacked(ClientTickEvent.class, callbacks -> (minecraft) -> {
        for (final ClientTickEvent callback : callbacks) {
            callback.tick(minecraft);
        }
    });
    public static final Event<LocalPlayerTickEvent> START_LOCAL_PLAYER_TICK = EventFactory.createArrayBacked(LocalPlayerTickEvent.class, callbacks -> (player) -> {
        for (final LocalPlayerTickEvent callback : callbacks) {
            callback.tick(player);
        }
    });
    public static final Event<LocalPlayerTickEvent> END_LOCAL_PLAYER_TICK = EventFactory.createArrayBacked(LocalPlayerTickEvent.class, callbacks -> (player) -> {
        for (final LocalPlayerTickEvent callback : callbacks) {
            callback.tick(player);
        }
    });
    public static final Event<ClientJoinServerEvent> CLIENT_JOIN_SERVER = EventFactory.createArrayBacked(ClientJoinServerEvent.class, callbacks -> (packet) -> {
        for (final ClientJoinServerEvent callback : callbacks) {
            callback.join(packet);
        }
    });
    public static final Event<ClientLeftServerEvent> CLIENT_LEFT_SERVER = EventFactory.createArrayBacked(ClientLeftServerEvent.class, callbacks -> () -> {
        for (final ClientLeftServerEvent callback : callbacks) {
            callback.leave();
        }
    });
    public static final Event<RenderHudEvent> RENDER_HUD = EventFactory.createArrayBacked(RenderHudEvent.class, callbacks -> (graphics, deltaTracker) -> {
        for (final RenderHudEvent callback : callbacks) {
            callback.render(graphics, deltaTracker);
        }
    });
    public static final Event<Render3DEvent> RENDER_3D = EventFactory.createArrayBacked(Render3DEvent.class, callbacks -> (graphics) -> {
        for (final Render3DEvent callback : callbacks) {
            callback.render(graphics);
        }
    });
    public static final Event<ReceivePacketEvent> START_RECEIVE_PACKET = EventFactory.createArrayBacked(ReceivePacketEvent.class, callbacks -> (packet, listener, ensureRunningOnSameThread, cancellable) -> {
        for (final ReceivePacketEvent callback : callbacks) {
            callback.receive(packet, listener, ensureRunningOnSameThread, cancellable);
            if (cancellable.isCancelled()) {
                break;
            }
        }
    });
    public static final Event<ReceivePacketEvent> END_RECEIVE_PACKET = EventFactory.createArrayBacked(ReceivePacketEvent.class, callbacks -> (packet, listener, ensureRunningOnSameThread, cancellable) -> {
        for (final ReceivePacketEvent callback : callbacks) {
            callback.receive(packet, listener, ensureRunningOnSameThread, cancellable);
        }
    });
    public static final Event<FileDropEvent> FILE_DROP = EventFactory.createArrayBacked(FileDropEvent.class, callbacks -> (files, failedCount) -> {
        for (final FileDropEvent callback : callbacks) {
            callback.dropFile(files, failedCount);
        }
    });
    public static final Event<AttackEntityEvent> ATTACK_ENTITY = EventFactory.createArrayBacked(AttackEntityEvent.class, callbacks -> (local, target) -> {
        for (final AttackEntityEvent callback : callbacks) {
            callback.attack(local, target);
        }
    });
    public static final Event<ShieldDisableEvent> SHIELD_DISABLE = EventFactory.createArrayBacked(ShieldDisableEvent.class, callbacks -> (player) -> {
        for (final ShieldDisableEvent callback : callbacks) {
            callback.disable(player);
        }
    });
    public static final Event<KeyInputEvent> KEY_INPUT = EventFactory.createArrayBacked(KeyInputEvent.class, callbacks -> (code, action, scancode, modifiers) -> {
        for (final KeyInputEvent callback : callbacks) {
            callback.key(code, action, scancode, modifiers);
        }
    });
    public static final Event<MouseButtonInputEvent> MOUSE_BUTTON_INPUT = EventFactory.createArrayBacked(MouseButtonInputEvent.class, callbacks -> (button, action, modifiers) -> {
        for (final MouseButtonInputEvent callback : callbacks) {
            callback.button(button, action, modifiers);
        }
    });
    public static final Event<MouseScrollInputEvent> MOUSE_SCROLL_INPUT = EventFactory.createArrayBacked(MouseScrollInputEvent.class, callbacks -> (xOffset, yOffset) -> {
        for (final MouseScrollInputEvent callback : callbacks) {
            callback.scroll(xOffset, yOffset);
        }
    });

    private WalksyLibEvents() {
    }
}
