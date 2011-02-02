package com.bukkit.jjfs85.BetterShop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.bukkit.Server;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.command.*;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BetterShop for Bukkit
 * 
 * @author jjfs85
 */
public class BetterShop extends JavaPlugin {
	public final static String commandPrefix = "b";
	public final static String messagePrefix = "§c[§7SHOP§c] ";
	private final BetterShopPlayerListener playerListener = new BetterShopPlayerListener(
			this);
	private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	private final BetterShopPriceList PriceList = new BetterShopPriceList();

	public BetterShop(PluginLoader pluginLoader, Server instance,
			PluginDescriptionFile desc, File folder, File plugin,
			ClassLoader cLoader) throws IOException {
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

		// NOTE: Event registration should be done in onEnable not here as all
		// events are unregistered when a plugin is disabled
	}

	public void onEnable() {
		// Register our events
		PluginManager pm = getServer().getPluginManager();

		pm.registerEvent(Event.Type.PLAYER_COMMAND, this.playerListener,
				Event.Priority.Normal, this);

		// EXAMPLE: Custom code, here we just output some info so we can check
		// all is well
		PluginDescriptionFile pdfFile = this.getDescription();
		System.out.println(pdfFile.getName() + " version "
				+ pdfFile.getVersion() + " is enabled!");

		// Load up items.db
		File folder = new File("plugins", pdfFile.getName());
		try {
			itemDb.load(folder, "items.db");
		} catch (IOException e) {
			System.out.println("BetterShop: Items.db error");
		}

		// Load prices
		try {
			PriceList.load();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onDisable() {

		// NOTE: All registered events are automatically unregistered when a
		// plugin is disabled

		// EXAMPLE: Custom code, here we just output some info so we can check
		// all is well
		System.out.println("BetterShop now unloaded!");
	}

	public boolean isDebugging(final Player player) {
		if (debugees.containsKey(player)) {
			return debugees.get(player);
		} else {
			return false;
		}
	}

	public void setDebugging(final CommandSender player, final boolean value) {
		debugees.put((Player) player, value);
	}

	public void list(CommandSender player, String[] s) {
		int pagesize = 5;
		if ((s.length != 2) && (s.length != 3)) {
			this.help(player);
		} else {
			int p = (s.length == 2) ? 1 : Integer.parseInt(s[2]);
			int j = 1;
			int i = 1;
			while ((j < p * pagesize)&&(i<2280)) {
				if (PriceList.isForSale(i)) {
					if (j > (p - 1) * pagesize) {
						try {
							BetterShop.sendMessage(player, "["
									+ itemDb.getName(i) + "] Buy: "
									+ PriceList.getBuyPrice(i) + " Sell: "
									+ PriceList.getSellPrice(i));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					j++;						
				}
					i++;
			}
		}
	}

	public void buy(CommandSender player, String[] s) {
		BetterShop.sendMessage(player, "Buying is not implemented yet.");
		// TODO Implement buy method
	}

	public void sell(CommandSender player, String[] s) {
		BetterShop.sendMessage(player, "Selling is not implemented yet.");
		// TODO Implement sell method
	}

	public void add(CommandSender player, String[] s) {
		if (s.length != 5) {
			this.help(player);
		} else {
			try {
				PriceList.setPrice(s[2], s[3], s[4]);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void remove(CommandSender player, String[] s) {
		if (s.length != 3) {
			this.help(player);
		} else {
			try {
				PriceList.remove(s[2]);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void update(CommandSender player, String[] s) {
		if (s.length != 5) {
			this.help(player);
		} else {
			try {
				PriceList.setPrice(s[2], s[3], s[4]);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void load(CommandSender player) {
		// TODO Implement shopping list loading
		try {
			PriceList.load();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			BetterShop
					.sendMessage(player, "Pricelist load error. See console.");
		}
		BetterShop.sendMessage(player, "PriceList loaded.");
	}

	public void help(CommandSender player) {
		// TODO Implement help method
		BetterShop.sendMessage(player,
				"--------------- Better Shop Usage ---------------");
		BetterShop
				.sendMessage(player, "/" + commandPrefix + "shop list <page>");
		BetterShop.sendMessage(player, "/" + commandPrefix
				+ "shop buy [item] [amount]");
		BetterShop.sendMessage(player, "/" + commandPrefix
				+ "shop sell [item] [amount]");
		if (BetterShop.hasPermission(player, "Admin")) {
			BetterShop.sendMessage(player, "/" + commandPrefix
					+ "shop add [item] [$buy] [$sell]");
			BetterShop.sendMessage(player, "/" + commandPrefix
					+ "shop remove [item]");
			BetterShop.sendMessage(player, "/" + commandPrefix
					+ "shop update [item] [$buy] [$sell]");
			BetterShop.sendMessage(player, "/" + commandPrefix + "shop load");
		}
	}

	private static boolean hasPermission(CommandSender player, String string) {
		// TODO Implement permission checking using the permissions plugin.
		if (string.equalsIgnoreCase("admin")) {
			if (((HumanEntity) player).getName().equalsIgnoreCase("jjfs85")) {
				return true;
			}
		}
		return false;
	}

	public final static void sendMessage(CommandSender player, String s) {
		player.sendMessage(messagePrefix + s);
	}
}
