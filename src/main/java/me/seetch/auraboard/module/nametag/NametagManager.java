package me.seetch.auraboard.module.nametag;

import me.seetch.auraboard.AuraBoardPlugin;
import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import me.seetch.auraboard.util.TextUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamDisplay;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import net.megavex.scoreboardlibrary.api.team.enums.NameTagVisibility;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {

    private final AuraBoardPlugin plugin;
    private final TeamManager teamManager;
    private final Map<UUID, String> activeTeam = new ConcurrentHashMap<>();
    private List<NametagConfig> configs = new ArrayList<>();

    public NametagManager(AuraBoardPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getScoreboardLibrary().createTeamManager();
        reload();
    }

    public void reload() {
        configs = loadConfigs();
        configs.sort(Comparator.comparingInt((NametagConfig c) -> c.priority()).reversed());
    }

    private List<NametagConfig> loadConfigs() {
        List<NametagConfig> list = new ArrayList<>();
        ConfigurationSection root = plugin.getConfigManager().getNametagConfig()
                .getConfigurationSection("nametags");
        if (root == null) return list;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
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

            String prefix = sec.getString("prefix", "");
            String suffix = sec.getString("suffix", "");
            String colorStr = sec.getString("player-color", "WHITE");
            NamedTextColor color = NamedTextColor.NAMES.value(colorStr.toLowerCase());
            if (color == null) color = NamedTextColor.WHITE;
            String visibility = sec.getString("visibility", "ALWAYS");

            list.add(new NametagConfig(id, priority, conditions, mode, prefix, suffix, color, visibility));
        }
        return list;
    }

    public void update(Player player) {
        NametagConfig target = resolveConfig(player);
        if (target == null) {
            removeFromTeams(player);
            return;
        }

        String currentId = plugin.getPlayerDataCache().getActiveNametag(player.getUniqueId());

        String teamName = "ab_nt_" + target.id();
        ScoreboardTeam team = teamManager.createIfAbsent(teamName);
        TeamDisplay display = team.defaultDisplay();

        String resolvedPrefix = plugin.getPlaceholderEngine().resolve(player, target.prefix());
        String resolvedSuffix = plugin.getPlaceholderEngine().resolve(player, target.suffix());

        display.prefix(TextUtil.parse(resolvedPrefix));
        display.suffix(TextUtil.parse(resolvedSuffix));
        if (target.playerColor() != null) display.playerColor(target.playerColor());

        // Visibility
        switch (target.visibility().toUpperCase()) {
            case "NEVER" -> {
                display.nameTagVisibility(NameTagVisibility.NEVER);
            }
            case "HIDE_FOR_OTHER_TEAMS" -> {
                display.nameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
            }
            default -> {
                display.nameTagVisibility(NameTagVisibility.ALWAYS);
            }
        }

        team.defaultDisplay().addEntry(player.getName());
        activeTeam.put(player.getUniqueId(), teamName);

        if (!target.id().equals(currentId)) {
            plugin.getPlayerDataCache().setActiveNametag(player.getUniqueId(), target.id());
        }
    }

    private NametagConfig resolveConfig(Player player) {
        for (NametagConfig cfg : configs) {
            if (plugin.getPlaceholderEngine().evaluateConditions(player, cfg.conditions(), cfg.conditionMode())) {
                return cfg;
            }
        }
        return null;
    }

    private void removeFromTeams(Player player) {
        String teamName = activeTeam.remove(player.getUniqueId());
        if (teamName != null) {
            ScoreboardTeam team = teamManager.team(teamName);
            if (team != null) team.defaultDisplay().removeEntry(player.getName());
        }
        plugin.getPlayerDataCache().setActiveNametag(player.getUniqueId(), null);
    }

    public void handleQuit(Player player) {
        removeFromTeams(player);
    }
}