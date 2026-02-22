package me.seetch.auraboard.module.tab;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import me.seetch.auraboard.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TabManager {

    private final AuraBoardPlugin plugin;
    private TabConfig config;
    private final TeamManager teamManager;
    private LuckPerms luckPerms;
    private final Map<UUID, String> activeSortingTeam = new ConcurrentHashMap<>();

    public TabManager(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getScoreboardLibrary().createTeamManager();

        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (Exception ignored) {
            }
        }

        reload();
    }

    public void reload() {
        config = loadConfig();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            teamManager.addPlayer(player);
            applySortingTeam(player);
        }
    }

    private TabConfig loadConfig() {
        TabConfig cfg = new TabConfig();
        ConfigurationSection sec = plugin.getConfigManager().getTabConfig();

        // Header
        ConfigurationSection headerSec = sec.getConfigurationSection("header");
        cfg.headerAnimation = headerSec != null && headerSec.getBoolean("animation", false);
        cfg.headerFrameInterval = headerSec != null ? headerSec.getInt("frame-interval", 15) : 15;
        cfg.headerFrames = headerSec != null ? headerSec.getStringList("frames") : new ArrayList<>();
        cfg.headerStatic = headerSec != null ? headerSec.getString("static", "") : "";

        // Footer
        ConfigurationSection footerSec = sec.getConfigurationSection("footer");
        cfg.footerAnimation = footerSec != null && footerSec.getBoolean("animation", false);
        cfg.footerFrameInterval = footerSec != null ? footerSec.getInt("frame-interval", 15) : 15;
        cfg.footerFrames = footerSec != null ? footerSec.getStringList("frames") : new ArrayList<>();
        cfg.footerStatic = footerSec != null ? footerSec.getString("static", "") : "";

        // Player format
        ConfigurationSection fmt = sec.getConfigurationSection("player-format");
        cfg.prefix = fmt != null ? fmt.getString("prefix", "") : "";
        cfg.playerName = fmt != null ? fmt.getString("name", "%player_name%") : "%player_name%";
        cfg.suffix = fmt != null ? fmt.getString("suffix", "") : "";

        // Sorting
        ConfigurationSection sort = sec.getConfigurationSection("sorting");
        cfg.sortingEnabled = sort != null && sort.getBoolean("enabled", false);
        cfg.sortingMode = sort != null ? sort.getString("mode", "NONE") : "NONE";
        cfg.secondarySort = sort != null ? sort.getString("secondary", "ALPHABETICAL") : "ALPHABETICAL";
        cfg.fallbackWeight = sort != null ? sort.getInt("fallback-weight", 0) : 0;
        cfg.groupWeights = new HashMap<>();
        if (sort != null) {
            ConfigurationSection gw = sort.getConfigurationSection("group-weights");
            if (gw != null) {
                for (String key : gw.getKeys(false)) {
                    cfg.groupWeights.put(key, gw.getInt(key, 0));
                }
            }
        }

        // Hide conditions
        cfg.hideConditions = Condition.loadList(
                sec.getConfigurationSection("hide-conditions") != null ? sec : null,
                "hide-conditions"
        );

        return cfg;
    }

    public void update(Player player) {
        teamManager.addPlayer(player);

        updateHeaderFooter(player);
        updatePlayerEntry(player);
    }

    private void updateHeaderFooter(Player player) {
        if (!plugin.getPlayerDataCache().isTabVisible(player.getUniqueId())) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }

        Component header = resolveAnimated(player, config.headerAnimation,
                config.headerFrames, config.headerFrameInterval, config.headerStatic);
        Component footer = resolveAnimated(player, config.footerAnimation,
                config.footerFrames, config.footerFrameInterval, config.footerStatic);

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Component resolveAnimated(Player player, boolean animated, List<String> frames,
                                      int interval, String staticText) {
        String raw;
        if (animated && !frames.isEmpty()) {
            int tick = plugin.getServer().getCurrentTick();
            int frame = (tick / interval) % frames.size();
            raw = frames.get(frame);
        } else {
            raw = staticText;
        }
        String resolved = plugin.getPlaceholderEngine().resolve(player, raw);
        return TextUtil.parse(resolved);
    }

    private void updatePlayerEntry(Player player) {
        // Check hide conditions
        boolean hidden = !config.hideConditions.isEmpty() &&
                plugin.getPlaceholderEngine().evaluateConditions(player, config.hideConditions, ConditionMode.ALL);

        // Apply team for sorting and nametag (done by NametagManager separately)
        // Here we just handle the display name in TAB
        String resolvedPrefix = plugin.getPlaceholderEngine().resolve(player, config.prefix);
        String resolvedName = plugin.getPlaceholderEngine().resolve(player, config.playerName);
        String resolvedSuffix = plugin.getPlaceholderEngine().resolve(player, config.suffix);

        Component displayName = Component.text()
                .append(TextUtil.parse(resolvedPrefix))
                .append(TextUtil.parse(resolvedName))
                .append(TextUtil.parse(resolvedSuffix))
                .build();

        player.playerListName(displayName);

        if (hidden) {
            // Tab-list hide via team approach or adventure API
            // scoreboard-library handles this via team visibility
        }

        // Sorting via team
        if (config.sortingEnabled && luckPerms != null) {
            applySortingTeam(player);
        }
    }

    private void applySortingTeam(Player player) {
        String group = "default";
        int weight = config.fallbackWeight;

        if (luckPerms != null) {
            var lpUser = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (lpUser != null) {
                group = lpUser.getPrimaryGroup();
                weight = config.groupWeights.getOrDefault(group, config.fallbackWeight);
            }
        }

        String teamName = String.format("ab_sort_%04d_%s", 9999 - weight, group.toLowerCase());

        String previousTeam = activeSortingTeam.get(player.getUniqueId());
        if (previousTeam != null && !previousTeam.equals(teamName)) {
            ScoreboardTeam old = teamManager.team(previousTeam);
            if (old != null) old.defaultDisplay().removeEntry(player.getName());
        }

        ScoreboardTeam team = teamManager.createIfAbsent(teamName);
        team.defaultDisplay().addEntry(player.getName());
        activeSortingTeam.put(player.getUniqueId(), teamName);
    }

    public void handleQuit(Player player) {
        player.playerListName(null);

        String teamName = activeSortingTeam.remove(player.getUniqueId());
        if (teamName != null) {
            ScoreboardTeam team = teamManager.team(teamName);
            if (team != null) team.defaultDisplay().removeEntry(player.getName());
        }

        teamManager.removePlayer(player);
    }

    public TabConfig getConfig() {
        return config;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}