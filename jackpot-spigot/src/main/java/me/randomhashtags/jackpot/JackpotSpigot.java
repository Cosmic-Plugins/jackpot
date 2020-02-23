package me.randomhashtags.jackpot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class JackpotSpigot extends JavaPlugin {

    public static JackpotSpigot getPlugin;
    public boolean placeholderapi;

    @Override
    public void onEnable() {
        getPlugin = this;
        getCommand("jackpot").setExecutor(JackpotAPI.INSTANCE);
        enable();
    }

    @Override
    public void onDisable() {
        disable();
    }

    public void enable() {
        saveDefaultConfig();
        placeholderapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        JackpotAPI.INSTANCE.load();
    }
    public void disable() {
        placeholderapi = false;
        JackpotAPI.INSTANCE.unload();
    }

    public void reload() {
        disable();
        enable();
    }
}
