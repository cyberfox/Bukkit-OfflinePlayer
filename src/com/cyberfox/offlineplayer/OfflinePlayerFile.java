package com.cyberfox.offlineplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.sk89q.jnbt.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

public class OfflinePlayerFile {
	private UUID uuid;
	private CompoundTag nbt = null;
	private File file;
	private File folder;
	private boolean autoSave = true;

	public OfflinePlayerFile(CommandSender sender, UUID uuid) {
		this.uuid = uuid;
		this.folder = OfflinePlayerPlugin.playerWorldFolder;

		try {
			load();
		} catch (FileNotFoundException e) {
			sender.sendMessage(ChatColor.RED + uuid.toString() + " could not be found");
			return;
		} catch (IOException e) {
			sender.sendMessage(ChatColor.RED + "Unable to write to " + uuid.toString() + ".dat please check server file permissions are correct.");
			return;
		}
	}

	public OfflinePlayerFile(CommandSender sender, UUID uuid, boolean autoSave) {
		this(sender, uuid);
		this.autoSave = autoSave;
	}

	public OfflinePlayerFile(UUID uuid) throws IOException, FileNotFoundException {
		this(uuid, OfflinePlayerPlugin.playerWorldFolder);
	}

	public OfflinePlayerFile(UUID uuid, boolean autoSave) throws IOException, FileNotFoundException {
		this(uuid, OfflinePlayerPlugin.playerWorldFolder);
		this.autoSave = autoSave;
	}

	public OfflinePlayerFile(UUID uuid, File folder) throws IOException, FileNotFoundException {
		this.uuid = uuid;
		this.folder = folder;

		load();
	}

	public OfflinePlayerFile(UUID uuid, File folder, boolean autoSave) throws IOException, FileNotFoundException {
		this(uuid, folder);
		this.autoSave = autoSave;
	}

	private void load() throws IOException, FileNotFoundException {
		file = new File(folder, uuid.toString() + ".dat");

		if (!file.exists())
			throw new FileNotFoundException("Can't find player data file.");

		if (!file.canWrite())
			throw new IOException("Can't write to the player data file.");

		try(BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
				GZIPInputStream gzIS = new GZIPInputStream(is);
				NBTInputStream nbIS = new NBTInputStream(gzIS)) {
			nbt = (CompoundTag)nbIS.readNamedTag().getTag();
		}
	}

	public CompoundTag getNbt() {
		return nbt;
	}

	public void save() {
		//            try {
		//  Don't know how to save.
		//                nbt.saveTo(Files.asByteSink(file), StreamOptions.GZIP_COMPRESSION);
		//            } catch (IOException e) {
		//                e.printStackTrace();
		//            }
	}

	private Inventory getInventory(Player holder, String path) {
		Class<?> inventoryClass = Util.getCraftClass("PlayerInventory");
		Class<?> entityHumanClass = Util.getCraftClass("EntityHuman");
		Class<?> craftPlayerClass = Util.getCraftBukkitClass("entity.CraftPlayer");
		Object inventory = null;

		try {
			Method getHandle = craftPlayerClass.getDeclaredMethod("getHandle", new Class<?>[]{});
			Object craftPlayer = getHandle.invoke(craftPlayerClass.cast(holder), new Object[]{});

			inventory = inventoryClass.getConstructor(entityHumanClass).newInstance(entityHumanClass.cast(craftPlayer));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}

		Method[] allMethods = inventoryClass.getMethods();
		for(Method m : allMethods) {
			System.out.println("name: " + m.getName());
			System.out.print("params: ");
			boolean comma = false;
			for(Class c : m.getParameterTypes()) {
				if(comma) { System.out.print(", "); }
				System.out.print(c.getName());
				comma = true;
			}
			System.out.println();
		}

		Class<?> ntl = Util.getCraftClass("NBTTagList");
		Inventory playerInv = null;

		Method inventoryMethod = Util.getMethod(inventoryClass, "b", new Class<?>[] { ntl });
		if(inventoryMethod != null) {
			try {
				NBTConverter converter = new NBTConverter();
				List<Tag> replaced = getInventoryTags(path);
				inventoryMethod.invoke(inventory, converter.convert(replaced));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

			Class<?> inventoryPlayerClass = Util.getCraftBukkitClass("inventory.CraftInventoryPlayer");

			try {
				playerInv = (Inventory) inventoryPlayerClass.getConstructor(inventoryClass).newInstance(inventory);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Could not retrieve inventory method.");
		}
		return playerInv;
	}

	private List<Tag> getInventoryTags(String path) {
		List<Tag> foo = nbt.getList(path);
		List<Tag> replaced = new ArrayList<>(foo.size());
		byte nextSlot = 0;
		for(Tag step : foo) {
			if (step instanceof CompoundTag) {
				CompoundTag ct = (CompoundTag)step;
				Map<String, Tag> slot = new HashMap<>(ct.getValue());
				slot.put("Slot", new ByteTag(nextSlot++));
				CompoundTag newTag = ct.setValue(slot);
				System.out.println(slot.keySet());
				replaced.add(newTag);
			} else {
				replaced.add(step);
			}
		}
		return replaced;
	}

	public Inventory getInventory(Player holder) {
		return getInventory(holder, "Inventory");
	}

	public void setInventory(Inventory inventory) {
		setInventory("Inventory", inventory);
	}

	private void setInventory(String path, Inventory inventory) {
		Class<?> inventoryPlayerClass = Util.getCraftBukkitClass("inventory.CraftInventoryPlayer");

		Object craftInventory = inventoryPlayerClass.cast(inventory);
		Method getInventoryMethod = Util.getMethod(inventoryPlayerClass, "getInventory");

		Object getInventory = null;

		try {
			getInventory = getInventoryMethod.invoke(craftInventory);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		Class<?> inventoryClass = Util.getCraftClass("PlayerInventory");
		ListTag nbtInv = nbt.getListTag(path);
		Method toNbtMethod = Util.getMethod(inventoryClass, "a", new Class<?>[] { nbtInv.getType() });
		nbtInv.getValue().clear();
		try {
			toNbtMethod.invoke(getInventory, nbtInv.getType());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		//		nbt.putPath(path, nbtInv);

		if (autoSave)
			save();
	}

	public Inventory getEnderChest(Player holder) {
		return getInventory(holder, "EnderItems");
	}

	public void setEnderChest(Inventory inventory) {
		setInventory("EnderItems", inventory);
	}

	public long getLastSeen() {
            return nbt.getLong("bukkit.lastPlayed");//, (long) file.lastModified());
	}
}
