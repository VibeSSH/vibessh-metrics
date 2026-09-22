package dev.vibessh.metrics.bar;

import dev.vibessh.metrics.config.MetricsConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import pl.royalmc.platform.api.PlatformException;
import pl.royalmc.platform.api.PlatformModule;
import pl.royalmc.platform.component.ComponentFormatter;

/**
 * A toggleable boss bar showing live TPS, MSPT and the viewer's own ping.
 *
 * <p>One {@link BossBar} per opted-in player (ping is per-player, so the bars cannot be
 * shared). A single repeating task refreshes them all on the main thread, where TPS and ping
 * are safe to read; the bar's fill and colour track TPS. Bars are dropped when their player
 * leaves, so the map never holds a ghost.
 */
public final class TpsBar implements PlatformModule, Listener {

    private final Plugin plugin;
    private final MetricsConfig config;
    private final ComponentFormatter formatter;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    private ScheduledTask task;

    public TpsBar(Plugin plugin, MetricsConfig config, ComponentFormatter formatter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public void enable() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        long period = Math.max(1L, this.config.bossbarUpdateTicks);
        this.task = this.plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(this.plugin, scheduled -> update(), period, period);
        if (this.task == null) {
            throw new PlatformException("VibeSSH Metrics could not schedule the boss bar task");
        }
    }

    @Override
    public void disable() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, BossBar> entry : this.bars.entrySet()) {
            Player player = this.plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        this.bars.clear();
    }

    /**
     * Turns the bar on or off for one player.
     *
     * @return the new state - {@code true} when it is now shown
     */
    public boolean toggle(Player player) {
        Objects.requireNonNull(player, "player");
        BossBar existing = this.bars.remove(player.getUniqueId());
        if (existing != null) {
            player.hideBossBar(existing);
            return false;
        }
        BossBar bar = BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
        this.bars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
        updateOne(player, bar);
        return true;
    }

    private void update() {
        if (this.bars.isEmpty()) {
            return;
        }
        Server server = this.plugin.getServer();
        Iterator<Map.Entry<UUID, BossBar>> iterator = this.bars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossBar> entry = iterator.next();
            Player player = server.getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            updateOne(player, entry.getValue());
        }
    }

    private void updateOne(Player player, BossBar bar) {
        Server server = this.plugin.getServer();
        double tps = Math.min(20.0, server.getTPS()[0]);
        double mspt = server.getAverageTickTime();
        int ping = player.getPing();

        bar.name(this.formatter.format(
                "<white>TPS:</white> " + tpsColored(tps)
                        + " <white>MSPT:</white> " + msptColored(mspt)
                        + " <white>Ping:</white> " + pingColored(ping)));
        bar.progress((float) Math.max(0.0, Math.min(1.0, tps / 20.0)));
        bar.color(tps >= 19.0 ? BossBar.Color.GREEN : (tps >= 15.0 ? BossBar.Color.YELLOW : BossBar.Color.RED));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.bars.remove(event.getPlayer().getUniqueId());
    }

    private static String tpsColored(double tps) {
        String color = tps >= 19.0 ? "green" : (tps >= 15.0 ? "yellow" : "red");
        return "<" + color + ">" + format(tps) + "</" + color + ">";
    }

    private static String msptColored(double mspt) {
        String color = mspt < 30.0 ? "green" : (mspt < 45.0 ? "yellow" : "red");
        return "<" + color + ">" + format(mspt) + " ms</" + color + ">";
    }

    private static String pingColored(int ping) {
        String color = ping < 100 ? "green" : (ping < 200 ? "yellow" : "red");
        return "<" + color + ">" + ping + "ms</" + color + ">";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
