package plugin.diamondshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DiamondShop extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        Material type = Objects.requireNonNull(e.getClickedBlock()).getType();
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (type.toString().contains("SIGN")) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            String[] lines = sign.getLines();
            if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                try {
                    int number = Integer.parseInt(lines[2].split(" ")[0]);
                    if (!lines[3].equalsIgnoreCase(player.getName())) {
                        if (removeDiamonds(number, item)) {
                            if (replaceItemInChest(sign, number, e.getPlayer())) {
                                player.sendMessage("You bought the items.");
                                e.setCancelled(true);
                            } else {
                                player.sendMessage("Out of items.");
                                e.setCancelled(true);
                            }
                        } else {
                            player.sendMessage("You do not have enough diamonds in your hand!");
                            e.setCancelled(true);
                        }
                    }
                    if (lines[3].equalsIgnoreCase(player.getName())) {
                        if (e.getAction().toString().contains("RIGHT_CLICK")) {
                            player.sendMessage("Shop was made successfully.");
                            e.setCancelled(true);
                        }
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage("The sign format is incorrect!");
                }
            }
        } else if (type == (Material.CHEST)) {
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : faces) {
                Block relativeBlock = e.getClickedBlock().getRelative(face);
                if (relativeBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) relativeBlock.getState();
                    String[] lines = sign.getLines();
                    if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                        if (!lines[3].equalsIgnoreCase(player.getName())) {
                            player.sendMessage("This isn't your shop!");
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    private boolean removeDiamonds(int number, ItemStack item) {
        if (item != null && item.getType() == Material.DIAMOND) {
            int amount = item.getAmount();
            return amount >= number;
        }
        return false;
    }
    private boolean replaceItemInChest(Sign sign, int number, Player player) {
        BlockData blockData = sign.getBlockData();
        if (!(blockData instanceof Directional)) {
            return false;
        }

        Directional directional = (Directional) blockData;
        BlockFace facing = directional.getFacing().getOppositeFace();
        Block chestBlock = sign.getBlock().getRelative(facing);
        if (!(chestBlock.getState() instanceof Chest)) {
            return false;
        }

        Chest chest = (Chest) chestBlock.getState();
        Inventory chestInventory = chest.getInventory();

        String[] itemDetails = sign.getLine(1).split(" ");
        if (itemDetails.length < 2) {
            return false;
        }

        Material itemType = Material.getMaterial(itemDetails[1].toUpperCase());
        if (itemType == null) {
            player.sendMessage("Out of stock.");
            return false;
        }

        int itemAmount;
        try {
            itemAmount = Integer.parseInt(itemDetails[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        for (int i = 0; i < chestInventory.getSize(); i++) {
            ItemStack item = chestInventory.getItem(i);
            if (item != null && item.getType() == itemType && item.getAmount() >= itemAmount) {
                item.setAmount(item.getAmount() - itemAmount);
                chestInventory.setItem(i, item);

                ItemStack diamonds = new ItemStack(itemType, itemAmount);
                addItemToPlayerInventory(player, diamonds);
                player.getItemInHand().setAmount(player.getItemInHand().getAmount() - number);
                return true;

            }
        }
        return false;
    }
    private void addItemToPlayerInventory(Player player, ItemStack item) {
        Inventory playerInventory = player.getInventory();
        ItemStack firstSlotItem = playerInventory.getItem(0);

        if (firstSlotItem == null || firstSlotItem.getType() == Material.AIR) {
            playerInventory.setItem(0, item);
        } else {
            player.getInventory().addItem(item);
        }
    }
}
//Made by WizarTheGreat