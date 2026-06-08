package com.example.visualscale.network;

import com.example.visualscale.ScaleManager;
import com.example.visualscale.VisualScalePlugin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Envía la escala visual al mod cliente a través del canal de plugin
 * "visualscale:scale".
 *
 * <p>Protocolo del paquete (big-endian):
 * <pre>
 *   long   uuid_most          — UUID.getMostSignificantBits()
 *   long   uuid_least         — UUID.getLeastSignificantBits()
 *   double scale              — escala (1.0 = tamaño normal)
 * </pre>
 *
 * El receptor (viewer) es quien recibe el canal; target es el jugador cuya
 * escala se informa. Si target == viewer, el cliente actualiza su propio
 * modelo.
 */
public class ScalePacketSender {

    private final VisualScalePlugin plugin;
    private final ScaleManager scaleManager;

    public ScalePacketSender(VisualScalePlugin plugin, ScaleManager scaleManager) {
        this.plugin       = plugin;
        this.scaleManager = scaleManager;
    }

    // ── API pública ────────────────────────────────────────────────────────────

    /**
     * Envía la escala de {@code target} a todos los viewers de la lista.
     */
    public void broadcast(Player target, double scale, Collection<? extends Player> viewers) {
        byte[] payload = buildPayload(target.getUniqueId(), scale);
        for (Player viewer : viewers) {
            if (viewer.isOnline()) {
                viewer.sendPluginMessage(plugin, VisualScalePlugin.CHANNEL, payload);
            }
        }
    }

    /**
     * Envía la escala de {@code target} a UN viewer específico.
     */
    public void sendTo(Player viewer, Player target, double scale) {
        if (!viewer.isOnline()) return;
        byte[] payload = buildPayload(target.getUniqueId(), scale);
        viewer.sendPluginMessage(plugin, VisualScalePlugin.CHANNEL, payload);
    }

    /**
     * Cuando un jugador entra al mundo, le enviamos las escalas de todos los
     * jugadores cercanos, y a los cercanos le enviamos la suya propia.
     */
    public void sendInitialScales(Player joiningPlayer) {
        double joiningScale = scaleManager.getScale(joiningPlayer);
        double viewerRange  = plugin.getConfig().getDouble("viewer-range", 64.0);
        double rangeSquared = viewerRange * viewerRange;

        for (Player other : joiningPlayer.getWorld().getPlayers()) {
            if (other.equals(joiningPlayer)) continue;
            double dist = other.getLocation().distanceSquared(joiningPlayer.getLocation());
            if (dist > rangeSquared) continue;

            // Decirle al jugador que entra la escala del otro
            if (scaleManager.hasCustomScale(other)) {
                sendTo(joiningPlayer, other, scaleManager.getScale(other));
            }
            // Decirle al otro la escala del jugador que entra
            if (scaleManager.hasCustomScale(joiningPlayer)) {
                sendTo(other, joiningPlayer, joiningScale);
            }
        }

        // Enviarle su propia escala (para que su cámara en 3ª persona sea correcta)
        if (scaleManager.hasCustomScale(joiningPlayer)) {
            sendTo(joiningPlayer, joiningPlayer, joiningScale);
        }
    }

    /**
     * Reaplica la escala actual de {@code target} a sí mismo y a todos los
     * jugadores en rango. Útil tras un respawn o cambio de mundo.
     */
    public void reapply(Player target) {
        double scale = scaleManager.getScale(target);
        double viewerRange  = plugin.getConfig().getDouble("viewer-range", 64.0);
        double rangeSquared = viewerRange * viewerRange;

        // Siempre enviarse a sí mismo
        sendTo(target, target, scale);

        for (Player other : target.getWorld().getPlayers()) {
            if (other.equals(target)) continue;
            double dist = other.getLocation().distanceSquared(target.getLocation());
            if (dist <= rangeSquared) {
                sendTo(other, target, scale);
            }
        }
    }

    /**
     * Envía escala 1.0 a todos para "borrar" la escala visual de {@code target}.
     */
    public void reset(Player target, Collection<? extends Player> viewers) {
        broadcast(target, 1.0, viewers);
        sendTo(target, target, 1.0);
    }

    // ── Interno ────────────────────────────────────────────────────────────────

    @SuppressWarnings("UnstableApiUsage")
    private byte[] buildPayload(UUID uuid, double scale) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeDouble(scale);
        return out.toByteArray();
    }
}
