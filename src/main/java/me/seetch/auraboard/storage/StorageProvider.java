package me.seetch.auraboard.storage;

import me.seetch.auraboard.cache.PlayerDataCache;

import java.util.UUID;
import java.util.function.Consumer;

public interface StorageProvider {

    void init();
    void loadPlayer(UUID uuid, Consumer<PlayerDataCache.PlayerData> callback);
    void savePlayerAsync(UUID uuid, PlayerDataCache.PlayerData data);
    void close();
}