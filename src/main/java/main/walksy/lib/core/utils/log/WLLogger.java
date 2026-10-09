package main.walksy.lib.core.utils.log;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

public final class WLLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[WalksyLib] ";
    private static final int MAX_CHANGES = 999;
    private final List<InternalLog> changes = new ArrayList<>();
    private volatile int revision;

    public void info(final Object message) {
        LOGGER.info(PREFIX + message);
    }

    public void warn(final Object message) {
        LOGGER.warn(PREFIX + message);
    }

    public void err(final Object message) {
        LOGGER.error(PREFIX + message);
    }

    public void err(final Object message, final Throwable cause) {
        LOGGER.error(PREFIX + message, cause);
    }

    public void change(final InternalLog change) {
        if (change == null) {
            return;
        }
        synchronized (this.changes) {
            this.changes.addFirst(change);
            while (this.changes.size() > MAX_CHANGES) {
                this.changes.removeLast();
            }
            this.revision++;
        }
    }

    public List<InternalLog> changes() {
        synchronized (this.changes) {
            return List.copyOf(this.changes);
        }
    }

    public int revision() {
        return this.revision;
    }

    public boolean hasChanges() {
        synchronized (this.changes) {
            return !this.changes.isEmpty();
        }
    }

    public void clearChanges() {
        synchronized (this.changes) {
            this.changes.clear();
            this.revision++;
        }
    }

}
