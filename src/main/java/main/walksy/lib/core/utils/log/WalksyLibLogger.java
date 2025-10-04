package main.walksy.lib.core.utils.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class WalksyLibLogger {
    final Logger LOGGER = Logger.getLogger("WalksyLib");
    private final List<InternalLog> logs;
    private int revision = 0;

    public WalksyLibLogger()
    {
        this.logs = new ArrayList<>();
    }

    public void log(InternalLog log)
    {
        this.logs.add(log);
        revision++;
    }

    public void info(Object message)
    {
        LOGGER.info((String) message);
    }

    public void err(Object message)
    {
        LOGGER.info("[Error] " + message);
    }

    public List<InternalLog> getLogs()
    {
        return Collections.unmodifiableList(this.logs);
    }
}
