package me.seetch.auraboard.module.belowname;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import me.seetch.auraboard.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BelownameManager {

    private final AuraBoardPlugin plugin;
    private boolean globalEnabled;
    private List<String> worlds;
    private List<BelownameConfig> configs = new ArrayList<>();

    // We use Bukkit scoreboard API for belowname (BELOW_NAME objective)
    // because it's a native MC feature and scoreboard-library doesn't expose it.
    private final org.bukkit.scoreboard.ScoreboardManager bukkitScoreboardManager;

    public BelownameManager(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        this.bukkitScoreboardManager = Bukkit.getScoreboardManager();
        reload();
    }

    public void reload() {
        ConfigurationSection sec = plugin.getConfigManager().getBelownameConfig();
        globalEnabled = sec.getBoolean("enabled", true);
        worlds = sec.getStringList("worlds");
        configs = loadConfigs(sec);
        configs.sort(Comparator.comparingInt((BelownameConfig c) -> c.priority()).reversed());
    }

    private List<BelownameConfig> loadConfigs(ConfigurationSection root) {
        List<BelownameConfig> list = new ArrayList<>();
        ConfigurationSection modes = root.getConfigurationSection("modes");
        if (modes == null) return list;

        for (String id : modes.getKeys(false)) {
            ConfigurationSection sec = modes.getConfigurationSection(id);
            if (sec == null) continue;

            int priority = sec.getInt("priority", 0);
            List<Condition> conditions = Condition.loadList(sec, "conditions");
            String modeStr = sec.getString("condition-mode", "ALL");
            ConditionMode mode;
            try {
                mode = ConditionMode.valueOf(modeStr.toUpperCase());
            } catch (Exception e) {
                mode = ConditionMode.ALL;
            }

            String score = sec.getString("score", "0");
            String displayName = sec.getString("display-name", "❤ HP");
            String customFormat = sec.getString("custom-score-format", null);

            list.add(new BelownameConfig(id, priority, conditions, mode, score, displayName, customFormat));
        }
        return list;
    }

    public void update(Player player) {
        if (!globalEnabled || !plugin.getPlayerDataCache().isBelownameVisible(player.getUniqueId())) {
            scheduleMainThread(() -> clearBelowname(player));
            return;
        }

        String world = player.getWorld().getName();
        if (!worlds.isEmpty() && !worlds.contains(world)) {
            scheduleMainThread(() -> clearBelowname(player));
            return;
        }

        BelownameConfig target = resolveConfig(player);
        if (target == null) {
            scheduleMainThread(() -> clearBelowname(player));
            return;
        }

        String resolvedDisplay = plugin.getPlaceholderEngine().resolve(player, target.displayName());
        String resolvedScore = plugin.getPlaceholderEngine().resolve(player, target.score());
        String resolvedFormat = target.customScoreFormat() != null
                ? plugin.getPlaceholderEngine().resolve(player, target.customScoreFormat())
                : null;

        scheduleMainThread(() -> {
            applyBelowname(player, target, resolvedDisplay, resolvedScore, resolvedFormat);
            plugin.getPlayerDataCache().setActiveBelowname(player.getUniqueId(), target.id());
        });
    }

    private void applyBelowname(Player player, BelownameConfig cfg,
                                String resolvedDisplay, String resolvedScore, String resolvedFormat) {
        org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null || scoreboard == bukkitScoreboardManager.getMainScoreboard()) {
            scoreboard = bukkitScoreboardManager.getNewScoreboard();
        }

        Objective obj = scoreboard.getObjective("ab_below");
        if (obj == null) {
            obj = scoreboard.registerNewObjective(
                    "ab_below",
                    Criteria.DUMMY,
                    Component.text("❤ HP")
            );
            obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        obj.displayName(TextUtil.parse(resolvedDisplay));

        int scoreValue;
        try {
            scoreValue = (int) Double.parseDouble(resolvedScore.trim());
        } catch (NumberFormatException e) {
            scoreValue = 0;
        }

        Score score = obj.getScore(player.getName());
        score.setScore(scoreValue);

        if (resolvedFormat != null && !resolvedFormat.isEmpty()) {
            try {
                score.customName(TextUtil.parse(resolvedFormat));
            } catch (Exception ignored) {
            }
        }

        player.setScoreboard(scoreboard);
    }

    private void clearBelowname(Player player) {
        org.bukkit.scoreboard.Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) return;
        Objective obj = scoreboard.getObjective("ab_below");
        if (obj != null) obj.unregister();
    }

    private BelownameConfig resolveConfig(Player player) {
        for (BelownameConfig cfg : configs) {
            if (plugin.getPlaceholderEngine().evaluateConditions(player, cfg.conditions(), cfg.conditionMode())) {
                return cfg;
            }
        }
        return null;
    }

    public void handleQuit(Player player) {
        clearBelowname(player);
        plugin.getPlayerDataCache().setActiveBelowname(player.getUniqueId(), null);
    }

    private void scheduleMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}