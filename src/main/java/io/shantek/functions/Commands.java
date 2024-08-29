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
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shantek.postoffice.reload") || sender.isOp()) {

                    // Reload all the config and language file
                    postOffice.pluginConfig.reloadConfigFile();
                    sender.sendMessage(ChatColor.GREEN + "Post Office config file has been reloaded.");
                    return true;
                } else {
                    // Player doesn't have permission to use the command
                    sender.sendMessage(ChatColor.RED + "You don't have access to this command!");
                    return false;
                }
            } else {
                // Invalid command format
                sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permission.");
                return false;
            }
        }
        return false;
    }

}

