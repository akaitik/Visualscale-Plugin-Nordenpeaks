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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScaleCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "Scale" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    private final VisualScalePlugin plugin;
    private final ScaleManager scaleManager;
    private final ScalePacketSender sender;

    public ScaleCommand(VisualScalePlugin plugin, ScaleManager scaleManager, ScalePacketSender sender) {
        this.plugin       = plugin;
        this.scaleManager = scaleManager;
        this.sender       = sender;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 2) {
                handleAdmin(sender, args);
            } else {
                sender.sendMessage(PREFIX + "Uso desde consola: /scale <jugador> <valor|reset>");
            }
            return true;
        }

        // /scale  — ver escala propia
        if (args.length == 0) {
            double current = scaleManager.getScale(player);
            player.sendMessage(PREFIX + "Tu escala actual: " + ChatColor.YELLOW + current);
            return true;
        }

        // /scale list
        if (args[0].equalsIgnoreCase("list") && player.hasPermission("visualscale.admin")) {
            Map<UUID, Double> all = scaleManager.getAllScales();
            if (all.isEmpty()) {
                player.sendMessage(PREFIX + "No hay escalas personalizadas activas.");
            } else {
                player.sendMessage(PREFIX + "Escalas activas (" + all.size() + "):");
                all.forEach((uuid, scale) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : uuid.toString().substring(0, 8) + "…";
                    player.sendMessage(ChatColor.GRAY + "  " + name + ChatColor.WHITE + " → " + ChatColor.YELLOW + scale);
                });
            }
            return true;
        }

        // /scale reset
        if (args[0].equalsIgnoreCase("reset")) {
            applyScale(player, player, 1.0);
            player.sendMessage(PREFIX + "Tu escala ha sido restaurada a " + ChatColor.YELLOW + "1.0");
            return true;
        }

        // /scale <jugador> <valor|reset>  — admin
        if (args.length >= 2 && player.hasPermission("visualscale.admin")) {
            handleAdmin(player, args);
            return true;
        }

        // /scale <valor>
        try {
            double value = Double.parseDouble(args[0]);
            double applied = applyScale(player, player, value);
            player.sendMessage(PREFIX + "Escala aplicada: " + ChatColor.YELLOW + applied);
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Valor inválido: " + args[0]);
        }
        return true;
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Jugador no encontrado: " + args[0]);
            return;
        }
        if (args[1].equalsIgnoreCase("reset")) {
            applyScale(sender instanceof Player p ? p : null, target, 1.0);
            sender.sendMessage(PREFIX + "Escala de " + target.getName() + " restaurada a 1.0.");
            target.sendMessage(PREFIX + "Tu escala fue restaurada a " + ChatColor.YELLOW + "1.0" + ChatColor.RESET + " por un administrador.");
        } else {
            try {
                double value   = Double.parseDouble(args[1]);
                double applied = applyScale(null, target, value);
                sender.sendMessage(PREFIX + "Escala de " + target.getName() + " → " + ChatColor.YELLOW + applied);
                target.sendMessage(PREFIX + "Tu escala fue cambiada a " + ChatColor.YELLOW + applied + ChatColor.RESET + " por un administrador.");
            } catch (NumberFormatException e) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Valor inválido: " + args[1]);
            }
        }
    }

    /** Persiste y envía la escala. Retorna el valor efectivamente aplicado. */
    private double applyScale(Player actor, Player target, double raw) {
        double applied = scaleManager.setScale(target, raw);
        // Broadcast al target y a todos en rango
        sender.reapply(target);
        return applied;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reset");
            completions.add("list");
            completions.addAll(List.of("0.5", "0.75", "1.0", "1.25", "1.5", "2.0"));
            if (sender.hasPermission("visualscale.admin")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 2 && sender.hasPermission("visualscale.admin")) {
            completions.addAll(List.of("reset", "0.5", "0.75", "1.0", "1.25", "1.5", "2.0"));
        }
        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}
