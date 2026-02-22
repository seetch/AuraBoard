package me.seetch.auraboard.command;

import me.seetch.auraboard.AuraBoardPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record AuraBoardCommand(AuraBoardPlugin plugin) implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "scoreboard" -> handleScoreboardToggle(sender, args);
            case "tab" -> handleTabToggle(sender, args);
            case "belowname" -> handleBelownameToggle(sender, args);
            default -> handleAuraBoard(sender, args);
        };
    }

    private boolean handleAuraBoard(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("AuraBoard v" + plugin.getDescription().getVersion(),
                    NamedTextColor.GOLD));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("auraboard.reload")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    break;
                }
                plugin.reload();
                sender.sendMessage(Component.text("AuraBoard reloaded!", NamedTextColor.GREEN));
            }

            case "debug" -> {
                if (!sender.hasPermission("auraboard.debug")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /auraboard debug <player>", NamedTextColor.RED));
                    break;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    break;
                }
                debugPlayer(sender, target);
            }

            case "set" -> {
                if (!sender.hasPermission("auraboard.set")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    break;
                }
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /auraboard set scoreboard <player> <id>",
                            NamedTextColor.RED));
                    break;
                }
                if (args[1].equalsIgnoreCase("scoreboard")) {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                        break;
                    }
                    plugin.getPlayerDataCache().setForcedScoreboard(target.getUniqueId(), args[3]);
                    if (plugin.getScoreboardManager() != null)
                        plugin.getScoreboardManager().update(target);
                    sender.sendMessage(Component.text("Set scoreboard for " + target.getName() +
                            " to " + args[3], NamedTextColor.GREEN));
                }
            }

            case "clear" -> {
                if (!sender.hasPermission("auraboard.set")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    break;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /auraboard clear scoreboard <player>",
                            NamedTextColor.RED));
                    break;
                }
                if (args[1].equalsIgnoreCase("scoreboard")) {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                        break;
                    }
                    plugin.getPlayerDataCache().setForcedScoreboard(target.getUniqueId(), null);
                    plugin.getPlayerDataCache().setActiveScoreboard(target.getUniqueId(), null);
                    if (plugin.getScoreboardManager() != null)
                        plugin.getScoreboardManager().update(target);
                    sender.sendMessage(Component.text("Cleared forced scoreboard for " + target.getName(),
                            NamedTextColor.GREEN));
                }
            }

            case "version" -> {
                sender.sendMessage(Component.text(
                        "AuraBoard v" + plugin.getDescription().getVersion() +
                                " | Scoreboard: " + (plugin.getScoreboardManager() != null ? "✓" : "✗") +
                                " | TAB: " + (plugin.getTabManager() != null ? "✓" : "✗") +
                                " | Nametag: " + (plugin.getNametagManager() != null ? "✓" : "✗") +
                                " | Belowname: " + (plugin.getBelownameManager() != null ? "✓" : "✗"),
                        NamedTextColor.YELLOW));
            }

            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use: reload, debug, set, clear, version",
                        NamedTextColor.RED));
            }
        }
        return true;
    }

    private void debugPlayer(CommandSender sender, Player target) {
        var cache = plugin.getPlayerDataCache();
        sender.sendMessage(Component.text("=== Debug: " + target.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active Scoreboard: " +
                cache.getActiveScoreboard(target.getUniqueId()), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Forced Scoreboard: " +
                cache.getForcedScoreboard(target.getUniqueId()), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Active Nametag: " +
                cache.getActiveNametag(target.getUniqueId()), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Active Belowname: " +
                cache.getActiveBelowname(target.getUniqueId()), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Scoreboard Visible: " +
                cache.isScoreboardVisible(target.getUniqueId()), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("TAB Visible: " +
                cache.isTabVisible(target.getUniqueId()), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Belowname Visible: " +
                cache.isBelownameVisible(target.getUniqueId()), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("World: " + target.getWorld().getName(), NamedTextColor.GRAY));
    }

    private boolean handleScoreboardToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("auraboard.scoreboard.toggle")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("toggle")) {
            player.sendMessage(Component.text("Usage: /scoreboard toggle", NamedTextColor.RED));
            return true;
        }
        var cache = plugin.getPlayerDataCache();
        boolean newState = !cache.isScoreboardVisible(player.getUniqueId());
        cache.setScoreboardVisible(player.getUniqueId(), newState);
        if (plugin.getScoreboardManager() != null) plugin.getScoreboardManager().update(player);
        plugin.getStorageProvider().savePlayerAsync(player.getUniqueId(), cache.getData(player.getUniqueId()));
        player.sendMessage(Component.text("Scoreboard " + (newState ? "shown" : "hidden") + ".",
                newState ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleTabToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("auraboard.tab.toggle")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("toggle")) {
            player.sendMessage(Component.text("Usage: /tab toggle", NamedTextColor.RED));
            return true;
        }
        var cache = plugin.getPlayerDataCache();
        boolean newState = !cache.isTabVisible(player.getUniqueId());
        cache.setTabVisible(player.getUniqueId(), newState);
        if (plugin.getTabManager() != null) plugin.getTabManager().update(player);
        plugin.getStorageProvider().savePlayerAsync(player.getUniqueId(), cache.getData(player.getUniqueId()));
        player.sendMessage(Component.text("TAB header/footer " + (newState ? "shown" : "hidden") + ".",
                newState ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleBelownameToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("auraboard.belowname.toggle")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("toggle")) {
            player.sendMessage(Component.text("Usage: /belowname toggle", NamedTextColor.RED));
            return true;
        }
        var cache = plugin.getPlayerDataCache();
        boolean newState = !cache.isBelownameVisible(player.getUniqueId());
        cache.setBelownameVisible(player.getUniqueId(), newState);
        if (plugin.getBelownameManager() != null) plugin.getBelownameManager().update(player);
        plugin.getStorageProvider().savePlayerAsync(player.getUniqueId(), cache.getData(player.getUniqueId()));
        player.sendMessage(Component.text("Belowname " + (newState ? "shown" : "hidden") + ".",
                newState ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("auraboard")) {
            if (args.length == 1) {
                return Arrays.asList("reload", "debug", "set", "clear", "version")
                        .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("debug"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).collect(Collectors.toList());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear"))) {
                return List.of("scoreboard");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("scoreboard")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).collect(Collectors.toList());
            }
            if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("scoreboard")) {
                return plugin.getScoreboardManager() == null ? List.of() :
                        plugin.getScoreboardManager().getConfigs().stream()
                                .map(c -> c.id()).collect(Collectors.toList());
            }
        }
        return List.of();
    }
}