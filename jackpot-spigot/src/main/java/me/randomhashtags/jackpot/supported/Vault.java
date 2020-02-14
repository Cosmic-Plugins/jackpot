package me.randomhashtags.jackpot.supported;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Vault {
    private static Vault instance;
    public static Vault getVault() {
        if(instance == null) instance = new Vault();
        return instance;
    }
    private boolean didSetupEco = false;
    private Economy economy = null;
    public boolean setupEconomy() {
        didSetupEco = true;
        final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if(economyProvider != null) economy = economyProvider.getProvider();
        return economy != null;
    }
    public Economy getEconomy() {
        if(!didSetupEco) {
            setupEconomy();
        }
        return economy;
    }
}
