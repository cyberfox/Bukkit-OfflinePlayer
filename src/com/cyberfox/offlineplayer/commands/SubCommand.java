package com.cyberfox.offlineplayer.commands;

import java.io.IOException;

import com.cyberfox.offlineplayer.OfflinePlayerPlugin;

import org.bukkit.command.CommandSender;

public interface SubCommand {

	public OfflinePlayerPlugin plugin = OfflinePlayerPlugin.getPlugin();

	public boolean onCommand(CommandSender sender, String[] args) throws IOException;

	public String help(CommandSender p);

	public String permission();

}
