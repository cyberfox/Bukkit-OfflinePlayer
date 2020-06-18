package com.cyberfox.offlineplayer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.cyberfox.offlineplayer.commands.*;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor, TabCompleter {
	private Map<String, SubCommand> commands;
	private String commandName;
	private OfflinePlayerPlugin plugin = OfflinePlayerPlugin.getPlugin();
	private Map<String, OfflinePlayer> offlinePlayerMap;

	public CommandHandler(String name, Map<String, OfflinePlayer> playerMap) {
		commands = new HashMap<>();
		commandName = name;

		offlinePlayerMap = playerMap;

		commands.put("inventory", new InventoryCommand());
		commands.put("enderchest", new EnderChestCommand());
	}

	public void registerSubCommand(String commandName, SubCommand command) {
		commands.put(commandName, command);
	}

	public Map<String, SubCommand> getCommands() {
		return commands;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd1, String commandLabel, String[] args) {
		String cmd = cmd1.getName();

		if (cmd.equalsIgnoreCase(commandName)) {
			if (args == null || args.length < 1) {
				sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getPluginFriendlyName());
				sender.sendMessage(ChatColor.GOLD + "Type /" + commandName + " help for help");

				return true;
			}

			if (args[0].equalsIgnoreCase("help")) {
				help(sender);
				return true;
			}
			String sub = args[0];

			Vector<String> l = new Vector<String>();
			l.addAll(Arrays.asList(args));
			l.remove(0);
			args = (String[]) l.toArray(new String[0]);
			if(args.length > 0) {
				String username = l.get(0);
				if (offlinePlayerMap.get(username) != null) {
					args[0] = offlinePlayerMap.get(username).getUniqueId().toString();
					System.out.println("Converted " + username + " to " + l.get(0));
				}
			}

			if (!commands.containsKey(sub)) {
				sender.sendMessage(ChatColor.RED + "Command dosent exist.");
				sender.sendMessage(ChatColor.DARK_AQUA + "Type /" + commandName + " help for help");
				return true;
			}
			try {
				if (!sender.hasPermission(plugin.getPermissionBase() + "." + commands.get(sub).permission()))
					sender.sendMessage(ChatColor.RED + "You do not have permission for this command");
				else {
					SubCommand command = commands.get(sub);
					if (!command.onCommand(sender, args) && command.help(sender) != null)
						sender.sendMessage(ChatColor.RED + "/" + commandName + " " + command.help(sender));
				}
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage(ChatColor.RED + "An error occured while executing the command. Check the console");
				sender.sendMessage(ChatColor.BLUE + "Type /" + commandName + " help for help");
			}
			return true;
		}
		return false;
	}

	public void help(CommandSender p) {
		p.sendMessage(ChatColor.GOLD + "/" + commandName + " <command> <args>");
		p.sendMessage(ChatColor.GOLD + "Commands are as follows:");
		for (SubCommand v : commands.values()) {
			if (p.hasPermission(plugin.getPermissionBase() + "." + v.permission()) && v.help(p) != null)
				p.sendMessage(ChatColor.AQUA + v.help(p));
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
		List<String> offers = new ArrayList<>();
		if(strings.length == 0) return offers;

		String prefix = strings[0];
		if(strings.length == 1) {
			offers = commands.keySet().stream().filter(cmd -> cmd.startsWith(prefix)).collect(Collectors.toList());
		}

		if(strings.length == 2 && commands.containsKey(prefix)) {
			String name = strings[1];
			for (Map.Entry<String, OfflinePlayer> entries : offlinePlayerMap.entrySet()) {
				OfflinePlayer player = entries.getValue();
				if (!player.isOnline() && player.getName() != null && player.getName().substring(0, name.length()).equalsIgnoreCase(name)) {
					offers.add(entries.getKey());
				}
			}
			if(!commandSender.isOp()) {
				offers.removeIf(s1 -> !s1.startsWith(commandSender.getName()));
			}
		}

		return offers;
	}
}
