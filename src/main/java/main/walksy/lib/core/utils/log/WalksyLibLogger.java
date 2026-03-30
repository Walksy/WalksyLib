package main.walksy.lib.core.utils.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class WalksyLibLogger {
    static final Logger LOGGER = Logger.getLogger("WalksyLib");
    private static final List<InternalLog> logs = new ArrayList<>();

    public static void log(InternalLog log) {
        logs.add(log);
    }

    public static void info(Object message) {
        LOGGER.info((String) message);
    }

    public static void err(Object message) {
        LOGGER.info("[Error] " + message);
    }

    public static List<InternalLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
