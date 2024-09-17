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
                }

                // Commands all players have access to
                completions.add("info");

                // Filter completions based on partial input
                return completions.stream()
                        .filter(c -> c.startsWith(args[0].toLowerCase()))
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
            }
        }

        return Collections.emptyList();
    }
}
