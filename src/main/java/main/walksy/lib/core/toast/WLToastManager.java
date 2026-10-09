package main.walksy.lib.core.toast;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public final class WLToastManager {
    public static final int PANEL_LAYER = 1000;
    public static final int CONTENT_LAYER = 1001;
    public static final int MAX_VISIBLE = 4;

    private final ConcurrentLinkedQueue<WLToast> incoming = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<WLToast> dismissedHandles = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Object> dismissedIds = new ConcurrentLinkedQueue<>();
    private final Set<WLToast> showing = ConcurrentHashMap.newKeySet();
    private final Set<Object> showingIds = ConcurrentHashMap.newKeySet();
    private final List<ToastCard> live = new ArrayList<>();
    private final List<WLToast> waiting = new ArrayList<>();
    private volatile boolean dismissAllRequested;

    public ToastHandle submit(final WLToast toast) {
        final WLToast copy = toast.copy();
        this.showing.add(copy);
        if (copy.getId() != null) {
            this.showingIds.add(copy.getId());
        }
        this.incoming.add(copy);
        return new ToastHandle(this, copy);
    }

    public ToastHandle submit(final String title) {
        return this.submit(WLToast.of(title));
    }

    public ToastHandle submit(final String title, final String message) {
        return this.submit(WLToast.of(title, message));
    }

    public ToastHandle info(final String title) {
        return this.submit(WLToast.of(title).info());
    }

    public ToastHandle info(final String title, final String message) {
        return this.submit(WLToast.of(title, message).info());
    }

    public ToastHandle success(final String title) {
        return this.submit(WLToast.of(title).success());
    }

    public ToastHandle success(final String title, final String message) {
        return this.submit(WLToast.of(title, message).success());
    }

    public ToastHandle warning(final String title) {
        return this.submit(WLToast.of(title).warning());
    }

    public ToastHandle warning(final String title, final String message) {
        return this.submit(WLToast.of(title, message).warning());
    }

    public ToastHandle error(final String title) {
        return this.submit(WLToast.of(title).error());
    }

    public ToastHandle error(final String title, final String message) {
        return this.submit(WLToast.of(title, message).error());
    }

    public void dismiss(final Object id) {
        if (id != null) {
            this.dismissedIds.add(id);
        }
    }

    public void dismissAll() {
        this.dismissAllRequested = true;
    }

    public boolean isShowing(final Object id) {
        return id != null && this.showingIds.contains(id);
    }

    public int queuedCount() {
        return this.showing.size();
    }

    void dismissHandle(final WLToast toast) {
        this.dismissedHandles.add(toast);
    }

    boolean isHandleShowing(final WLToast toast) {
        return this.showing.contains(toast);
    }

    List<ToastCard> beginFrame() {
        this.drain();
        return this.live;
    }

    void endFrame() {
        this.retire();
    }

    private void drain() {
        for (WLToast toast = this.incoming.poll(); toast != null; toast = this.incoming.poll()) {
            if (!this.replaceExisting(toast)) {
                this.waiting.add(toast);
            }
        }
        if (this.dismissAllRequested) {
            this.dismissAllRequested = false;
            for (final ToastCard card : this.live) {
                card.requestDismiss();
            }
            this.forget(this.waiting);
            this.waiting.clear();
        }
        for (Object next = this.dismissedIds.poll(); next != null; next = this.dismissedIds.poll()) {
            final Object id = next;
            for (final ToastCard card : this.live) {
                if (id.equals(card.id())) {
                    card.requestDismiss();
                }
            }
            this.dropWaiting(toast -> id.equals(toast.getId()));
        }
        for (WLToast toast = this.dismissedHandles.poll(); toast != null; toast = this.dismissedHandles.poll()) {
            for (final ToastCard card : this.live) {
                if (card.spec() == toast) {
                    card.requestDismiss();
                }
            }
            final WLToast target = toast;
            this.dropWaiting(candidate -> candidate == target);
        }
        while (this.live.size() < MAX_VISIBLE && !this.waiting.isEmpty()) {
            this.live.add(new ToastCard(this.waiting.removeFirst()));
        }
    }

    private boolean replaceExisting(final WLToast toast) {
        final Object id = toast.getId();
        if (id == null) {
            return false;
        }
        for (final ToastCard card : this.live) {
            if (id.equals(card.id())) {
                this.forget(card.spec(), false);
                card.replaceWith(toast);
                return true;
            }
        }
        for (int i = 0; i < this.waiting.size(); i++) {
            final WLToast queued = this.waiting.get(i);
            if (id.equals(queued.getId())) {
                this.waiting.set(i, toast);
                this.forget(queued, false);
                return true;
            }
        }
        return false;
    }

    private void dropWaiting(final Predicate<WLToast> match) {
        final Iterator<WLToast> pending = this.waiting.iterator();
        while (pending.hasNext()) {
            final WLToast queued = pending.next();
            if (match.test(queued)) {
                pending.remove();
                this.forget(queued, true);
            }
        }
    }

    private void retire() {
        final Iterator<ToastCard> cards = this.live.iterator();
        while (cards.hasNext()) {
            final ToastCard card = cards.next();
            if (!card.isGone()) {
                continue;
            }
            cards.remove();
            this.forget(card.spec(), true);
        }
    }

    private void forget(final Iterable<WLToast> toasts) {
        for (final WLToast toast : toasts) {
            this.forget(toast, true);
        }
    }

    private void forget(final WLToast toast, final boolean notify) {
        this.showing.remove(toast);
        final Object id = toast.getId();
        if (id != null && !this.stillCarries(id)) {
            this.showingIds.remove(id);
        }
        final Runnable onDismissed = toast.getOnDismissed();
        if (notify && onDismissed != null) {
            onDismissed.run();
        }
    }

    private boolean stillCarries(final @Nullable Object id) {
        for (final WLToast toast : this.showing) {
            if (id != null && id.equals(toast.getId())) {
                return true;
            }
        }
        return false;
    }
}
