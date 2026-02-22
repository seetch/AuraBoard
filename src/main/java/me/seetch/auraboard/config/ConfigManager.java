package me.seetch.auraboard.config;

import me.seetch.auraboard.AuraBoardPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final AuraBoardPlugin plugin;

    private FileConfiguration mainConfig;
    private FileConfiguration scoreboardConfig;
    private FileConfiguration tabConfig;
    private FileConfiguration nametagConfig;
    private FileConfiguration belownameConfig;

    public ConfigManager(AuraBoardPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        scoreboardConfig = loadFile("scoreboard.yml");
        tabConfig = loadFile("tab.yml");
        nametagConfig = loadFile("nametag.yml");
        belownameConfig = loadFile("belowname.yml");
    }

    private FileConfiguration loadFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getMainConfig() { return mainConfig; }
    public FileConfiguration getScoreboardConfig() { return scoreboardConfig; }
    public FileConfiguration getTabConfig() { return tabConfig; }
    public FileConfiguration getNametagConfig() { return nametagConfig; }
    public FileConfiguration getBelownameConfig() { return belownameConfig; }
}