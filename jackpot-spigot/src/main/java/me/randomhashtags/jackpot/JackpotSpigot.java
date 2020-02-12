package me.randomhashtags.jackpot;

import org.bukkit.plugin.java.JavaPlugin;

public final class JackpotSpigot extends JavaPlugin {

    public static JackpotSpigot getPlugin;

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
        JackpotAPI.INSTANCE.load();
    }
    public void disable() {
        JackpotAPI.INSTANCE.unload();
    }

    public void reload() {
        disable();
        enable();
    }
}
