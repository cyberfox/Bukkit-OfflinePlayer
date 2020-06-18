package com.cyberfox.offlineplayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cyberfox.offlineplayer.listeners.InventoryEvents;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class OfflinePlayerPlugin extends JavaPlugin {
	private static OfflinePlayerPlugin instance;
	public static File playerWorldFolder;

	public void onEnable() {
		instance = this;
		playerWorldFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");

		getConfig().options().copyDefaults(true);
		saveConfig();

		Map<String, OfflinePlayer> playerMap = getPlayerMap(playerWorldFolder);

		getCommand("xoffline").setExecutor(new CommandHandler("xoffline", playerMap));

		getServer().getPluginManager().registerEvents(new InventoryEvents(), this);
	}

	public Map<String, OfflinePlayer> getPlayerMap(File playerFolder) {
		Map<String, OfflinePlayer> offlinePlayerMap = new HashMap<>();
		File[] dirList = playerFolder.listFiles((dir, name) -> name.endsWith(".dat"));
		for(File f : dirList) {
			String uuid = f.getName();
			if(uuid.endsWith(".dat")) {
				uuid = uuid.substring(0, uuid.length()-4);
				UUID uniqueId = UUID.fromString(uuid);
				OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);
				offlinePlayerMap.put(player.getName()+"-"+uuid.substring(0, 4), player);
			}
		}
		System.out.println(String.format("Scanned %d users.", offlinePlayerMap.size()));

		return offlinePlayerMap;
	}

	public static OfflinePlayerPlugin getPlugin() {
		return instance;
	}

	public String getPluginFriendlyName() {
		return "OfflinePlayer";
	}

	public String getPermissionBase() {
		return "offlineplayer";
	}
}
