package plugin.diamondshop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public final class DiamondShop extends JavaPlugin implements Listener {
    private boolean isThere;
    private boolean open;
    private int stacks;
    private int count;
    private int openSlots;
    private boolean enough;
    private int MaxShops = getConfig().getInt("MaxShops");
    private HashMap<String,Integer> Shops = new HashMap<>();
    private YamlConfiguration cfg;
    private File shops;



    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        openSlots = 0;
        shops = new File(getDataFolder(), "shops.yml");
        if(!shops.exists()) {
            try {
                shops.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create shops.yml file!");
            }
        }
        cfg = YamlConfiguration.loadConfiguration(shops);
        for (String key : cfg.getKeys(false)) {
            Shops.put(key, cfg.getInt(key));
        }
    }

    @Override
    public void onDisable() {
        for (String key : Shops.keySet()){
            cfg.set(key, Shops.get(key));
        }
        try {
            cfg.save(shops);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @EventHandler
    public void onSignChange(SignChangeEvent e){
        Player player = e.getPlayer();
        String[] lines = e.getLines();


        if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds")) && MaxShops > 0) {
            if (!lines[3].equalsIgnoreCase(player.getName())){
                e.setCancelled(true);
                player.sendMessage("You cannot create a shop for someone else.");
            }else  if (Shops.containsKey(player.getName())) {
                Integer value = Shops.get(player.getName());
                if (value == MaxShops){
                    e.setCancelled(true);
                    player.sendMessage("You are already maxed out on shops!");
                } else{
                getLogger().info("playername");
                    Shops.replace(player.getName(), value + 1);
                    player.sendMessage("You are now at " + Shops.get(player.getName() + "/" + MaxShops + "shops"));
                }
            }else {
                Shops.put(player.getName(), 1);
                player.sendMessage("You are at 1/" + MaxShops + " shops");
        }
        }
    }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Material type = block.getBlockData().getMaterial();
        if (type.toString().contains("SIGN") && MaxShops > 0) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();
            if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                if(Shops.containsKey(lines[3])){
                    Shops.replace(lines[3],Shops.get(lines[3]) - 1);
                    if (e.getPlayer().getUniqueId() == Bukkit.getOfflinePlayer(lines[3]).getUniqueId()){
                        e.getPlayer().sendMessage("You are now at " + Shops.get(e.getPlayer().getName()) + "/" + MaxShops + "shops");
                    }
                }
            }
        }
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        Iterator<Block> iterator = e.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Material type = block.getType();
            if (type.toString().contains("SIGN")) {
                Sign sign = (Sign) block.getState();
                String[] lines = sign.getLines();
                if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                    iterator.remove();
                }
            } else if (type == Material.CHEST) {
                BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
                for (BlockFace face : faces) {
                    Block relativeBlock = block.getRelative(face);
                    if (relativeBlock.getState() instanceof Sign) {
                        Sign sign = (Sign) relativeBlock.getState();
                        String[] lines = sign.getLines();
                        if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
        }
    }
    @EventHandler
    public void onHopper(InventoryMoveItemEvent e){
        if(e.getSource().getType().equals(InventoryType.CHEST)){
            BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : faces) {
                Block relativeBlock = Objects.requireNonNull(e.getSource().getLocation()).getBlock().getRelative(face);
                if (relativeBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) relativeBlock.getState();
                    String[] lines = sign.getLines();
                    if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                            e.setCancelled(true);
                    }
                }
            }

        }
    }
    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        Material type = Objects.requireNonNull(e.getClickedBlock()).getType();
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (type.toString().contains("SIGN")) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            String[] lines = sign.getLines();
            if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                try {
                    int number = Integer.parseInt(lines[2].split(" ")[0]);
                    if (!(Objects.requireNonNull(Bukkit.getOfflinePlayer(lines[3])).getUniqueId() == Objects.requireNonNull(player.getUniqueId()))) {
                        if (removeDiamonds(number, item)) {
                            if (replaceItemInChest(sign, number, e.getPlayer())) {
                                player.sendMessage("You bought the items.");
                                e.setCancelled(true);
                            } else {
                                e.setCancelled(true);
                            }
                        } else {
                            player.sendMessage("You do not have enough diamonds in your hand!");
                            e.setCancelled(true);
                        }
                    }

                    if (Objects.requireNonNull(Bukkit.getOfflinePlayer(lines[3])).getUniqueId() == Objects.requireNonNull(player.getUniqueId())) {
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
                        if ((!(Objects.requireNonNull(Bukkit.getOfflinePlayer(lines[3])).getUniqueId() == Objects.requireNonNull(player.getUniqueId())))) {
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
            player.sendMessage("Improper format! Need number then item.");
            return false;
        }

        Material itemType = Material.getMaterial(itemDetails[1].toUpperCase());
        String itemName = itemDetails[1].toUpperCase();
        if (itemType == null && !(itemName.length() > 10)) {
            player.sendMessage("This isn't a sellable item.");
            return false;
        }

        int itemAmount = Integer.parseInt(itemDetails[0]);

        if (itemAmount >= 64) {
            stacks = ((int) Math.floor((double) itemAmount / 64));
            itemAmount = itemAmount - (stacks * 64);
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                openSlots++;
            }
        }

        if (openSlots >= 1 + stacks){
            open = true;
        }
            if (open) {
                for (int i = 0; i < chestInventory.getSize(); i++) {
                    ItemStack item = chestInventory.getItem(i);
                    if (item != null && (item.getType() == itemType || item.getType().toString().contains(itemName))) {
                        if (item.getAmount() == 64) {
                            count++;
                            isThere = true;
                        } else if ((item.getAmount() == itemAmount || itemAmount == 0) && stacks <= count) {
                            enough = true;
                            isThere = true;
                            if (itemAmount > 0) {
                                item.setAmount(0);
                                ItemStack sale = new ItemStack(itemType, itemAmount);
                                addItemToPlayerInventory(player, sale);
                            }
                            break;
                        }
                    }
                }
                if(enough) {
                    for (int i = 0; i < chestInventory.getSize(); i++) {
                        ItemStack item = chestInventory.getItem(i);
                        if (stacks > 0 && count >= stacks) {
                            if (item != null && (item.getType() == itemType || item.getType().toString().contains(itemName)) && item.getAmount() == 64) {
                                item.setAmount(0);
                                stacks--;
                                ItemStack full = new ItemStack(itemType, 64);
                                addItemToPlayerInventory(player, full);

                            }
                        } else {
                            break;
                        }
                    }
                    ItemStack diamonds = new ItemStack(Material.DIAMOND, number);
                    player.getItemInHand().setAmount(player.getItemInHand().getAmount() - number);
                    chestInventory.addItem(diamonds);
                }else{
                    isThere = false;
                }
                if (isThere) {
                    isThere = false;
                    return true;
                } else {
                    player.sendMessage("This shop is out of stock!");
                }

                open = false;
                stacks = 0;
                count = 0;
                openSlots = 0;
                return false;
            } else {
                player.sendMessage("You don't have enough space.");
            }
            return false;

        }
        private void addItemToPlayerInventory (Player player, ItemStack item){
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