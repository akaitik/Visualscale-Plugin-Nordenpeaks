package com.example.visualscale;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona y persiste las escalas visuales por jugador.
 * No modifica ningún atributo del servidor; la escala es puramente cosmética.
 */
public class ScaleManager {

    private final VisualScalePlugin plugin;
    private final Map<UUID, Double> scales = new ConcurrentHashMap<>();
    private final File scalesFile;

    public ScaleManager(VisualScalePlugin plugin) {
        this.plugin    = plugin;
        this.scalesFile = new File(plugin.getDataFolder(), "scales.yml");
        loadScales();
    }

    // ── Lectura ────────────────────────────────────────────────────────────────

    public boolean hasCustomScale(Player player) {
        return scales.containsKey(player.getUniqueId());
    }

    public double getScale(Player player) {
        return scales.getOrDefault(player.getUniqueId(), 1.0);
    }

    public Map<UUID, Double> getAllScales() {
        return Collections.unmodifiableMap(scales);
    }

    // ── Escritura ──────────────────────────────────────────────────────────────

    /**
     * Establece la escala de un jugador y la almacena en memoria.
     * @return la escala efectivamente aplicada (clamped)
     */
    public double setScale(Player player, double rawScale) {
        double min = plugin.getConfig().getDouble("min-scale", 0.0625);
        double max = plugin.getConfig().getDouble("max-scale", 16.0);
        double clamped = Math.max(min, Math.min(max, rawScale));

        if (Math.abs(clamped - 1.0) < 1e-6) {
            scales.remove(player.getUniqueId());
        } else {
            scales.put(player.getUniqueId(), clamped);
        }
        return clamped;
    }

    public void resetScale(Player player) {
        scales.remove(player.getUniqueId());
    }

    // ── Persistencia ───────────────────────────────────────────────────────────

    public void loadScales() {
        if (!scalesFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(scalesFile);
        yaml.getKeys(false).forEach(key -> {
            try {
                UUID uuid = UUID.fromString(key);
                double val = yaml.getDouble(key, 1.0);
                if (Math.abs(val - 1.0) >= 1e-6) scales.put(uuid, val);
            } catch (IllegalArgumentException ignored) { }
        });
        plugin.getLogger().info("Escalas cargadas: " + scales.size() + " jugadores.");
    }

    public void saveScales() {
        YamlConfiguration yaml = new YamlConfiguration();
        scales.forEach((uuid, scale) -> yaml.set(uuid.toString(), scale));
        try {
            yaml.save(scalesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Error al guardar escalas: " + e.getMessage());
        }
    }
}
