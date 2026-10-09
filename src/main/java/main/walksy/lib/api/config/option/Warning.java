package main.walksy.lib.api.config.option;

public record Warning(String title, String message, Runnable onYes, Runnable onNo) {
}
