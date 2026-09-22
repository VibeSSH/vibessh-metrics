package dev.vibessh.metrics;

import dev.rollczi.litecommands.LiteCommands;
import dev.vibessh.metrics.bar.TpsBar;
import dev.vibessh.metrics.command.MetricsCommand;
import dev.vibessh.metrics.config.MessagesConfig;
import dev.vibessh.metrics.config.MetricsConfig;
import dev.vibessh.metrics.status.StatusWriter;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl.royalmc.platform.api.ModuleLifecycle;
import pl.royalmc.platform.component.ComponentFormatter;
import pl.royalmc.platform.config.ConfigService;
import pl.royalmc.platform.notice.AudienceNoticeService;
import pl.royalmc.platform.notice.NoticeServiceProvider;
import pl.royalmc.platform.paper.command.PaperCommands;

/**
 * VibeSSH Metrics entry point.
 *
 * <p>The platform gives no base plugin class on purpose, so the wiring is assembled here:
 * load config through {@link ConfigService}, start the {@link StatusWriter} through a
 * {@link ModuleLifecycle} (so it stops cleanly with everything else), and register commands
 * through {@link PaperCommands}, which comes pre-wired with MiniMessage and the platform's
 * technical replies.
 */
public final class VibeSSHMetricsPlugin extends JavaPlugin {

    private ModuleLifecycle lifecycle;
    private LiteCommands<CommandSender> commands;

    @Override
    public void onEnable() {
        ComponentFormatter formatter = ComponentFormatter.standard();

        ConfigService configs = new ConfigService();
        MetricsConfig config = configs.load(getDataPath(), "config.yml", MetricsConfig.class);
        MessagesConfig messages = configs.load(getDataPath(), "messages.yml", MessagesConfig.class);
        NoticeServiceProvider<CommandSender> notices = new AudienceNoticeService<>(messages, formatter);

        StatusWriter statusWriter = new StatusWriter(this, config, getSLF4JLogger());
        TpsBar tpsBar = new TpsBar(this, config, formatter);
        this.lifecycle = new ModuleLifecycle(getSLF4JLogger());
        this.lifecycle.register(statusWriter);
        this.lifecycle.register(tpsBar);
        this.lifecycle.enableAll();

        this.commands = PaperCommands.builder(this, formatter, notices)
                .commands(new MetricsCommand(statusWriter, tpsBar))
                .build();
    }

    @Override
    public void onDisable() {
        if (this.commands != null) {
            this.commands.unregister();
        }
        if (this.lifecycle != null) {
            this.lifecycle.disableAll();
        }
    }
}
