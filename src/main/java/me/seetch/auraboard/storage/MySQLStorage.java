package me.seetch.auraboard.storage;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.cache.PlayerDataCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.UUID;
import java.util.function.Consumer;

public class MySQLStorage implements StorageProvider {

    private final AuraBoardPlugin plugin;
    private HikariDataSource dataSource;

    public MySQLStorage(AuraBoardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        ConfigurationSection mysql = plugin.getConfigManager().getMainConfig()
                .getConfigurationSection("storage.mysql");
        if (mysql == null) {
            plugin.getLogger().severe("MySQL config missing!");
            return;
        }

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
                mysql.getString("host", "localhost"),
                mysql.getInt("port", 3306),
                mysql.getString("database", "auraboard")));
        hc.setUsername(mysql.getString("user", "root"));
        hc.setPassword(mysql.getString("password", ""));
        hc.setMaximumPoolSize(mysql.getInt("pool-size", 5));
        hc.setConnectionTimeout(10000);
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(hc);
        createTable();
    }

    private void createTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    scoreboard_visible TINYINT(1) DEFAULT 1,
                    tab_visible TINYINT(1) DEFAULT 1,
                    belowname_visible TINYINT(1) DEFAULT 1,
                    forced_scoreboard VARCHAR(64)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("MySQL table creation failed: " + e.getMessage());
        }
    }

    @Override
    public void loadPlayer(UUID uuid, Consumer<PlayerDataCache.PlayerData> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerDataCache.PlayerData data = new PlayerDataCache.PlayerData();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
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
                plugin.getLogger().warning("MySQL load error: " + e.getMessage());
            }
            callback.accept(data);
        });
    }

    @Override
    public void savePlayerAsync(UUID uuid, PlayerDataCache.PlayerData data) {
        if (data == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO player_data (uuid, scoreboard_visible, tab_visible, belowname_visible, forced_scoreboard)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        scoreboard_visible = VALUES(scoreboard_visible),
                        tab_visible = VALUES(tab_visible),
                        belowname_visible = VALUES(belowname_visible),
                        forced_scoreboard = VALUES(forced_scoreboard)
                 """)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, data.scoreboardVisible ? 1 : 0);
                ps.setInt(3, data.tabVisible ? 1 : 0);
                ps.setInt(4, data.belownameVisible ? 1 : 0);
                ps.setString(5, data.forcedScoreboardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL save error: " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }
}