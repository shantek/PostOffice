package io.shantek.functions;

import io.shantek.PostOffice;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor {

    public PostOffice postOffice;
    public Commands(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("postoffice")) {
            if (args.length == 2 && args[0].equalsIgnoreCase("barrelname")) {
                // Check if the second argument is present
                String name = args[1];
                postOffice.customBarrelName = name;

                postOffice.pluginConfig.setCustomBarrelName(postOffice.customBarrelName);
                sender.sendMessage("Custom barrel name set to " + postOffice.customBarrelName);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("barrelname")) {
                sender.sendMessage(ChatColor.RED + "Invalid barrel name, use /postoffice barrelname <name>");
                return true;

            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {
                    // Reload logic
                    postOffice.pluginConfig.reloadConfigFile();
                    sender.sendMessage(ChatColor.GREEN + "PostOffice config reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                }
                return true;
            } else {
                // Invalid command format
                sender.sendMessage(ChatColor.RED + "Invalid command");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
            return false;
        }
    }

}
