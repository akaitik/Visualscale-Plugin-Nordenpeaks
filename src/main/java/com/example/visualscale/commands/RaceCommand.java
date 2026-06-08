package com.example.visualscale.commands;

import com.example.visualscale.ScaleManager;
import com.example.visualscale.VisualScalePlugin;
import com.example.visualscale.network.ScalePacketSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RaceCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "Race" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    private final VisualScalePlugin plugin;
    private final ScaleManager scaleManager;
    private final ScalePacketSender sender;

    /** UUID → escala original antes de la carrera */
    private final Map<UUID, Double> savedScales = new HashMap<>();
    private boolean raceActive = false;

    public RaceCommand(VisualScalePlugin plugin, ScaleManager scaleManager, ScalePacketSender sender) {
        this.plugin       = plugin;
        this.scaleManager = scaleManager;
        this.sender       = sender;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("visualscale.race")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Sin permiso.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(PREFIX + "Uso: /race <start|stop|status|assign <jugador> <escala>>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start"  -> startRace(sender);
            case "stop"   -> stopRace(sender);
            case "status" -> showStatus(sender);
            case "assign" -> {
                if (args.length < 3) { sender.sendMessage(PREFIX + "Uso: /race assign <jugador> <escala>"); return true; }
                assignScale(sender, args[1], args[2]);
            }
            default -> sender.sendMessage(PREFIX + "Sub-comando desconocido: " + args[0]);
        }
        return true;
    }

    private void startRace(CommandSender sender) {
        if (raceActive) { sender.sendMessage(PREFIX + "Ya hay una carrera activa. Usa /race stop primero."); return; }
        raceActive = true;
        savedScales.clear();

        List<Double> raceScales = plugin.getConfig().getDoubleList("race-scales");
        if (raceScales.isEmpty()) raceScales = List.of(0.5, 0.75, 1.0, 1.25, 1.5, 2.0);
        List<Double> pool = new ArrayList<>(raceScales);
        Collections.shuffle(pool);

        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        int i = 0;
        for (Player p : online) {
            savedScales.put(p.getUniqueId(), scaleManager.getScale(p));
            double newScale = pool.get(i % pool.size());
            scaleManager.setScale(p, newScale);
            this.sender.reapply(p);
            p.sendMessage(PREFIX + "¡Carrera iniciada! Tu escala: " + ChatColor.YELLOW + newScale);
            i++;
        }
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Carrera iniciada con " + online.size() + " jugadores.");
    }

    private void stopRace(CommandSender sender) {
        if (!raceActive) { sender.sendMessage(PREFIX + "No hay ninguna carrera activa."); return; }
        raceActive = false;

        for (Player p : Bukkit.getOnlinePlayers()) {
            double original = savedScales.getOrDefault(p.getUniqueId(), 1.0);
            scaleManager.setScale(p, original);
            this.sender.reapply(p);
            p.sendMessage(PREFIX + "Carrera finalizada. Escala restaurada a " + ChatColor.YELLOW + original);
        }
        savedScales.clear();
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Carrera detenida. Escalas restauradas.");
    }

    private void showStatus(CommandSender sender) {
        if (!raceActive) { sender.sendMessage(PREFIX + "No hay ninguna carrera activa."); return; }
        sender.sendMessage(PREFIX + "Escalas de la carrera:");
        for (Player p : Bukkit.getOnlinePlayers()) {
            sender.sendMessage(ChatColor.GRAY + "  " + p.getName() + ChatColor.WHITE + " → " + ChatColor.YELLOW + scaleManager.getScale(p));
        }
    }

    private void assignScale(CommandSender sender, String playerName, String scaleStr) {
        if (!raceActive) { sender.sendMessage(PREFIX + "La carrera no está activa."); return; }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Jugador no encontrado."); return; }
        try {
            double value   = Double.parseDouble(scaleStr);
            double applied = scaleManager.setScale(target, value);
            this.sender.reapply(target);
            sender.sendMessage(PREFIX + "Escala de " + target.getName() + " → " + ChatColor.YELLOW + applied);
            target.sendMessage(PREFIX + "Tu escala de carrera fue ajustada a " + ChatColor.YELLOW + applied);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Valor inválido: " + scaleStr);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("start", "stop", "status", "assign");
        if (args.length == 2 && args[0].equalsIgnoreCase("assign"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("assign"))
            return List.of("0.5", "0.75", "1.0", "1.25", "1.5", "2.0");
        return List.of();
    }
}
