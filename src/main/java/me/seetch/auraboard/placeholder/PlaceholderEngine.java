package me.seetch.auraboard.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderEngine {

    private final AuraBoardPlugin plugin;
    private final boolean papiEnabled;
    private final Map<UUID, Map<String, CachedValue>> cache = new ConcurrentHashMap<>();
    private long cacheTimeMs;

    public PlaceholderEngine(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        this.papiEnabled = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.cacheTimeMs = plugin.getConfigManager().getMainConfig().getLong("placeholder-cache-ms", 500);
    }

    public String resolve(Player player, String text) {
        if (text == null) return "";
        if (!papiEnabled) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Cached placeholder resolution with per-player cache
     */
    public String resolveCached(Player player, String placeholder) {
        if (plugin.getApi() != null &&
                plugin.getApi().getCustomConditions().containsKey(placeholder.toUpperCase())) {
            return placeholder;
        }

        Map<String, CachedValue> playerCache = cache.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>()
        );

        CachedValue cached = playerCache.get(placeholder);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.timestamp < cacheTimeMs) {
            return cached.value;
        }

        String resolved = papiEnabled
                ? PlaceholderAPI.setPlaceholders(player, placeholder)
                : placeholder;

        playerCache.put(placeholder, new CachedValue(resolved, now));
        return resolved;
    }

    public boolean evaluateConditions(Player player, List<Condition> conditions, ConditionMode mode) {
        if (conditions.isEmpty()) return true;

        for (Condition condition : conditions) {
            boolean result;

            if (plugin.getApi() != null &&
                    plugin.getApi().getCustomConditions().containsKey(condition.placeholder().toUpperCase())) {
                result = plugin.getApi().evaluateCustomCondition(
                        condition.placeholder(), player, condition.value()
                );
            } else {
                String resolved = resolveCached(player, condition.placeholder());
                result = condition.evaluate(resolved);
            }

            if (mode == ConditionMode.ANY && result) return true;
            if (mode == ConditionMode.ALL && !result) return false;
        }
        return mode == ConditionMode.ALL;
    }

    public void invalidatePlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public void invalidateAll() {
        cache.clear();
        cacheTimeMs = plugin.getConfigManager().getMainConfig().getLong("placeholder-cache-ms", 500);

        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            invalidatePlayer(player.getUniqueId());
        }
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    private record CachedValue(String value, long timestamp) {
    }
}