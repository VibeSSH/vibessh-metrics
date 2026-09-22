# VibeSSH Metrics

Live TPS, MSPT, memory and players - in game with `/metrics`, and in your VibeSSH panel.

A Paper plugin built on the [royalmc-platform](../royalmc-platform). It writes a compact
`status.json` snapshot every few seconds; VibeSSH reads it over SSH and shows a live
**Minecraft** card. No port, no token - the plugin only writes a file.

## What it does

- **`/metrics`** - TPS / MSPT / RAM and per-world entity report in chat.
- **`/metrics tpsbar`** - toggle a boss bar with live TPS / MSPT / your ping.
- **`/metrics worst`** - what is eating the tick (stub for now, see roadmap).
- Writes `.vibessh/status.json` (relative to the server directory) every `write-interval-seconds`.
  The state is read on the main thread, the file is written async and renamed into place, so the
  panel never sees a half-written snapshot.

## Build

The platform is consumed from the local Maven cache, so publish it once first:

```bash
cd ../royalmc-platform
./gradlew publishToMavenLocal
```

Then build the plugin:

```bash
./gradlew build
```

The shaded jar lands in `build/libs/VibeSSHMetrics-0.1.0.jar`. Drop it in `plugins/`, restart.

## Layout

- `dev.vibessh.metrics.VibeSSHMetricsPlugin` - entry point; wires config, the writer and commands.
- `.../status/StatusWriter` - the `PlatformModule` that schedules and writes the snapshot.
- `.../status/ServerStatus` - the snapshot record; its field names are the JSON keys.
- `.../config/MetricsConfig` - the Okaeri config (`config.yml`).
- `.../command/MetricsCommand` - the `/metrics` command (LiteCommands).

## Config (`config.yml`)

| Key | Default | Meaning |
| --- | --- | --- |
| `write-interval-seconds` | `5` | How often the snapshot is written. |
| `status-file` | `.vibessh/status.json` | Where it is written. VibeSSH reads this exact path. |
| `include-player-names` | `true` | Include online player names in the snapshot. |

## To verify before shipping

This is a skeleton written against the platform's documented API. Run `./gradlew build` and
fix anything the compiler flags - the likely spots are the LiteCommands 3.11 annotation package
names, the Shadow/Gradle version pairing in `build.gradle.kts`, and the exact relocation packages
(Multification in particular). The panel-side **Minecraft** card is a separate change in the
VibeSSH desktop app.

## Roadmap

- `/metrics worst` - rank entity/chunk hotspots per world.
- Panel: TPS-drop alert, Vibe AI reading `status.json` for lag diagnosis.

Part of [VibeSSH](https://vibessh.dev).
