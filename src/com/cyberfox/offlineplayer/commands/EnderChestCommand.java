package com.cyberfox.offlineplayer.commands;

import com.cyberfox.offlineplayer.OfflinePlayerFile;
import com.cyberfox.offlineplayer.listeners.InventoryEvents;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.lang.IllegalArgumentException;

public class EnderChestCommand implements SubCommand {

	@Override
	public boolean onCommand(final CommandSender sender, String[] args) {
		if (args.length != 1)
			return false;

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Not usable via console.");
			return true;
		}

		final String uuidString = args[0];
		UUID playerUuid;
		try {
			playerUuid = UUID.fromString(uuidString);
		} catch(IllegalArgumentException badUuid) {
			sender.sendMessage(ChatColor.RED + "Bad UUID.");
			return true;
		}

		Player player = Bukkit.getPlayer(playerUuid);
		if (player != null) {
			sender.sendMessage(ChatColor.RED + player.getName() + " is online!");
			return true;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				OfflinePlayerFile player = new OfflinePlayerFile(sender, playerUuid);

				if (player.getNbt() == null)
					return;

				final Inventory playerInventory = player.getEnderChest((Player) sender);

				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

					@Override
					public void run() {
						((Player) sender).openInventory(playerInventory);

						InventoryEvents.openedEnderInvs.put(((Player) sender).getUniqueId(), playerUuid);
					}
				});
			}
		});

		return true;
	}

	@Override
	public String help(CommandSender p) {
		return "echest <playerName>";
	}

	@Override
	public String permission() {
		return "echest";
	}

}
