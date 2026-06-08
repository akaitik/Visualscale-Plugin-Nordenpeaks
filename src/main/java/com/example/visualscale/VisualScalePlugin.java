package com.example.visualscale;

import com.example.visualscale.commands.RaceCommand;
import com.example.visualscale.commands.ScaleCommand;
import com.example.visualscale.listeners.PlayerEventListener;
import com.example.visualscale.network.ScalePacketSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class VisualScalePlugin extends JavaPlugin {

    /** Canal de plugin usado para enviar la escala al mod cliente. */
    public static final String CHANNEL = "visualscale:scale";

    private ScaleManager scaleManager;
    private ScalePacketSender packetSender;
    private BukkitTask autoSaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        scaleManager  = new ScaleManager(this);
        packetSender  = new ScalePacketSender(this, scaleManager);

        // Registrar el canal de salida (servidor → cliente)
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        // Eventos de jugador
        getServer().getPluginManager().registerEvents(
                new PlayerEventListener(this, scaleManager, packetSender), this);

        // Comandos
        ScaleCommand scaleCmd = new ScaleCommand(this, scaleManager, packetSender);
        getCommand("scale").setExecutor(scaleCmd);
        getCommand("scale").setTabCompleter(scaleCmd);

        RaceCommand raceCmd = new RaceCommand(this, scaleManager, packetSender);
        getCommand("race").setExecutor(raceCmd);
        getCommand("race").setTabCompleter(raceCmd);

        // Auto-guardado
        int intervalMin = getConfig().getInt("auto-save-interval", 5);
        if (intervalMin > 0) {
            long ticks = intervalMin * 60L * 20L;
            autoSaveTask = getServer().getScheduler()
                    .runTaskTimerAsynchronously(this, scaleManager::saveScales, ticks, ticks);
        }

        getLogger().info("VisualScalePlugin habilitado (canal: " + CHANNEL + ").");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        scaleManager.saveScales();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getLogger().info("VisualScalePlugin deshabilitado. Datos guardados.");
    }

    public ScaleManager getScaleManager()   { return scaleManager; }
    public ScalePacketSender getPacketSender() { return packetSender; }
}
