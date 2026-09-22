package dev.vibessh.metrics.status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.vibessh.metrics.config.MetricsConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import pl.royalmc.platform.api.PlatformException;
import pl.royalmc.platform.api.PlatformModule;

/**
 * Writes the server's live status to a JSON file on a fixed interval.
 *
 * <p>The read of the server state happens on the global-region (main) thread, where the
 * Bukkit API is safe to touch; the file write is then handed to the async scheduler so I/O
 * never sits on a tick. The write goes to a temp file and is renamed into place, so the panel
 * never reads a half-written snapshot. A {@link PlatformModule}, so the plugin's
 * {@code ModuleLifecycle} starts and stops it with everything else.
 */
public final class StatusWriter implements PlatformModule {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final Plugin plugin;
    private final MetricsConfig config;
    private final Logger logger;
    private final Path statusFile;
    private final Path tempFile;

    private ScheduledTask task;

    public StatusWriter(Plugin plugin, MetricsConfig config, Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        // Relative to the server's working directory, which is where the panel resolves
        // an application's paths from.
        this.statusFile = Path.of(config.statusFile).toAbsolutePath();
        this.tempFile = this.statusFile.resolveSibling(this.statusFile.getFileName() + ".tmp");
    }

    @Override
    public void enable() {
        long periodTicks = Math.max(1L, this.config.writeIntervalSeconds) * 20L;
        this.task = this.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                this.plugin,
                scheduled -> {
                    ServerStatus status = snapshot();
                    this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, ignored -> write(status));
                },
                periodTicks,
                periodTicks);
        if (this.task == null) {
            throw new PlatformException("VibeSSH Metrics could not schedule the status task");
        }
    }

    @Override
    public void disable() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * Reads the current server state into an immutable snapshot.
     *
     * <p>Must be called on the main / global-region thread - it touches the Bukkit API.
     */
    public ServerStatus snapshot() {
        Server server = this.plugin.getServer();
        double[] tps = server.getTPS();
        Runtime runtime = Runtime.getRuntime();

        List<ServerStatus.World> worlds = new ArrayList<>();
        for (World world : server.getWorlds()) {
            worlds.add(new ServerStatus.World(
                    world.getName(),
                    world.getPlayers().size(),
                    world.getEntities().size(),
                    world.getLoadedChunks().length));
        }

        List<String> names = null;
        if (this.config.includePlayerNames) {
            names = new ArrayList<>();
            for (Player player : server.getOnlinePlayers()) {
                names.add(player.getName());
            }
        }

        return new ServerStatus(
                ServerStatus.SCHEMA,
                ISO.format(Instant.now()),
                new ServerStatus.Server(server.getMinecraftVersion(), server.getName(), uptimeSeconds()),
                new ServerStatus.Tps(round(tps[0]), round(tps[1]), round(tps[2])),
                round(server.getAverageTickTime()),
                new ServerStatus.Players(server.getOnlinePlayers().size(), server.getMaxPlayers(), names),
                new ServerStatus.Memory(
                        (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB,
                        runtime.maxMemory() / BYTES_PER_MB),
                worlds);
    }

    private void write(ServerStatus status) {
        try {
            Path directory = this.statusFile.getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Files.writeString(this.tempFile, GSON.toJson(status), StandardCharsets.UTF_8);
            Files.move(this.tempFile, this.statusFile, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException exception) {
            this.logger.warn("VibeSSH Metrics could not write {}: {}", this.statusFile, exception.getMessage());
        }
    }

    private static long uptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
