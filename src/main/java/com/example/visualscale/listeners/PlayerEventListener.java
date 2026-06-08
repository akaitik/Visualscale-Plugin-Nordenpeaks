package com.example.visualscale.listeners;

import com.example.visualscale.ScaleManager;
import com.example.visualscale.VisualScalePlugin;
import com.example.visualscale.network.ScalePacketSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerEventListener implements Listener {

    private final VisualScalePlugin plugin;
    private final ScaleManager scaleManager;
    private final ScalePacketSender sender;

    public PlayerEventListener(VisualScalePlugin plugin,
                               ScaleManager scaleManager,
                               ScalePacketSender sender) {
        this.plugin       = plugin;
        this.scaleManager = scaleManager;
        this.sender       = sender;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delay de 5 ticks para que el cliente termine de cargar el canal
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sender.sendInitialScales(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sender.reapply(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                sender.reapply(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Guardado asíncrono de las escalas al salir
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                scaleManager::saveScales);
    }
}
