package me.seetch.auraboard;

import me.seetch.auraboard.api.AuraBoardAPI;
import me.seetch.auraboard.cache.PlayerDataCache;
import me.seetch.auraboard.command.AuraBoardCommand;
import me.seetch.auraboard.config.ConfigManager;
import me.seetch.auraboard.hook.FloodgateHook;
import me.seetch.auraboard.module.belowname.BelownameManager;
import me.seetch.auraboard.module.nametag.NametagManager;
import me.seetch.auraboard.module.scoreboard.ScoreboardManager;
import me.seetch.auraboard.module.tab.TabManager;
import me.seetch.auraboard.placeholder.PlaceholderEngine;
import me.seetch.auraboard.storage.MySQLStorage;
import me.seetch.auraboard.storage.SQLiteStorage;
import me.seetch.auraboard.storage.StorageProvider;
import me.seetch.auraboard.storage.YamlStorage;
import me.seetch.auraboard.task.UpdateTask;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AuraBoardPlugin extends JavaPlugin implements Listener {

    private static AuraBoardPlugin instance;

    private ScoreboardLibrary scoreboardLibrary;
    private ConfigManager configManager;
    private PlaceholderEngine placeholderEngine;
    private PlayerDataCache playerDataCache;
    private StorageProvider storageProvider;

    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;
    private NametagManager nametagManager;
    private BelownameManager belownameManager;

    private UpdateTask updateTask;
    private AuraBoardAPI api;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfigs();

        // Init scoreboard-library
        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            getLogger().warning("No packet adapter available, using noop library.");
            scoreboardLibrary = new NoopScoreboardLibrary();
        }

        configManager = new ConfigManager(this);
        configManager.loadAll();

        placeholderEngine = new PlaceholderEngine(this);
        playerDataCache = new PlayerDataCache();

        // Init storage
        storageProvider = createStorage();
        storageProvider.init();

        // Init floodgate hook
        FloodgateHook.init(this);

        // Init modules
        if (configManager.getMainConfig().getBoolean("modules.scoreboard", true)) {
            scoreboardManager = new ScoreboardManager(this);
        }
        if (configManager.getMainConfig().getBoolean("modules.tab", true)) {
            tabManager = new TabManager(this);
        }
        if (configManager.getMainConfig().getBoolean("modules.nametag", true)) {
            nametagManager = new NametagManager(this);
        }
        if (configManager.getMainConfig().getBoolean("modules.belowname", true)) {
            belownameManager = new BelownameManager(this);
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register commands
        AuraBoardCommand commandHandler = new AuraBoardCommand(this);
        getCommand("auraboard").setExecutor(commandHandler);
        getCommand("auraboard").setTabCompleter(commandHandler);
        getCommand("scoreboard").setExecutor(commandHandler);
        getCommand("tab").setExecutor(commandHandler);
        getCommand("belowname").setExecutor(commandHandler);

        // Init for online players (reload scenario)
        for (Player player : Bukkit.getOnlinePlayers()) {
            onPlayerJoinInternal(player);
        }

        // Start update task
        int interval = configManager.getMainConfig().getInt("update-interval", 20);
        updateTask = new UpdateTask(this);
        updateTask.runTaskTimerAsynchronously(this, interval, interval);

        // API
        api = new AuraBoardAPI(this);

        // BEDROCK CONDITION
        api.registerCondition("IS_BEDROCK", (player, value) -> {
            boolean isBedrock = FloodgateHook.getInstance().isFloodgatePlayer(
                    player.getUniqueId(), player.getName()
            );
            return isBedrock == Boolean.parseBoolean(value);
        });

        getLogger().info("AuraBoard v" + getDescription().getVersion() + " enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) updateTask.cancel();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataCache.remove(player.getUniqueId());
            if (scoreboardManager != null) scoreboardManager.handleQuit(player);
            if (tabManager != null) tabManager.handleQuit(player);
            if (nametagManager != null) nametagManager.handleQuit(player);
            if (belownameManager != null) belownameManager.handleQuit(player);
        }

        if (scoreboardLibrary != null) scoreboardLibrary.close();
        if (storageProvider != null) storageProvider.close();

        getLogger().info("AuraBoard disabled.");
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig();
        saveResourceIfNotExists("scoreboard.yml");
        saveResourceIfNotExists("tab.yml");
        saveResourceIfNotExists("nametag.yml");
        saveResourceIfNotExists("belowname.yml");
    }

    private void saveResourceIfNotExists(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    public void reload() {
        if (updateTask != null) updateTask.cancel();

        configManager.loadAll();
        placeholderEngine.invalidateAll();

        if (scoreboardManager != null) scoreboardManager.reload();
        if (tabManager != null) tabManager.reload();
        if (nametagManager != null) nametagManager.reload();
        if (belownameManager != null) belownameManager.reload();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataCache.resetActiveModules(player.getUniqueId());
            applyAll(player);
        }

        int interval = configManager.getMainConfig().getInt("update-interval", 20);
        updateTask = new UpdateTask(this);
        updateTask.runTaskTimerAsynchronously(this, interval, interval);

        getLogger().info("AuraBoard reloaded.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load storage state async, then apply on main thread
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            storageProvider.loadPlayer(player.getUniqueId(), data -> {
                playerDataCache.init(player.getUniqueId(), data);
                Bukkit.getScheduler().runTask(this, () -> onPlayerJoinInternal(player));
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        storageProvider.savePlayerAsync(player.getUniqueId(), playerDataCache.getData(player.getUniqueId()));
        if (scoreboardManager != null) scoreboardManager.handleQuit(player);
        if (tabManager != null) tabManager.handleQuit(player);
        if (nametagManager != null) nametagManager.handleQuit(player);
        if (belownameManager != null) belownameManager.handleQuit(player);
        placeholderEngine.invalidatePlayer(player.getUniqueId());
        playerDataCache.remove(player.getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        placeholderEngine.invalidatePlayer(player.getUniqueId());
        // Re-apply modules that may change per world
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> applyAll(player));
    }

    private void onPlayerJoinInternal(Player player) {
        applyAll(player);
    }

    public void applyAll(Player player) {
        if (scoreboardManager != null) scoreboardManager.update(player);
        if (tabManager != null) tabManager.update(player);
        if (nametagManager != null) nametagManager.update(player);
        if (belownameManager != null) belownameManager.update(player);
    }

    private StorageProvider createStorage() {
        String type = configManager.getMainConfig().getString("storage.type", "YAML");
        return switch (type.toUpperCase()) {
            case "MYSQL" -> new MySQLStorage(this);
            case "SQLITE" -> new SQLiteStorage(this);
            default -> new YamlStorage(this);
        };
    }

    public static AuraBoardPlugin getInstance() {
        return instance;
    }

    public ScoreboardLibrary getScoreboardLibrary() {
        return scoreboardLibrary;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlaceholderEngine getPlaceholderEngine() {
        return placeholderEngine;
    }

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public BelownameManager getBelownameManager() {
        return belownameManager;
    }

    public AuraBoardAPI getApi() {
        return api;
    }
}