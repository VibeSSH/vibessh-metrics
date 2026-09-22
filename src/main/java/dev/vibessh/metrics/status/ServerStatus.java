package dev.vibessh.metrics.status;

import java.util.List;

/**
 * The status snapshot VibeSSH Metrics writes to disk and the VibeSSH panel reads over SSH.
 *
 * <p>The record component names are the JSON keys, so renaming one is a breaking change:
 * bump {@link #SCHEMA} and update the panel's reader when that happens. {@code players.names}
 * is {@code null} when player names are switched off in the config.
 */
public record ServerStatus(
        int schema,
        String updatedAt,
        Server server,
        Tps tps,
        double msptAvg,
        Players players,
        Memory memory,
        List<World> worlds) {

    /** The schema the panel checks before trusting the rest of the file. */
    public static final int SCHEMA = 1;

    public record Server(String version, String software, long uptimeSeconds) {
    }

    public record Tps(double m1, double m5, double m15) {
    }

    public record Players(int online, int max, List<String> names) {
    }

    public record Memory(long usedMb, long maxMb) {
    }

    public record World(String name, int players, int entities, int chunks) {
    }
}
