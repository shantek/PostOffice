package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PostBoxTabCompleter implements TabCompleter {

    private final PostOffice postOffice;

    public PostBoxTabCompleter(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("postbox")) {
            return Collections.emptyList();
        }

        // Only offer player name completions for arg 1, and only with the others permission
        if (args.length == 1 && sender.hasPermission("shantek.postoffice.openremote.others")) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
