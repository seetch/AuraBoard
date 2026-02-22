package me.seetch.auraboard.hook;

import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateHook {

    private static FloodgateHook instance;
    private final boolean enabled;

    private FloodgateHook(Plugin plugin) {
        this.enabled = plugin.getServer().getPluginManager().getPlugin("floodgate") != null;
        if (enabled) {
            plugin.getLogger().info("Floodgate hook enabled.");
        }
    }

    public static void init(Plugin plugin) {
        instance = new FloodgateHook(plugin);
    }

    public static FloodgateHook getInstance() {
        return instance;
    }

    public boolean isFloodgatePlayer(UUID uniqueId, String name) {
        if (!enabled) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uniqueId);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}