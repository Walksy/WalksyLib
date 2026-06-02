package main.walksy.lib.core.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class WalksyLibTeamManager {

    private static final Map<String, Team> PLAYER_TEAMS = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DEST_DIR = FabricLoader.getInstance().getConfigDir().resolve("WalksyLib");
    private static final Path FILE_PATH = DEST_DIR.resolve("global_teams.json");
    private static final File FILE = FILE_PATH.toFile();

    public void addToTeam(final String player, final Team team) {
        PLAYER_TEAMS.put(player, team);
        this.save();
    }

    public void removeFromRegistry(final String player) {
        PLAYER_TEAMS.remove(player);
        this.save();
    }

    public Team getPlayerTeam(final String player) {
        return PLAYER_TEAMS.getOrDefault(player, Team.NONE);
    }

    public Map<String, Team> getTeams() {
        return PLAYER_TEAMS;
    }

    public void save() {
        try {
            if (!Files.exists(DEST_DIR)) {
                Files.createDirectories(DEST_DIR);
            }
            try (final Writer writer = new FileWriter(FILE)) {
                GSON.toJson(PLAYER_TEAMS, writer);
            }
        } catch (IOException e) {
            WalksyLibLogger.err(e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(FILE_PATH)) return;
        try (final Reader reader = new FileReader(FILE)) {
            final Map<String, Team> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Team>>(){}.getType());
            if (loaded != null) {
                PLAYER_TEAMS.clear();
                PLAYER_TEAMS.putAll(loaded);
            }
        } catch (IOException e) {
            WalksyLibLogger.err(e.getMessage());
        }
    }
}
