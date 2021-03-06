/**
 * Copyright (C) 2011 Jacob Scott <jascottytechie@gmail.com>
 * Description: class for accessing a pricelist for bettershop
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.bettershop.shop;

import me.jascotty2.lib.io.FileIO;
import me.jascotty2.lib.io.CheckInput;
import me.jascotty2.lib.bukkit.item.JItem;
import me.jascotty2.lib.bukkit.item.JItemDB;
import me.jascotty2.lib.bukkit.shop.PriceList;

import me.jascotty2.bettershop.utils.BSPermissions;
import me.jascotty2.bettershop.utils.BetterShopLogger;
import me.jascotty2.bettershop.enums.BetterShopPermission;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import me.jascotty2.bettershop.BSConfig;
import me.jascotty2.bettershop.BSEcon;
import me.jascotty2.bettershop.BSutils;
import me.jascotty2.bettershop.BetterShop;
import me.jascotty2.lib.bukkit.config.Configuration;
import me.jascotty2.lib.bukkit.shop.ItemStock;
import org.bukkit.ChatColor;

import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;


public class BSPriceList extends PriceList {

	final Shop shop;
	ShopConfig config = null;

	//public BSPriceList(ShopConfig config) {
	public BSPriceList(Shop shop) {
		//this.stock = stock;
		// load the pricelist.
		// load();
		this.shop = shop;
		this.config = shop.config;
	}

	public final boolean load() {
		sortOrder = config.getCustomSort();
		useCache = config.useDBcaching();
		dbCacheTTL = config.pricelistCacheTime();
		if (config.useMySQL()) {
			try {
				//System.out.println("attempting MySQL");
				if (loadMySQL(config.sql_database,
						config.tableName,
						config.sql_username,
						config.sql_password,
						config.sql_hostName,
						config.sql_portNum)) {
					BetterShopLogger.Log(Level.INFO, "MySQL database " + pricelistName() + " loaded.");
					loadOldPricelist();
					return true;
				}
			} catch (Exception ex) {
				BetterShopLogger.Log(Level.SEVERE, ex, false);
			}
			BetterShopLogger.Log(Level.SEVERE, "Failed to connect to MySQL database "
					+ config.sql_database, false);

		} else {
			try {
				if (loadFile(new File(BSConfig.pluginFolder.getPath() + File.separatorChar
						+ config.tableName + ".csv"))) {
					BetterShopLogger.Log(Level.INFO, config.tableName + ".csv loaded.");
					loadOldPricelist();
					return true;
				}
			} catch (IOException ex) {
				BetterShopLogger.Log(Level.SEVERE, ex, false);
			} catch (Exception ex) {
				BetterShopLogger.Log(Level.SEVERE, ex);
			}
			BetterShopLogger.Log(Level.SEVERE, "Failed to load pricelist database "
					+ config.tableName + ".csv ", false);
		}
		return false;
	}

	public void loadOldPricelist() {
		File oldFile = new File(BSConfig.pluginFolder.getPath() + File.separatorChar + "PriceList.yml");
		if (loadOldPricelist(oldFile)) {
			oldFile.renameTo(new File(BSConfig.pluginFolder.getPath() + File.separatorChar + "OLD_PriceList.yml"));
			BetterShopLogger.Log("Old Pricelist Imported Successfully ");
		}
	}

	public boolean loadOldPricelist(File oldFile) {
		if (oldFile.exists()) {
			try {
				BetterShopLogger.Log("Found old PriceList.yml file");
				Configuration pricelistconfig = new Configuration(oldFile);
				//YamlConfiguration pricelistconfig = YamlConfiguration.loadConfiguration(oldFile);
				pricelistconfig.load();
				if (pricelistconfig.getNode("prices") != null) {
					for (String itn : pricelistconfig.getKeys("prices")) {
						if (itn.startsWith("item")) {
							int id = 0, sub = 0;
							if (itn.contains("sub")) {
								id = CheckInput.GetInt(itn.substring(4, itn.indexOf("sub")), 0);
								sub = CheckInput.GetInt(itn.substring(itn.indexOf("sub") + 3), 0);
							} else {
								id = CheckInput.GetInt(itn.substring(4), 0);
							}
							JItem toAdd = JItemDB.GetItem(id, (byte) sub);
							if (toAdd != null) {
								setPrice(toAdd, pricelistconfig.getDouble("prices." + itn + ".buy", -1),
										pricelistconfig.getDouble("prices." + itn + ".sell", -1));
							} else {
								BetterShopLogger.Log("Invalid Item: " + itn);
							}
						} else {
							BetterShopLogger.Log("Invalid Item: " + itn);
						}
					}
				}
				save();
				return true;
			} catch (Exception ex) {
				BetterShopLogger.Log(Level.SEVERE, ex, false);
			}
		}
		return false;
	}

	public boolean setPrice(String item, String b, String s) {
		return setPrice(JItemDB.findItem(item),
				CheckInput.GetDouble(b, -1),
				CheckInput.GetDouble(s, -1));
	}

	public boolean setPrice(JItem item, String b, String s) {
		return setPrice(item,
				CheckInput.GetDouble(b, -1),
				CheckInput.GetDouble(s, -1));
	}

	@Override
	public boolean setPrice(JItem item, double b, double s) {
		try {
			return super.setPrice(item, b, s);
		} catch (SQLException ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
		} catch (IOException ex) {
			BetterShopLogger.Log(Level.SEVERE, ex, false);
		} catch (Exception ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
		}
		return false;
	}

	public double itemBuyPrice(Player player, JItem toBuy, int amt) {
		return toBuy == null ? Double.NEGATIVE_INFINITY
				: itemBuyPrice(player, toBuy.ID(), (byte) toBuy.Data(), amt);
	}

	public double itemBuyPrice(Player player, int id, byte dat, int amt) {
		try {
			double b = getBuyPrice(id, dat);
			if (b > 0) {
				b -= BSEcon.getPlayerDiscount(player) * b;
			}
			return b * amt;
		} catch (Exception ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
			BSutils.sendMessage(player, "Error looking up price"); // .. Attempting DB reload..
            /*if (load(null)) {
			// ask to try command again.. don't want accidental infinite recursion & don't want to plan for recursion right now
			BSutils.sendMessage(player, "Success! Please try again.. ");
			} else {
			BSutils.sendMessage(player, ChatColor.RED + "Failed! Please let an OP know of this error");
			}*/
			return Double.NEGATIVE_INFINITY;
		}
	}

	public double itemSellPrice(Player player, JItem toSell, int amt) {
		return toSell == null ? Double.NEGATIVE_INFINITY
				: itemSellPrice(player, toSell.ID(), (byte) toSell.Data(), amt);
	}

	public double itemSellPrice(Player player, ItemStack toSell, int amt) {
		return toSell == null ? Double.NEGATIVE_INFINITY
				: itemSellPrice(player, toSell.getTypeId(),
				toSell.getData() == null ? 0 : toSell.getData().getData(), amt);
	}

	public double itemSellPrice(Player player, int id, byte dat, int amt) {
		try {
			double s = getSellPrice(id, dat);
			if (s > 0) {
				switch(BetterShop.getSettings().discountSellingMethod) {
					case LOWER:
						s -= BSEcon.getPlayerDiscount(player) * s;
						break;
					case HIGHER:
						s += BSEcon.getPlayerDiscount(player) * s;
						break;
				}
			}
			return s * amt;
		} catch (Exception ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
			BSutils.sendMessage(player, "Error looking up price"); // .. Attempting DB reload..
			return Double.NEGATIVE_INFINITY;
		}
	}

	public List<String> GetShopListPage(int pageNum, CommandSender player, ItemStock stock) {
		try {
			return getShopListPage(pageNum, player instanceof Player,
					config.getPageSize(),
					config.getListFormat(),
					config.getListHead(),
					config.getListTail(),
					config.allowIllegalPurchase() || BSPermissions.hasPermission(player, BetterShopPermission.ADMIN_ILLEGAL, false),
					true, stock,
					player instanceof Player ? BSEcon.getPlayerDiscount((Player) player) : 0);
		} catch (Exception ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
		}
		// an error occured: let player know
		LinkedList<String> ret = new LinkedList<String>();
		ret.add(ChatColor.RED + "An Error Occurred while retrieving pricelist.. ");
		ret.add(ChatColor.RED + "  see the server log or let an OP know of this error");
		return ret;
	}

	public boolean restoreDB(File toImport) {
		try {
			removeAll();
			return importDB(toImport);
		} catch (IOException ex) {
			BetterShopLogger.Log(Level.SEVERE, ex, false);
		} catch (Exception ex) {
			BetterShopLogger.Log(Level.SEVERE, ex);
		}
		return false;
	}

	public boolean importDB(File toImport) {
		if (toImport.getName().toLowerCase().endsWith(".yml")) {
			if (loadOldPricelist(toImport)) {
				return true;
			}
		} else {
			try {
				List<String[]> db = FileIO.loadCSVFile(toImport);
				int n = 0;//num = 0,
				for (String fields[] : db) {
					if (fields.length > 3) {
						if (fields.length > 3) {
							//JItem plItem = JItem.findItem(fields[0] + ":" + (fields[1].length() == 0 ? "0" : fields[1]));
							JItem plItem = JItemDB.findItem(fields[0] + ":" + (fields[1].equals(" ") ? "0" : fields[1]));
							if (plItem != null) {
								setPrice(plItem,
										fields[2].equals(" ") ? -1 : CheckInput.GetDouble(fields[2], -1),
										fields[3].equals(" ") ? -1 : CheckInput.GetDouble(fields[3], -1));
								//++num;
							} else if (n > 0) { // first line is expected invalid: is title
								BetterShopLogger.Log(Level.WARNING, String.format("Invalid item on line %d in %s", (n + 1), toImport.getName()));
							}
						} else {
							BetterShopLogger.Log(Level.WARNING, String.format("unexpected pricelist line at %d in %s", (n + 1), toImport.getName()));
						}
					}
				}
				return true;
			} catch (IOException ex) {
				BetterShopLogger.Log(Level.SEVERE, ex, false);
			} catch (Exception ex) {
				BetterShopLogger.Log(Level.SEVERE, ex);
			}
		}
		return false;
	}

	/**
	 * without doing an inventory check, <br/>
	 * instead check for how much of this item can be bought
	 * @param player
	 * @param cost
	 * @param toBuy
	 * @return
	 */public int getAmountCanBuy(Player player, double cost, JItem toBuy) {
		 return getAmountCanBuy(player, cost, toBuy, -1);
	 }
	public int getAmountCanBuy(Player player, double cost, JItem toBuy, double customPrice) {
		double price = customPrice >= 0 ? customPrice : itemBuyPrice(player, toBuy, 1);
		if (price == 0) {
			return -1;
		} else if (price < 0 || cost <= 0) {
			return 0;
		}
		long stock = -1;
		try {
			stock = shop.stock.getItemAmount(toBuy);
		} catch (Exception ex) {
			BetterShopLogger.Severe("Error in Stock Database", ex, false);
		}
		
		long amt = (long)(cost / price);
		if(amt > stock){
			amt = stock;
		}
		if(amt > Integer.MAX_VALUE){
			return Integer.MAX_VALUE;
		} else {
			return (int) amt;
		}
	}

	public int getAmountCanBuy(Player player, JItem toBuy) {
		return getAmountCanBuy(player, toBuy, -1);
	}
	
	public int getAmountCanBuy(Player player, JItem toBuy, double customPrice) {
		int canHold = BSutils.amtCanHold(player, toBuy);

		if (canHold <= 0) {
			return 0;
		}
		double bal = BSEcon.getBalance(player);
		int canAfford = getAmountCanBuy(player, bal, toBuy, customPrice);
		
		return canAfford > canHold ? canHold : canAfford;
	}
	
	public static int amtCanBuy(Player player, JItem toBuy) {
		return player == null ? 0 : 
			BetterShop.getPricelist(player.getLocation()).getAmountCanBuy(player, toBuy);
	}

}
