package dev.vibessh.serverpulse.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;

/**
 * ServerPulse's configuration.
 *
 * <p>Every field carries an explicit {@link CustomKey}: Okaeri's class-wide naming strategy
 * is deprecated, so the kebab-case keys are spelled out here to keep the written YAML stable.
 */
@Header("ServerPulse - writes a compact status snapshot the VibeSSH panel reads over SSH.")
public class ServerPulseConfig extends OkaeriConfig {

    @Comment("How often the snapshot is written, in seconds.")
    @CustomKey("write-interval-seconds")
    public int writeIntervalSeconds = 5;

    @Comment({
            "Where the snapshot is written, relative to the server directory.",
            "VibeSSH reads this exact path, so change it only if you also change it there."
    })
    @CustomKey("status-file")
    public String statusFile = ".vibessh/status.json";

    @Comment("Include the list of online player names in the snapshot.")
    @CustomKey("include-player-names")
    public boolean includePlayerNames = true;

    @Comment("How often the /pulse tpsbar boss bar refreshes, in ticks (20 = once a second).")
    @CustomKey("bossbar-update-ticks")
    public int bossbarUpdateTicks = 20;
}
