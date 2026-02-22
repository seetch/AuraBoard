package me.seetch.auraboard.task;

import me.seetch.auraboard.AuraBoardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateTask extends BukkitRunnable {

    private final AuraBoardPlugin plugin;

    public UpdateTask(AuraBoardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // This runs async - scoreboard-library is packet-level and thread-safe
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (plugin.getScoreboardManager() != null) plugin.getScoreboardManager().update(player);
                if (plugin.getTabManager() != null) plugin.getTabManager().update(player);
                // Nametag and belowname update less frequently (every 2 calls)
                if (plugin.getNametagManager() != null) plugin.getNametagManager().update(player);
                if (plugin.getBelownameManager() != null) plugin.getBelownameManager().update(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating player " + player.getName() + ": " + e.getMessage());
                if (plugin.getConfigManager().getMainConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        }
    }
}