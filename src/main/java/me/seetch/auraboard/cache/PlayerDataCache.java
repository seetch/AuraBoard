package me.seetch.auraboard.cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataCache {

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public void init(UUID uuid, PlayerData data) {
        cache.put(uuid, data != null ? data : new PlayerData());
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public PlayerData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public void resetActiveModules(UUID uuid) {
        PlayerData data = getData(uuid);
        data.activeScoreboardId = null;
        data.activeNametagId = null;
        data.activeBelownameId = null;
    }

    public boolean isScoreboardVisible(UUID uuid) {
        return getData(uuid).scoreboardVisible;
    }

    public void setScoreboardVisible(UUID uuid, boolean visible) {
        getData(uuid).scoreboardVisible = visible;
    }

    public boolean isTabVisible(UUID uuid) {
        return getData(uuid).tabVisible;
    }

    public void setTabVisible(UUID uuid, boolean visible) {
        getData(uuid).tabVisible = visible;
    }

    public boolean isBelownameVisible(UUID uuid) {
        return getData(uuid).belownameVisible;
    }

    public void setBelownameVisible(UUID uuid, boolean visible) {
        getData(uuid).belownameVisible = visible;
    }

    public String getForcedScoreboard(UUID uuid) {
        return getData(uuid).forcedScoreboardId;
    }

    public void setForcedScoreboard(UUID uuid, String id) {
        getData(uuid).forcedScoreboardId = id;
    }

    public String getActiveScoreboard(UUID uuid) {
        return getData(uuid).activeScoreboardId;
    }

    public void setActiveScoreboard(UUID uuid, String id) {
        getData(uuid).activeScoreboardId = id;
    }

    public String getActiveNametag(UUID uuid) {
        return getData(uuid).activeNametagId;
    }

    public void setActiveNametag(UUID uuid, String id) {
        getData(uuid).activeNametagId = id;
    }

    public String getActiveBelowname(UUID uuid) {
        return getData(uuid).activeBelownameId;
    }

    public void setActiveBelowname(UUID uuid, String id) {
        getData(uuid).activeBelownameId = id;
    }

    // Inner data class
    public static class PlayerData {
        public boolean scoreboardVisible = true;
        public boolean tabVisible = true;
        public boolean belownameVisible = true;
        public String forcedScoreboardId = null;
        public String activeScoreboardId = null;
        public String activeNametagId = null;
        public String activeBelownameId = null;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put("scoreboard-visible", scoreboardVisible);
            map.put("tab-visible", tabVisible);
            map.put("belowname-visible", belownameVisible);
            if (forcedScoreboardId != null) map.put("forced-scoreboard", forcedScoreboardId);
            return map;
        }

        public static PlayerData fromMap(Map<String, Object> map) {
            PlayerData data = new PlayerData();
            if (map == null) return data;
            data.scoreboardVisible = (boolean) map.getOrDefault("scoreboard-visible", true);
            data.tabVisible = (boolean) map.getOrDefault("tab-visible", true);
            data.belownameVisible = (boolean) map.getOrDefault("belowname-visible", true);
            data.forcedScoreboardId = (String) map.get("forced-scoreboard");
            return data;
        }
    }
}