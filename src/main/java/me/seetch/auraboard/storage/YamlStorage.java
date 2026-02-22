package me.seetch.auraboard.storage;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.cache.PlayerDataCache;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class YamlStorage implements StorageProvider {

    private final AuraBoardPlugin plugin;
    private File file;
    private YamlConfiguration config;

    public YamlStorage(AuraBoardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void loadPlayer(UUID uuid, Consumer<PlayerDataCache.PlayerData> callback) {
        Map<String, Object> map = new HashMap<>();
        if (config.contains(uuid.toString())) {
            org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection(uuid.toString());
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    map.put(key, sec.get(key));
                }
            }
        }
        callback.accept(PlayerDataCache.PlayerData.fromMap(map));
    }

    @Override
    public void savePlayerAsync(UUID uuid, PlayerDataCache.PlayerData data) {
        if (data == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Object> map = data.toMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                config.set(uuid + "." + entry.getKey(), entry.getValue());
            }
            try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
        });
    }

    @Override
    public void close() {}
}