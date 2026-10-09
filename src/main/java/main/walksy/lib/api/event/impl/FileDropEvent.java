package main.walksy.lib.api.event.impl;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface FileDropEvent {
    void dropFile(List<Path> files, int failedCount);
}
