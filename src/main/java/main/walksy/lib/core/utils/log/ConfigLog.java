package main.walksy.lib.core.utils.log;

public class ConfigLog extends InternalLog {

    public String configName, optionName, oldValue, newValue;

    public ConfigLog(String configName, String optionName, String oldValue, String newValue)
    {
        this.configName = configName;
        this.optionName = optionName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
