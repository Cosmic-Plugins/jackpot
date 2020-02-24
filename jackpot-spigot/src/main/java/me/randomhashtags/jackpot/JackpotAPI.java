package me.randomhashtags.jackpot;

import me.randomhashtags.jackpot.event.JackpotPurchaseTicketsEvent;
import me.randomhashtags.jackpot.supported.Vault;
import me.randomhashtags.jackpot.universal.UInventory;
import me.randomhashtags.jackpot.universal.UVersionable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public enum JackpotAPI implements Listener, CommandExecutor, UVersionable {
    INSTANCE;
    
    private boolean isEnabled;
    
    private ItemStack item;
    private ItemMeta itemMeta;
    private List<String> lore;

    private File otherdataF;
    private YamlConfiguration otherdata;
    public YamlConfiguration CONFIG;
    private Economy eco;
    
    public int task;
    public List<Integer> countdownTasks;

    private UInventory gui;
    private List<Integer> confirmSlots, cancelSlots;
    public BigDecimal tax, ticketCost, maxTickets, minTickets, playersPerPage, value;
    public long winnerPickedEvery, pickNextWinner;
    public HashMap<UUID, BigDecimal> ticketsSold, top, purchasing;

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        final int l = args.length;
        if(l == 0) {
            view(sender);
        } else {
            final String a = args[0];
            switch (a) {
                case "pickwinner":
                    if(sender.hasPermission("Jackpot.pickwinner")) {
                        pickWinner();
                    }
                    break;
                case "stats":
                    if(player != null) {
                        viewStats(player);
                    }
                    break;
                case "top":
                    viewTop(sender, l == 1 ? 1 : getRemainingInt(args[1]));
                    break;
                case "buy":
                    final List<String> b = getStringList(CONFIG, "messages.enter valid ticket amount");
                    if(l == 1) {
                        sendStringListMessage(player, b, null);
                    } else {
                        final BigDecimal amount = BigDecimal.valueOf(getRemainingDouble(args[1]));
                        final int amt = amount.intValue();
                        if(amt < minTickets.intValue() || amt > maxTickets.intValue()) {
                            sendStringListMessage(player, b, null);
                        } else {
                            confirmPurchaseTickets(player, amount);
                        }
                    }
                    break;
                case "toggle":
                case "stfu":
                    if(player != null) {
                        tryToggleNotifications(player);
                    }
                    break;
                default:
                    viewHelp(sender);
                    break;
            }
        }
        return true;
    }

    public void load() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        final long started = System.currentTimeMillis();
        
        PLUGIN_MANAGER.registerEvents(this, JACKPOT);
        
        item = new ItemStack(Material.APPLE);
        itemMeta = item.getItemMeta();
        lore = new ArrayList<>();

        eco = Vault.getVault().getEconomy();

        CONFIG = YamlConfiguration.loadConfiguration(new File(DATA_FOLDER, "config.yml"));
        save(null, "_data.yml");
        otherdataF = new File(DATA_FOLDER, "_data.yml");
        otherdata = YamlConfiguration.loadConfiguration(otherdataF);

        gui = new UInventory(null, CONFIG.getInt("gui.size"), colorize(CONFIG.getString("gui.title")));
        final Inventory gi = gui.getInventory();
        final ItemStack confirm = createItemStack(CONFIG, "gui.confirm"), cancel = createItemStack(CONFIG, "gui.cancel");
        confirmSlots = new ArrayList<>();
        cancelSlots = new ArrayList<>();
        for(String s : getConfigurationSectionKeys(CONFIG, "gui", false)) {
            if(!s.equals("title") && !s.equals("size") && !s.equals("confirm") && !s.equals("cancel")) {
                final int slot = CONFIG.getInt("gui." + s + ".slot");
                final String i = CONFIG.getString("gui." + s + ".item").toLowerCase();
                final boolean isConfirm = i.equals("confirm"), isCancel = i.equals("cancel");
                if(isConfirm) {
                    confirmSlots.add(slot);
                } else if(isCancel) {
                    cancelSlots.add(slot);
                }
                item = isConfirm ? confirm : isCancel ? cancel : createItemStack(CONFIG, "gui." + s);
                gi.setItem(slot, item);
            }
        }

        tax = BigDecimal.valueOf(CONFIG.getDouble("settings.tax"));
        ticketCost = BigDecimal.valueOf(CONFIG.getInt("settings.ticket cost"));
        maxTickets = BigDecimal.valueOf(CONFIG.getInt("settings.max tickets"));
        minTickets = BigDecimal.valueOf(CONFIG.getInt("settings.min tickets"));
        playersPerPage = BigDecimal.valueOf(CONFIG.getInt("messages.players per page"));
        winnerPickedEvery = CONFIG.getInt("settings.winner picked every");
        ticketsSold = new HashMap<>();
        top = new HashMap<>();
        purchasing = new HashMap<>();

        final long e = otherdata.getLong("jackpot.pick next winner");
        pickNextWinner = e == 0 ? started+winnerPickedEvery*1000 : e;

        value = BigDecimal.valueOf(otherdata.getDouble("jackpot.value"));
        for(String s : getConfigurationSectionKeys(otherdata, "jackpot", false, "value", "pick next winner")) {
            final UUID uuid = UUID.fromString(s);
            final BigDecimal b = BigDecimal.valueOf(otherdata.getInt("jackpot." + s));
            ticketsSold.put(uuid, b);
        }
        startTask(started);
        sendConsoleDidLoadFeature("Jackpot", started);
    }
    public void unload() {
        if(isEnabled) {
            isEnabled = false;
            HandlerList.unregisterAll(this);

            otherdata.set("jackpot", null);
            otherdata.set("jackpot.pick next winner", pickNextWinner);
            otherdata.set("jackpot.value", value.doubleValue());
            for(UUID uuid : ticketsSold.keySet()) {
                otherdata.set("jackpot." + uuid.toString(), ticketsSold.get(uuid).intValue());
            }
            saveOtherData();
            SCHEDULER.cancelTask(task);
            for(int i : countdownTasks) {
                SCHEDULER.cancelTask(i);
            }
            for(JPlayer player : JPlayer.CACHED_PLAYERS.values()) {
                player.backup();
            }
        }
    }

    private void saveOtherData() {
        try {
            otherdata.save(otherdataF);
            otherdataF = new File(DATA_FOLDER, "_data.yml");
            otherdata = YamlConfiguration.loadConfiguration(otherdataF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pickWinner() {
        final List<UUID> tickets = getTickets();
        final int size = tickets.size();
        if(size > 0) {
            final UUID winner = tickets.get(RANDOM.nextInt(size));
            final OfflinePlayer op = Bukkit.getOfflinePlayer(winner);
            final HashMap<String, String> replacements = new HashMap<>();
            final BigDecimal winnerTickets = ticketsSold.get(winner), taxed = value.multiply(tax), total = value.subtract(taxed);
            eco.depositPlayer(op, total.doubleValue());

            final JPlayer pdata = JPlayer.get(winner);
            pdata.totalWins = pdata.totalWins.add(BigDecimal.ONE);
            pdata.totalWonCash = pdata.totalWonCash.add(total);

            final String percent = formatDouble(getPercent(winnerTickets, size)), tt = formatInt(size);
            replacements.put("{PLAYER}", op.getName());
            replacements.put("{TICKETS}", formatBigDecimal(winnerTickets));
            replacements.put("{TICKETS%}", percent);
            replacements.put("{TOTAL_TICKETS}", tt);
            replacements.put("{$}", formatBigDecimal(total));

            final Collection<? extends Player> o = Bukkit.getOnlinePlayers();
            for(String s : getStringList(CONFIG, "messages.won")) {
                for(String r : replacements.keySet()) {
                    s = s.replace(r, replacements.get(r));
                }
                for(Player p : o) {
                    p.sendMessage(s);
                }
            }
            value = BigDecimal.ZERO;
            ticketsSold.clear();
        }
        final long time = System.currentTimeMillis();
        pickNextWinner = time+winnerPickedEvery*1000;
        startTask(time);
    }
    public List<UUID> getTickets() {
        final List<UUID> list = new ArrayList<>();
        for(UUID uuid : ticketsSold.keySet()) {
            for(int i = 1; i <= ticketsSold.get(uuid).intValue(); i++) {
                list.add(uuid);
            }
        }
        return list;
    }
    public double getPercent(@NotNull BigDecimal tickets, int size) {
        final BigDecimal big = BigDecimal.valueOf((double) (size == 0 ? 1 : size)), hundred = BigDecimal.valueOf(100);
        return round(BigDecimal.valueOf(tickets.doubleValue()/big.doubleValue()).multiply(hundred).doubleValue(), 2);
    }
    private void broadcastCountdown(long timeleft) {
        final List<String> msg = getStringList(CONFIG, "messages.countdown");
        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{TIME}", getRemainingTime(timeleft));
        replacements.put("{$}", formatBigDecimal(value));
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(JPlayer.get(player.getUniqueId()).doesReceiveNotifications()) {
                sendStringListMessage(player, msg, replacements);
            }
        }
    }
    private void startTask(long time) {
        final long pick = ((pickNextWinner-time)/1000)*20;
        if(countdownTasks != null) {
            for(int i : countdownTasks) {
                SCHEDULER.cancelTask(i);
            }
        }
        task = SCHEDULER.scheduleSyncDelayedTask(JACKPOT, this::pickWinner, pick);
        countdownTasks = new ArrayList<>();
        for(String s : getStringList(CONFIG, "messages.countdowns")) {
            final long delay = (getDelay(s)/1000)*20;
            countdownTasks.add(SCHEDULER.scheduleSyncDelayedTask(JACKPOT, () -> {
                broadcastCountdown((delay/20)*1000);
            }, pick-delay));
        }
    }
    public void confirmPurchaseTickets(@NotNull Player player, @NotNull BigDecimal tickets) {
        if(player.hasPermission("Jackpot.buy")) {
            player.closeInventory();
            purchasing.put(player.getUniqueId(), tickets);
            final String amount = formatBigDecimal(tickets), cost = formatBigDecimal(ticketCost.multiply(tickets));
            final int size = gui.getSize();
            player.openInventory(Bukkit.createInventory(player, size, gui.getTitle().replace("{AMOUNT}", amount)));
            final Inventory top = player.getOpenInventory().getTopInventory();
            top.setContents(gui.getInventory().getContents());
            for(int i = 0; i < size; i++) {
                item = top.getItem(i);
                if(item != null) {
                    itemMeta = item.getItemMeta(); lore.clear();
                    if(itemMeta.hasLore()) {
                        for(String l : itemMeta.getLore()) {
                            lore.add(l.replace("{AMOUNT}", amount).replace("{$}", cost));
                        }
                    }
                    itemMeta.setLore(lore); lore.clear();
                    item.setItemMeta(itemMeta);
                }
            }
            player.updateInventory();
        }
    }
    public void purchaseTickets(@NotNull Player player, @NotNull BigDecimal tickets) {
        final BigDecimal cost = tickets.multiply(ticketCost);
        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("{AMOUNT}", formatBigDecimal(tickets));
        replacements.put("{$}", formatBigDecimal(cost));
        if(eco.withdrawPlayer(player, cost.doubleValue()).transactionSuccess()) {
            final JackpotPurchaseTicketsEvent e = new JackpotPurchaseTicketsEvent(player, tickets, cost);
            PLUGIN_MANAGER.callEvent(e);
            if(!e.isCancelled()) {
                final UUID uuid = player.getUniqueId();
                final JPlayer pdata = JPlayer.get(uuid);
                pdata.totalTicketsBought = pdata.totalTicketsBought.add(tickets);
                ticketsSold.put(uuid, ticketsSold.getOrDefault(uuid, BigDecimal.ZERO).add(tickets));
                value = value.add(cost);
                sendStringListMessage(player, getStringList(CONFIG, "messages.purchased"), replacements);
            }
        } else {
            sendStringListMessage(player, getStringList(CONFIG, "messages.cannot afford"), replacements);
        }
    }

    public void view(@NotNull CommandSender sender) {
        if(sender.hasPermission("Jackpot.command")) {
            final String time = getRemainingTime(pickNextWinner-System.currentTimeMillis());
            final HashMap<String, String> replacements = new HashMap<>();
            final BigDecimal t = sender instanceof Player ? ticketsSold.getOrDefault(((Player) sender).getUniqueId(), BigDecimal.ZERO) : BigDecimal.ZERO;
            final int s = getTickets().size();
            replacements.put("{VALUE}", formatBigDecimal(value));
            replacements.put("{TICKETS}", formatDouble(getTickets().size()));
            replacements.put("{YOUR_TICKETS}", formatBigDecimal(t));
            replacements.put("{YOUR_TICKETS%}", formatDouble(getPercent(t, s)));
            replacements.put("{TIME}", time);
            sendStringListMessage(sender, getStringList(CONFIG, "messages.view"), replacements);
        }
    }
    public void viewStats(@NotNull Player player) {
        if(player.hasPermission("Jackpot.stats")) {
            final HashMap<String, String> replacements = new HashMap<>();
            final JPlayer pdata = JPlayer.get(player.getUniqueId());
            replacements.put("{$}", formatBigDecimal(pdata.totalWonCash));
            replacements.put("{TICKETS}", formatBigDecimal(pdata.totalTicketsBought));
            replacements.put("{WINS}", formatBigDecimal(pdata.totalWins));
            sendStringListMessage(player, getStringList(CONFIG, "messages.stats"), replacements);
        }
    }
    public void viewTop(@NotNull CommandSender sender, int page) {
        if(sender.hasPermission("Jackpot.top")) {
            final List<String> list = colorizeListString(getStringList(CONFIG, "messages.top"));
            final String p = Integer.toString(page);
            final int perPage = CONFIG.getInt("messages.players per page");
            for(String s : list) {
                if(s.contains("{PLACE}")) {
                    // TODO: fix dis
                } else {
                    s = s.replace("{PAGE}", p);
                    sender.sendMessage(s);
                }
            }
        }
    }
    public void viewHelp(@NotNull CommandSender sender) {
        if(sender.hasPermission("Jackpot.help")) {
            sendStringListMessage(sender, getStringList(CONFIG, "messages.help"), null);
        }
    }
    public void tryToggleNotifications(@NotNull Player player) {
        if(player.hasPermission("Jackpot.toggle")) {
            final JPlayer pdata = JPlayer.get(player.getUniqueId());
            final boolean status = !pdata.doesReceiveNotifications();
            pdata.setReceivesNotifications(status);
            sendStringListMessage(player, getStringList(CONFIG, "messages.toggle notifications." + (status ? "on" : "off")), null);
        }
    }

    @EventHandler
    private void inventoryCloseEvent(InventoryCloseEvent event) {
        purchasing.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void inventoryClickEvent(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final UUID uuid = player.getUniqueId();
        if(purchasing.containsKey(uuid)) {
            event.setCancelled(true);
            player.updateInventory();
            final int slot = event.getRawSlot();
            final ItemStack current = event.getCurrentItem();
            if(slot < 0 || slot >= player.getOpenInventory().getTopInventory().getSize() || current == null || current.getType().equals(Material.AIR)) {
                return;
            }

            if(confirmSlots.contains(slot)) {
                purchaseTickets(player, purchasing.get(uuid));
            } else if(cancelSlots.contains(slot)) {
            } else {
                return;
            }
            player.closeInventory();
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void playerJoinEvent(PlayerJoinEvent event) {
        JPlayer.get(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void playerQuitEvent(PlayerQuitEvent event) {
        JPlayer.get(event.getPlayer().getUniqueId()).unload();
    }
}
