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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("postoffice")) {
            if (args.length > 1 && args[0].equalsIgnoreCase("barrelname")) {

                if (args.length > 2) {

                    // They are sending something longer than they should be
                    sender.sendMessage(ChatColor.RED + "Invalid barrel name, use /postoffice barrelname <name>");
                    return true;

                } else {

                    // The length seems valid, time to process it
                    String name = args[1];
                    postOffice.customBarrelName = name;

                    postOffice.pluginConfig.setCustomBarrelName(postOffice.customBarrelName);
                    sender.sendMessage("Custom barrel name set to " + postOffice.customBarrelName);
                    return true;
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("barrelname")) {

                // They sent /postoffice barrelname without actually defining a name to use
                sender.sendMessage(ChatColor.RED + "Invalid barrel name, use /postoffice barrelname <name>");
                return true;

            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {

                    // Reload all the config and language file
                    postOffice.pluginConfig.reloadConfigFile();
                    sender.sendMessage(ChatColor.GREEN + "Post Office config file has been reloaded.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                }
                return true;
            } else {
                // Invalid command format
                sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
                return false;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
            return false;
        }
    }

}
