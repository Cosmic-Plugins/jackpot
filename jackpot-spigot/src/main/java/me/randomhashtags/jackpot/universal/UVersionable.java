package me.randomhashtags.jackpot.universal;

import me.randomhashtags.jackpot.JackpotSpigot;
import me.randomhashtags.jackpot.util.Versionable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public interface UVersionable extends Versionable {
    JackpotSpigot JACKPOT = JackpotSpigot.getPlugin;
    FileConfiguration JACKPOT_CONFIG = JACKPOT.getConfig();
    PluginManager PLUGIN_MANAGER = Bukkit.getPluginManager();
    Random RANDOM = new Random();

    BukkitScheduler SCHEDULER = Bukkit.getScheduler();
    ConsoleCommandSender CONSOLE = Bukkit.getConsoleSender();

    HashMap<FileConfiguration, HashMap<String, List<String>>> FEATURE_MESSAGES = new HashMap<>();

    default List<String> getStringList(FileConfiguration yml, String identifier) {
        if(!FEATURE_MESSAGES.containsKey(yml)) {
            FEATURE_MESSAGES.put(yml, new HashMap<>());
        }
        final HashMap<String, List<String>> messages = FEATURE_MESSAGES.get(yml);
        if(!messages.containsKey(identifier)) {
            messages.put(identifier, colorizeListString(yml.getStringList(identifier)));
        }
        return messages.get(identifier);
    }

    default HashSet<String> getConfigurationSectionKeys(FileConfiguration yml, String key, boolean includeKeys, String...excluding) {
        final ConfigurationSection section = yml.getConfigurationSection(key);
        if(section != null) {
            final HashSet<String> set = new HashSet<>(section.getKeys(includeKeys));
            set.removeAll(Arrays.asList(excluding));
            return set;
        } else {
            return new HashSet<>();
        }
    }

    default String getRemainingTime(long time) {
        int sec = (int) TimeUnit.MILLISECONDS.toSeconds(time), min = sec/60, hr = min/60, d = hr/24;
        hr -= d*24;
        min -= (hr*60)+(d*60*24);
        sec -= (min*60)+(hr*60*60)+(d*60*60*24);
        final String dys = d > 0 ? d + "d" + (hr != 0 ? " " : "") : "";
        final String hrs = hr > 0 ? hr + "h" + (min != 0 ? " " : "") : "";
        final String mins = min != 0 ? min + "m" + (sec != 0 ? " " : "") : "";
        final String secs = sec != 0 ? sec + "s" : "";
        return dys + hrs + mins + secs;
    }
    default void sendConsoleMessage(String msg) {
        CONSOLE.sendMessage(colorize(msg));
    }
    default void sendConsoleDidLoadFeature(String what, long started) {
        sendConsoleMessage("&6[Jackpot] &aLoaded " + what + " &e(took " + (System.currentTimeMillis()-started) + "ms)");
    }
    default String formatBigDecimal(BigDecimal b) {
        return formatBigDecimal(b, false);
    }
    default String formatBigDecimal(BigDecimal b, boolean currency) {
        return (currency ? NumberFormat.getCurrencyInstance() : NumberFormat.getInstance()).format(b);
    }
    default String formatDouble(double d) {
        String decimals = Double.toString(d).split("\\.")[1];
        if(decimals.equals("0")) { decimals = ""; } else { decimals = "." + decimals; }
        return formatInt((int) d) + decimals;
    }
    default String formatInt(int integer) {
        return String.format("%,d", integer);
    }
    default int getRemainingInt(String string) {
        string = ChatColor.stripColor(colorize(string)).replaceAll("\\p{L}", "").replaceAll("\\s", "").replaceAll("\\p{P}", "").replaceAll("\\p{S}", "");
        return string.isEmpty() ? -1 : Integer.parseInt(string);
    }
    default Double getRemainingDouble(String string) {
        string = ChatColor.stripColor(colorize(string).replaceAll("\\p{L}", "").replaceAll("\\p{Z}", "").replaceAll("\\.", "d").replaceAll("\\p{P}", "").replaceAll("\\p{S}", "").replace("d", "."));
        return string.isEmpty() ? -1.00 : Double.parseDouble(string.contains(".") && string.split("\\.").length > 1 && string.split("\\.")[1].length() > 2 ? string.substring(0, string.split("\\.")[0].length() + 3) : string);
    }
    default long getDelay(String input) {
        input = input.toLowerCase();
        long l = 0;
        if(input.contains("d")) {
            final String[] s = input.split("d");
            l += getRemainingDouble(s[0])*1000*60*60*24;
            input = s.length > 1 ? s[1] : input;
        }
        if(input.contains("h")) {
            final String[] s = input.split("h");
            l += getRemainingDouble(s[0])*1000*60*60;
            input = s.length > 1 ? s[1] : input;
        }
        if(input.contains("m")) {
            final String[] s = input.split("m");
            l += getRemainingDouble(s[0])*1000*60;
            input = s.length > 1 ? s[1] : input;
        }
        if(input.contains("s")) {
            l += getRemainingDouble(input.split("s")[0])*1000;
        }
        return l;
    }

    default double round(double input, int decimals) {
        // From http://www.baeldung.com/java-round-decimal-number
        if(decimals < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(input));
        bd = bd.setScale(decimals, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    default List<String> colorizeListString(List<String> input) {
        final List<String> i = new ArrayList<>();
        if(input != null) {
            for(String s : input) {
                i.add(colorize(s));
            }
        }
        return i;
    }
    default String colorize(String input) {
        return input != null ? ChatColor.translateAlternateColorCodes('&', input) : "NULL";
    }
    default void sendStringListMessage(CommandSender sender, List<String> message, HashMap<String, String> replacements) {
        if(message != null && message.size() > 0 && !message.get(0).equals("")) {
            final boolean papi = JACKPOT.placeholderapi, isPlayer = sender instanceof Player;
            final Player player = isPlayer ? (Player) sender : null;
            for(String s : message) {
                if(replacements != null) {
                    for(String r : replacements.keySet()) {
                        final String replacement = replacements.get(r);
                        s = s.replace(r, replacement != null ? replacement : "null");
                    }
                }
                if(s != null) {
                    if(papi && isPlayer) {
                        s = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, s);
                    }
                    sender.sendMessage(colorize(s));
                }
            }
        }
    }
}
