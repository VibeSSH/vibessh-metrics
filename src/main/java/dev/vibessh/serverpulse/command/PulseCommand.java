package dev.vibessh.serverpulse.command;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import dev.vibessh.serverpulse.bar.TpsBar;
import dev.vibessh.serverpulse.status.ServerStatus;
import dev.vibessh.serverpulse.status.StatusWriter;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.entity.Player;

/**
 * {@code /pulse} - a live TPS / MSPT / RAM / per-world report in chat.
 *
 * <p>Each method returns a MiniMessage string, which the platform's Adventure extension
 * parses and sends to whoever ran the command. The same {@link StatusWriter} that feeds the
 * panel produces the snapshot, so chat and panel can never disagree.
 */
@Command(name = "pulse")
@Permission("serverpulse.use")
public final class PulseCommand {

    private final StatusWriter statusWriter;
    private final TpsBar tpsBar;

    public PulseCommand(StatusWriter statusWriter, TpsBar tpsBar) {
        this.statusWriter = Objects.requireNonNull(statusWriter, "statusWriter");
        this.tpsBar = Objects.requireNonNull(tpsBar, "tpsBar");
    }

    @Execute
    public String report() {
        ServerStatus status = this.statusWriter.snapshot();

        StringBuilder worlds = new StringBuilder();
        for (ServerStatus.World world : status.worlds()) {
            worlds.append("\n<gray>  - ").append(world.name()).append(":</gray> <white>")
                    .append(world.players()).append("</white><gray>p, </gray><white>")
                    .append(world.entities()).append("</white><gray> entity, </gray><white>")
                    .append(world.chunks()).append("</white><gray> chunk</gray>");
        }

        return "<gradient:#57c7d8:#8be9fd><b>ServerPulse</b></gradient> <dark_gray>|</dark_gray> <gray>status</gray>"
                + "\n<white>TPS:</white> " + tpsColored(status.tps().m1())
                + " <gray>(5m " + format(status.tps().m5()) + ", 15m " + format(status.tps().m15()) + ")</gray>"
                + "\n<white>MSPT:</white> <yellow>" + format(status.msptAvg()) + " ms</yellow>"
                + "\n<white>Players:</white> <aqua>" + status.players().online() + "</aqua><gray>/" + status.players().max() + "</gray>"
                + "\n<white>RAM:</white> <aqua>" + status.memory().usedMb() + "</aqua><gray>/" + status.memory().maxMb() + " MB</gray>"
                + "\n<white>Uptime:</white> <gray>" + (status.server().uptimeSeconds() / 60) + " min</gray>"
                + worlds;
    }

    @Execute(name = "tpsbar")
    public String toggleBar(@Context Player player) {
        boolean shown = this.tpsBar.toggle(player);
        return shown
                ? "<green>ServerPulse:</green> <gray>TPS bar on.</gray>"
                : "<gray>ServerPulse: TPS bar off.</gray>";
    }

    @Execute(name = "worst")
    public String worst() {
        // Roadmap: rank what is eating the tick (entity / chunk hotspots per world).
        return "<gray>ServerPulse:</gray> <white>/pulse worst</white> <gray>- coming soon.</gray>";
    }

    private static String tpsColored(double tps) {
        String color = tps >= 19.0 ? "green" : (tps >= 15.0 ? "yellow" : "red");
        return "<" + color + ">" + format(tps) + "</" + color + ">";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
