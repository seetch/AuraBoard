package me.seetch.auraboard.storage;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.cache.PlayerDataCache;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.function.Consumer;

public class SQLiteStorage implements StorageProvider {

    private final AuraBoardPlugin plugin;
    private Connection connection;

    public SQLiteStorage(AuraBoardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "auraboard.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("SQLite init failed: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    scoreboard_visible INTEGER DEFAULT 1,
                    tab_visible INTEGER DEFAULT 1,
                    belowname_visible INTEGER DEFAULT 1,
                    forced_scoreboard TEXT
                )
            """);
        }
    }

    @Override
    public void loadPlayer(UUID uuid, Consumer<PlayerDataCache.PlayerData> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerDataCache.PlayerData data = new PlayerDataCache.PlayerData();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    data.scoreboardVisible = rs.getInt("scoreboard_visible") == 1;
                    data.tabVisible = rs.getInt("tab_visible") == 1;
                    data.belownameVisible = rs.getInt("belowname_visible") == 1;
                    data.forcedScoreboardId = rs.getString("forced_scoreboard");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("SQLite load error: " + e.getMessage());
            }
            callback.accept(data);
        });
    }

    @Override
    public void savePlayerAsync(UUID uuid, PlayerDataCache.PlayerData data) {
        if (data == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_data (uuid, scoreboard_visible, tab_visible, belowname_visible, forced_scoreboard)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    scoreboard_visible = excluded.scoreboard_visible,
                    tab_visible = excluded.tab_visible,
                    belowname_visible = excluded.belowname_visible,
                    forced_scoreboard = excluded.forced_scoreboard
            """)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, data.scoreboardVisible ? 1 : 0);
                ps.setInt(3, data.tabVisible ? 1 : 0);
                ps.setInt(4, data.belownameVisible ? 1 : 0);
                ps.setString(5, data.forcedScoreboardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("SQLite save error: " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}