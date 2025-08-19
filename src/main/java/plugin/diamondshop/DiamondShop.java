package plugin.diamondshop;
import net.luckperms.api.model.user.User;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public final class DiamondShop extends JavaPlugin implements Listener {
    private boolean isThere;
    int defaultMaxShops = getConfig().getInt("MaxShops");
    private boolean lpEnabled;
    private HashMap<String,Integer> Shops = new HashMap<>();
    private YamlConfiguration cfg;
    private File shops;
    private boolean lp;



    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);

        lpEnabled = getConfig().getBoolean("LuckPermsEnabled");
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
        Objects.requireNonNull(getCommand("diamondshop")).setExecutor(new Commands());

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            lp = true;
        } else {
            getLogger().info("LuckPerms not found, going with default configuration.");
        }
        if (lp && lpEnabled){
            getLogger().warning("Luckperms integration with DiamondShop is an experimental feature. If any issues arise, please report them at https://github.com/WizarTheGreat/DiamondShop/issues");
        }

        int resourceId = 118064;
        UpdateChecker checker = new UpdateChecker(this, resourceId);
        String currentVersion = getDescription().getVersion();
        checker.checkForUpdate((latestVersion) -> {
            if (latestVersion != null && !latestVersion.equalsIgnoreCase(getDescription().getVersion())) {
                getLogger().warning("=================================");
                getLogger().warning("A new version of DiamondShop is available!");
                getLogger().warning("Current: " + currentVersion + " | Latest: " + latestVersion);
                getLogger().warning("Download it from: https://www.spigotmc.org/resources/" + resourceId);
                getLogger().warning("=================================");
            }
        });
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
    public int getGroupMax(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        try {
            User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).get();
            String primaryGroup = user.getPrimaryGroup();
            if(!primaryGroup.equals("default")) {
                int maxShops = getConfig().getInt(primaryGroup);
                return maxShops;
            }else
                return defaultMaxShops;
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().warning("COULD NOT RETRIEVE LUCKPERMS, GOING WITH DEFAULT");
            return defaultMaxShops;
        }
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UpdateChecker checker = new UpdateChecker(this, 118064);
        if(player.isOp()) {
            checker.checkForUpdate((latestVersion) -> {
                if (latestVersion != null && !latestVersion.equalsIgnoreCase(getDescription().getVersion())) {
                    player.sendMessage("§cYour version of diamondshop is outdated!");
                    player.sendMessage("§cA new version is available: " + latestVersion);
                    player.sendMessage("§cDownload it here: https://www.spigotmc.org/resources/118064");
                }

            });
            if(lp && lpEnabled){
                player.sendMessage("Luckperms integration with DiamondShop is an experimental feature. If any issues arise, please report them at https://github.com/WizarTheGreat/DiamondShop/issues");
            }
        }
    }
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        String[] lines = e.getLines();


        if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds")) && defaultMaxShops > 0) {
            if (!lines[3].equalsIgnoreCase(player.getName())) {
                e.setCancelled(true);
                player.sendMessage("You cannot create a shop for someone else.");
            } else if (Shops.containsKey(player.getName())) {
                Integer value = Shops.get(player.getName());
                //Running with Luckperms
                if (lp && lpEnabled){
                    if (value == getGroupMax(player)) {
                        e.setCancelled(true);
                        player.sendMessage("You are already maxed out on shops!");
                    } else {
                        Shops.replace(player.getName(), value + 1);
                        player.sendMessage("You are now at " + Shops.get(player.getName()) + "/" + getGroupMax(player) + " shops");
                    }
                    //Default run without Luckperms
                }else {
                    if (value == defaultMaxShops) {
                        e.setCancelled(true);
                        player.sendMessage("You are already maxed out on shops!");
                    } else {
                        Shops.replace(player.getName(), value + 1);
                        player.sendMessage("You are now at " + Shops.get(player.getName()) + "/" + defaultMaxShops + " shops");
                    }
                }
                }
            }
        }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        Material type = block.getBlockData().getMaterial();
        if (type.toString().contains("SIGN") && (defaultMaxShops > 0)) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();
            if (lines[0].equalsIgnoreCase("Shop") && (lines[2].contains("Diamond") || lines[2].contains("Diamonds"))) {
                if(Shops.containsKey(lines[3])){
                    Shops.replace(lines[3],Shops.get(lines[3]) - 1);
                    if (player.getUniqueId() == Bukkit.getOfflinePlayer(lines[3]).getUniqueId()){
                        if(lp && lpEnabled){
                            player.sendMessage("You are now at " + Shops.get(player.getName()) + "/" + getGroupMax(player) + " shops");
                        }
                        else{
                        player.sendMessage("You are now at " + Shops.get(player.getName()) + "/" + defaultMaxShops + " shops");
                    }}
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
                            player.sendMessage("§4You do not have enough diamonds in your hand!");
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
                    player.sendMessage("§4The sign format is incorrect!");
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
                            player.sendMessage("§4This isn't your shop!");
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
        boolean enough = false;
        boolean open = false;
        int stacks = 0;
        int count = 0;
        int openSlots = 0;
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
            player.sendMessage("§4Improper format! Need number then item.");
            return false;
        }

        Material itemType = Material.getMaterial(itemDetails[1].toUpperCase());
        String itemName = itemDetails[1].toUpperCase();
        if (itemType == null && !(itemName.length() > 10)) {
            player.sendMessage("§4This isn't a sellable item.");
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
                    player.sendMessage("§4This shop is out of stock!");
                }

                return false;
            } else {
                player.sendMessage("§4You don't have enough space.");
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