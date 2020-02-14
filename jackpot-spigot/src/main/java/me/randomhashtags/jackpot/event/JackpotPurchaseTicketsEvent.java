package me.randomhashtags.jackpot.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

public class JackpotPurchaseTicketsEvent extends Event implements Cancellable {
    private static HandlerList HANDLERS = new HandlerList();
    private Player player;
    private BigDecimal tickets, cost;
    private boolean cancelled;
    public JackpotPurchaseTicketsEvent(Player player, BigDecimal tickets, BigDecimal cost) {
        this.player = player;
        this.tickets = tickets;
        this.cost = cost;
    }

    public Player getPlayer() {
        return player;
    }
    public BigDecimal getTickets() {
        return tickets;
    }
    public BigDecimal getCost() {
        return cost;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
