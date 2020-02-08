package me.randomhashtags.jackpot;

import org.bukkit.plugin.java.JavaPlugin;

public final class JackpotSpigot extends JavaPlugin {

    public static JackpotSpigot getPlugin;

    @Override
    public void onEnable() {
        getPlugin = this;
        saveSettings();
        getCommand("jackpot").setExecutor(JackpotAPI.INSTANCE);
        enable();
    }

    @Override
    public void onDisable() {
        disable();
    }

    private void saveSettings() {
        saveDefaultConfig();
    }

    public void enable() {
        saveSettings();
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
