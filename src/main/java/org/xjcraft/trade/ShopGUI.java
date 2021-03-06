package org.xjcraft.trade;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.xjcraft.database.CustomItem;
import org.xjcraft.database.Trade;
import org.xjcraft.util.ItemUtil;
import org.xjcraft.util.SerializeUtil;
import org.xjcraft.util.SqlUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Weiyuan on 2016/1/12.
 */
public class ShopGUI {
    public static void shopGUI(Plugin plugin, Player player, Material shopType, short durability, int moneyPrice, int itemPrice,
                               int sellNumber, int buyNumber, boolean itemSize, boolean moneySize) {
        shopGUI(plugin, player, shopType.name() + ":" + durability, moneyPrice, itemPrice, sellNumber, buyNumber, itemSize, moneySize);
    }

    public static void shopGUI(Plugin plugin, Player player, String name, int moneyPrice, int itemPrice,
                               int sellNumber, int buyNumber, boolean itemSize, boolean moneySize) {
        String[] names = name.split(":");
        ItemStack shopType;
        short durability;
        if (names.length == 2 && names[0].equalsIgnoreCase("S")) {
            CustomItem custom = SqlUtil.getEbeanServer().find(CustomItem.class).where().ieq("name", names[1]).findUnique();
            shopType = SerializeUtil.deSerialization(custom.getFlatItem());
            durability = 0;
        } else {
            Material material = Material.getMaterial(names[0]);
            durability = Short.parseShort(names[1]);
            shopType = new ItemStack(material, 1, durability);

        }


        itemPrice = itemPrice % 10000;
        moneyPrice = moneyPrice % 10000;
        if (itemPrice == 0) {
            itemPrice = 1;
        }
        if (moneyPrice == 0) {
            moneyPrice = 1;
        }

        sellNumber = sellNumber % 1000;
        buyNumber = buyNumber % 1000;
        if (sellNumber == 0) {
            sellNumber = 1;
        }
        if (buyNumber == 0) {
            buyNumber = 1;
        }
        Inventory menu = Bukkit.createInventory(null, 54, plugin.getConfig().getString("shop.name"));
        ItemStack[] itemStacks = menu.getContents();

        for (int i = 0; i < 4; i++) {
            itemStacks[12 - i] = ItemUtil.getNumberStack(itemPrice / (int) Math.pow(10, (double) i) % 10);
            itemStacks[17 - i] = ItemUtil.getNumberStack(moneyPrice / (int) Math.pow(10, (double) i) % 10);
            if (i != 3) {
                itemStacks[38 - i] = ItemUtil.getNumberStack(sellNumber / (int) Math.pow(10, (double) i) % 10);
                itemStacks[44 - i] = ItemUtil.getNumberStack(buyNumber / (int) Math.pow(10, (double) i) % 10);
            }
        }

        int[] upArrow = {0, 1, 2, 3, 5, 6, 7, 8, 27, 28, 29, 33, 34, 35};
        for (int i : upArrow) {
            itemStacks[i] = ItemUtil.getUpArrow();
        }

        int[] downArrow = {18, 19, 20, 21, 23, 24, 25, 26, 45, 46, 47, 51, 52, 53};
        for (int i : downArrow) {
            itemStacks[i] = ItemUtil.getDownArrow();
        }

        ItemStack isStack = new ItemStack(shopType.getType(), 1, shopType.getDurability());
        if (itemSize) {
            isStack.setAmount(shopType.getMaxStackSize());
        } else {
            isStack.setAmount(1);
        }
        itemStacks[39] = isStack;

        if (moneySize) {
            itemStacks[41] = new ItemStack(ItemUtil.getCurrency(), ItemUtil.getCurrency().getMaxStackSize());
        } else {
            itemStacks[41] = new ItemStack(ItemUtil.getCurrency(), 1);
        }

        itemStacks[13] = ItemUtil.button(shopType, String.format(plugin.getConfig().getString("message.priceButton"), itemPrice, name, moneyPrice));
        ItemMeta itemMeta13 = itemStacks[13].getItemMeta();
        List<String> list13 = new ArrayList<String>();
        list13.add(getLowest(plugin, name, durability));
        list13.add(getHighest(plugin, name, durability));
        itemMeta13.setLore(list13);
        itemStacks[13].setItemMeta(itemMeta13);
        itemStacks[13].setDurability(durability);

        itemStacks[40] = ItemUtil.getDetail(name, moneyPrice, itemPrice, sellNumber, buyNumber, itemSize, moneySize);

        if (itemSize) {
            sellNumber = sellNumber * shopType.getMaxStackSize();
        }
        if (moneySize) {
            buyNumber = buyNumber * ItemUtil.getCurrency().getMaxStackSize();
        }

        // 买卖键
        itemStacks[48] = ItemUtil.button(shopType,
                String.format(plugin.getConfig().getString("message.sellButton"), sellNumber, name, sellNumber * moneyPrice / itemPrice));
        itemStacks[50] = ItemUtil.button(ItemUtil.getCurrency(),
                String.format(plugin.getConfig().getString("message.buyButton"), name, buyNumber, itemPrice, moneyPrice));

        // 库存显示
        itemStacks[30] = ItemUtil.button(Material.CHEST,
                String.format(plugin.getConfig().getString("message.itemOwned"), ItemUtil.getItemNumber(player, shopType), name));
        itemStacks[32] = ItemUtil.button(Material.TRAPPED_CHEST,
                String.format(plugin.getConfig().getString("message.moneyOwned"), ItemUtil.getItemNumber(player, ItemUtil.getCurrency())));

//        // 最低卖价
//        itemStacks[4] = getLowest(plugin, shopType);
//
//        // 最高售价
//        itemStacks[22] = getHighest(plugin, shopType);

        menu.setContents(itemStacks);
        player.openInventory(menu);
    }


    private static String getHighest(Plugin plugin, String name, short durability) {
        name = getRealName(name);
        ItemStack itemStack = ItemUtil.getHighest();
        ItemMeta itemMetaH = itemStack.getItemMeta();
//        plugin.getLogger().info(name + "," + durability);
        Trade tradeH = SqlUtil.getFirst(
                SqlUtil.getEbeanServer().find(Trade.class).where().ieq("material", name).ieq("durability", durability + "").ieq("sell", "0").orderBy().desc("price").orderBy().asc("id").findList());
        if (tradeH != null) {
            itemMetaH.setDisplayName(String.format(plugin.getConfig().getString("message.highest"), tradeH.getItemPrice(), tradeH.getMoneyPrice(), tradeH.getPlayer()));
        } else {
            itemMetaH.setDisplayName(plugin.getConfig().getString("message.nohighest"));
        }
        itemStack.setItemMeta(itemMetaH);
        return itemMetaH.getDisplayName();
    }

    private static String getLowest(Plugin plugin, String name, short durability) {
//        String names[]= name.split(":");
        name = getRealName(name);
        ItemStack itemStack = ItemUtil.getLowest();
        ItemMeta itemMeta = itemStack.getItemMeta();
        Trade trade = SqlUtil.getFirst(
                SqlUtil.getEbeanServer().find(Trade.class).where().ieq("material", name).ieq("durability", durability + "").ieq("sell", "1").orderBy().asc("price").orderBy().asc("id").findList());
        if (trade != null) {
            itemMeta.setDisplayName(String.format(plugin.getConfig().getString("message.lowest"), trade.getItemPrice(), trade.getMoneyPrice(), trade.getPlayer()));
        } else {
            itemMeta.setDisplayName(plugin.getConfig().getString("message.nolowest"));
        }
        itemStack.setItemMeta(itemMeta);
        return itemMeta.getDisplayName();
    }

    private static String getRealName(String name) {
        String[] names = name.split(":");
        if (names.length == 2 && names[0].equalsIgnoreCase("S")) {
            return name;
        } else return names[0];
    }
}
