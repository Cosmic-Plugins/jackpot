package me.randomhashtags.jackpot;

import me.randomhashtags.jackpot.universal.UVersionable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

public class JPlayer implements UVersionable {
    private static final String PLAYER_DATA_FOLDER = JACKPOT.getDataFolder() + File.separator + "playerData";
    public static final HashMap<UUID, JPlayer> CACHED_PLAYERS = new HashMap<>();

    private boolean isLoaded;
    private UUID uuid;
    private File file;
    private YamlConfiguration yml;
    private boolean notifications = true;
    public BigDecimal totalWonCash = BigDecimal.ZERO, totalTicketsBought = BigDecimal.ZERO, totalWins = BigDecimal.ZERO;

    public JPlayer(UUID uuid) {
        this.uuid = uuid;
        final File f = new File(PLAYER_DATA_FOLDER, uuid.toString() + ".yml");
        boolean backup = false;
        if(!CACHED_PLAYERS.containsKey(uuid)) {
            if(!f.exists()) {
                try {
                    final File folder = new File(JPlayer.PLAYER_DATA_FOLDER);
                    if(!folder.exists()) {
                        folder.mkdirs();
                    }
                    f.createNewFile();
                    backup = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file = new File(PLAYER_DATA_FOLDER, uuid.toString() + ".yml");
            yml = YamlConfiguration.loadConfiguration(file);
            CACHED_PLAYERS.put(uuid, this);
        }
        if(backup) {
            backup();
        }
    }

    public static JPlayer get(UUID player) {
        return CACHED_PLAYERS.getOrDefault(player, new JPlayer(player).load());
    }

    public JPlayer load() {
        if(!isLoaded) {
            isLoaded = true;
            notifications = yml.getBoolean("notifications");
            totalTicketsBought = BigDecimal.valueOf(yml.getInt("total tickets bought"));
            totalWins = BigDecimal.valueOf(yml.getInt("total wins"));
            totalWonCash = BigDecimal.valueOf(yml.getInt("total won cash"));
        }
        return this;
    }
    public void unload() {
        if(isLoaded) {
            try {
                backup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isLoaded = false;
            CACHED_PLAYERS.remove(uuid);
        }
    }
    public void backup() {
        yml.set("notifications", notifications);
        yml.set("total tickets bought", totalTicketsBought.intValue());
        yml.set("total wins", totalWins.intValue());
        yml.set("total won cash", totalWonCash.intValue());

        save();
    }
    private void save() {
        try {
            yml.save(file);
            yml = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UUID getUUID() {
        return uuid;
    }
    public OfflinePlayer getOfflinePlayer() {
        return uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
    }

    public boolean doesReceiveNotifications() {
        return notifications;
    }
    public void setReceivesNotifications(boolean jackpotNotifications) {
        this.notifications = jackpotNotifications;
    }
}
