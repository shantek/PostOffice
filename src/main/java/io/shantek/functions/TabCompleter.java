package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

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
                // Check the first argument
                if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("shantek.postoffice.reload")) {
                    completions.add("reload");
                }
                if ("barrelname".startsWith(args[0].toLowerCase()) && sender.hasPermission("shantek.postoffice.setname")) {
                    completions.add("barrelname");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("barrelname") && sender.hasPermission("shantek.postoffice.setname")) {
                // Check the second argument for "barrelname" subcommand
                if (args[1].isEmpty()) {
                    completions.add("<name>");
                }
            }
        }

        return completions;
    }

}
