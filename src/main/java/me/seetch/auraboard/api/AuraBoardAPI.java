package me.seetch.auraboard.api;

import me.seetch.auraboard.AuraBoardPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Public API for other plugins to interact with AuraBoard.
 */
public class AuraBoardAPI {

    private final AuraBoardPlugin plugin;
    private final Map<String, BiPredicate<Player, String>> customConditions = new HashMap<>();

    private static AuraBoardAPI instance;

    public AuraBoardAPI(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static AuraBoardAPI get() {
        return instance;
    }

    /** Force-set scoreboard for player (ignores conditions) */
    public void setScoreboard(Player player, String scoreboardId) {
        plugin.getPlayerDataCache().setForcedScoreboard(player.getUniqueId(), scoreboardId);
        if (plugin.getScoreboardManager() != null) plugin.getScoreboardManager().update(player);
    }

    /** Clear forced scoreboard, return to auto mode */
    public void clearForcedScoreboard(Player player) {
        plugin.getPlayerDataCache().setForcedScoreboard(player.getUniqueId(), null);
        plugin.getPlayerDataCache().setActiveScoreboard(player.getUniqueId(), null);
        if (plugin.getScoreboardManager() != null) plugin.getScoreboardManager().update(player);
    }

    /** Check if scoreboard is visible for player */
    public boolean isScoreboardVisible(Player player) {
        return plugin.getPlayerDataCache().isScoreboardVisible(player.getUniqueId());
    }

    /**
     * Register a custom condition operator.
     * Example: api.registerCondition("HAS_PERMISSION", (player, value) -> player.hasPermission(value));
     */
    public void registerCondition(String operatorName, BiPredicate<Player, String> evaluator) {
        customConditions.put(operatorName.toUpperCase(), evaluator);
    }

    public boolean evaluateCustomCondition(String operator, Player player, String value) {
        BiPredicate<Player, String> pred = customConditions.get(operator.toUpperCase());
        return pred != null && pred.test(player, value);
    }

    public Map<String, BiPredicate<Player, String>> getCustomConditions() {
        return customConditions;
    }
}