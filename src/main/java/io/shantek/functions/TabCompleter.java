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
            }
        }
        return completions;
    }

}
