package me.seetch.auraboard.module.scoreboard;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import me.seetch.auraboard.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final AuraBoardPlugin plugin;
    private final Map<UUID, Sidebar> activeSidebars = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> titleFrameCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pageTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pageCounters = new ConcurrentHashMap<>();

    private List<ScoreboardConfig> configs = new ArrayList<>();

    public ScoreboardManager(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        configs = loadConfigs();
        configs.sort(Comparator.comparingInt((ScoreboardConfig c) -> c.priority()).reversed());
    }

    private List<ScoreboardConfig> loadConfigs() {
        List<ScoreboardConfig> list = new ArrayList<>();
        ConfigurationSection root = plugin.getConfigManager().getScoreboardConfig()
                .getConfigurationSection("scoreboards");
        if (root == null) return list;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            int priority = sec.getInt("priority", 0);
            List<String> worlds = sec.getStringList("worlds");

            List<Condition> conditions = Condition.loadList(sec, "conditions");
            String modeStr = sec.getString("condition-mode", "ALL");
            ConditionMode mode;
            try {
                mode = ConditionMode.valueOf(modeStr.toUpperCase());
            } catch (Exception e) {
                mode = ConditionMode.ALL;
            }

            ConfigurationSection titleSec = sec.getConfigurationSection("title");
            boolean titleAnim = titleSec != null && titleSec.getBoolean("animation", false);
            String titleStatic = titleSec != null ? titleSec.getString("static", "<bold>AuraBoard") : "<bold>AuraBoard";
            List<String> titleFrames = titleSec != null ? titleSec.getStringList("frames") : new ArrayList<>();
            int titleFrameInterval = titleSec != null ? titleSec.getInt("frame-interval", 10) : 10;

            List<String> lines = sec.getStringList("lines");

            ConfigurationSection pagesSec = sec.getConfigurationSection("pages");
            boolean pagesEnabled = pagesSec != null && pagesSec.getBoolean("enabled", false);
            int pagesInterval = pagesSec != null ? pagesSec.getInt("interval", 100) : 100;

            Map<Integer, List<String>> pages = new LinkedHashMap<>();
            pages.put(1, lines);

            if (pagesSec != null) {
                for (String key : pagesSec.getKeys(false)) {
                    if (key.equals("enabled") || key.equals("interval")) continue;
                    try {
                        int pageNum = Integer.parseInt(key);
                        List<String> pageLines = pagesSec.getStringList(key);
                        if (!pageLines.isEmpty()) pages.put(pageNum, pageLines);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            list.add(new ScoreboardConfig(
                    id, priority, worlds, conditions, mode,
                    titleAnim, titleStatic, titleFrames, titleFrameInterval,
                    pages, pagesEnabled, pagesInterval
            ));
        }
        return list;
    }

    public void update(Player player) {
        if (!plugin.getPlayerDataCache().isScoreboardVisible(player.getUniqueId())) {
            removeSidebar(player);
            return;
        }

        ScoreboardConfig target = resolveConfig(player);
        if (target == null) {
            removeSidebar(player);
            return;
        }

        String currentId = plugin.getPlayerDataCache().getActiveScoreboard(player.getUniqueId());

        if (!target.id().equals(currentId)) {
            removeSidebar(player);
            plugin.getPlayerDataCache().setActiveScoreboard(player.getUniqueId(), target.id());
            titleFrameCounters.put(player.getUniqueId(), 0);
            pageCounters.put(player.getUniqueId(), 0);
            pageTimers.put(player.getUniqueId(), System.currentTimeMillis());
            Sidebar sidebar = plugin.getScoreboardLibrary().createSidebar();
            sidebar.addPlayer(player);
            activeSidebars.put(player.getUniqueId(), sidebar);
        }

        Sidebar sidebar = activeSidebars.get(player.getUniqueId());
        if (sidebar == null) return;

        Component title = resolveTitle(player, target);
        sidebar.title(title);

        List<String> rawLines = resolveLines(player, target);
        for (int i = 0; i < rawLines.size(); i++) {
            String rawLine = rawLines.get(i);
            String resolved = plugin.getPlaceholderEngine().resolve(player, rawLine);
            Component line = TextUtil.parse(resolved);
            sidebar.line(i, line);
        }
    }

    private Component resolveTitle(Player player, ScoreboardConfig cfg) {
        if (!cfg.titleAnimation() || cfg.titleFrames().isEmpty()) {
            String resolved = plugin.getPlaceholderEngine().resolve(player, cfg.titleStatic());
            return TextUtil.parse(resolved);
        }
        int tick = plugin.getServer().getCurrentTick();
        int frame = (tick / cfg.titleFrameInterval()) % cfg.titleFrames().size();
        String resolved = plugin.getPlaceholderEngine().resolve(player, cfg.titleFrames().get(frame));
        return TextUtil.parse(resolved);
    }

    private List<String> resolveLines(Player player, ScoreboardConfig cfg) {
        if (!cfg.pagesEnabled() || cfg.pages().size() <= 1) {
            return cfg.pages().getOrDefault(1, List.of());
        }

        List<Integer> pageKeys = new ArrayList<>(cfg.pages().keySet());
        Collections.sort(pageKeys);
        int totalPages = pageKeys.size();

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        long lastChange = pageTimers.getOrDefault(uuid, now);
        long intervalMs = (long) cfg.pagesInterval() * 50L;

        if (now - lastChange >= intervalMs) {
            pageTimers.put(uuid, now);
            pageCounters.put(uuid, (pageCounters.getOrDefault(uuid, 0) + 1) % totalPages);
        }

        int index = pageCounters.getOrDefault(uuid, 0);
        int pageKey = pageKeys.get(index);
        return cfg.pages().getOrDefault(pageKey, List.of());
    }

    private ScoreboardConfig resolveConfig(Player player) {
        String forced = plugin.getPlayerDataCache().getForcedScoreboard(player.getUniqueId());
        if (forced != null) {
            return configs.stream().filter(c -> c.id().equals(forced)).findFirst().orElse(null);
        }

        String world = player.getWorld().getName();
        for (ScoreboardConfig cfg : configs) {
            if (!cfg.worlds().isEmpty() && !cfg.worlds().contains(world)) continue;
            if (plugin.getPlaceholderEngine().evaluateConditions(player, cfg.conditions(), cfg.conditionMode())) {
                return cfg;
            }
        }
        return null;
    }

    private void removeSidebar(Player player) {
        Sidebar sidebar = activeSidebars.remove(player.getUniqueId());
        if (sidebar != null) sidebar.close();
        plugin.getPlayerDataCache().setActiveScoreboard(player.getUniqueId(), null);
    }

    public void handleQuit(Player player) {
        removeSidebar(player);
        titleFrameCounters.remove(player.getUniqueId());
        pageTimers.remove(player.getUniqueId());
        pageCounters.remove(player.getUniqueId());
    }

    public List<ScoreboardConfig> getConfigs() {
        return configs;
    }
}