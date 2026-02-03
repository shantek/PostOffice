package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    public PostOffice postOffice;

    public TabCompleter(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("postoffice")) {
            if (args.length == 1) {
                // Add commands based on permissions
                if (sender.hasPermission("shantek.postoffice.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("shantek.postoffice.claim") || sender.hasPermission("shantek.postoffice.claim.others")) {
                    completions.add("claim");
                }
                if (sender.hasPermission("shantek.postoffice.register")) {
                    completions.add("register");
                    completions.add("remove");
                    completions.add("list");
                    completions.add("removesecondary");
                    completions.add("log");
                }
                if (sender.hasPermission("shantek.postoffice.claim")) {
                    completions.add("secondary");
                }

                // Commands all players have access to
                completions.add("info");
                completions.add("history");

                // Filter completions based on partial input
                return completions.stream()
                        .filter(c -> c.startsWith(args[0].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("log") && sender.hasPermission("shantek.postoffice.register")) {
                // Tab completion for log command filters
                // Support: /postoffice log [timeframe] [page] [type:<type>] [player:<name>]
                
                String lastArg = args[args.length - 1];
                
                // If they're typing a type filter
                if (lastArg.toLowerCase().startsWith("type:")) {
                    completions.add("type:registered");
                    completions.add("type:claimed");
                    completions.add("type:removed");
                    completions.add("type:secondary");
                    completions.add("type:secondary_removed");
                    completions.add("type:deposited");
                    completions.add("type:withdrawn");
                } 
                // If they're typing a player filter
                else if (lastArg.toLowerCase().startsWith("player:")) {
                    String prefix = "player:";
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(prefix + player.getName());
                    }
                } 
                // Otherwise suggest all filter options
                else {
                    completions.add("type:");
                    completions.add("player:");
                    // Also suggest common timeframes
                    completions.add("7d");
                    completions.add("30d");
                    completions.add("90d");
                }

                // Filter completions based on partial input
                return completions.stream()
                        .filter(c -> c.toLowerCase().startsWith(lastArg.toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("claim") && sender.hasPermission("shantek.postoffice.claim.others")) {
                // Populate online players' names for claim.others permission
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }

                // Filter players based on partial input
                return completions.stream()
                        .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list") && sender.hasPermission("shantek.postoffice.register")) {
                // Populate online players' names for list command
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }

                // Filter players based on partial input
                return completions.stream()
                        .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("removesecondary") && sender.hasPermission("shantek.postoffice.register")) {
                // Populate online players' names for removesecondary command
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }

                // Filter players based on partial input
                return completions.stream()
                        .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
