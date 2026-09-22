package dev.vibessh.metrics.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import pl.royalmc.platform.notice.PlatformMessages;
import pl.royalmc.platform.notice.PlatformNotice;

/**
 * The platform's technical messages, in English, as this plugin's {@code messages.yml}.
 *
 * <p>The platform ships {@code PlatformMessagesConfig} with Polish defaults, because RoyalMC is
 * a Polish network. VibeSSH's plugins are published in English, so this is a standalone English
 * copy - not a subclass, because Okaeri 5.0.13 writes only the fields declared on the config
 * class itself and would drop every inherited message from the file. It implements
 * {@link PlatformMessages}, so the notice service takes it in place of the platform's default.
 *
 * <p>The colour scheme lives in {@code prefix}: it carries the label and the colour the body is
 * rendered in, and every message below is plain text, so restyling all technical messages is a
 * single edit. Placeholders are MiniMessage tags ({@code <usage>}); an empty value silences a
 * message rather than leaving a bare prefix.
 */
public class MessagesConfig extends OkaeriConfig implements PlatformMessages {

    @Comment("Colour scheme for technical messages - prepended to each one. Editing it")
    @Comment("restyles them all at once; the texts below carry no colour. Empty prefix =")
    @Comment("messages with no label.")
    @CustomKey("prefix")
    public String prefix = "<red><bold>ERROR > </bold><gray>";

    @Comment("")
    @Comment("Prefix for a usage hint. Wrong usage is a hint, not a player error, so it is")
    @Comment("yellow rather than red.")
    @CustomKey("usage-prefix")
    public String usagePrefix = "<yellow><bold>USAGE > </bold><gray>";

    @Comment("")
    @Comment("Prefix for an unknown command - plain red, no label. A typo is not a failure to")
    @Comment("announce with the word 'ERROR', just a short 'no such thing'.")
    @CustomKey("not-found-prefix")
    public String notFoundPrefix = "<red>";

    @Comment("")
    @Comment("Technical messages, used by every VibeSSH plugin. Gameplay messages belong to the")
    @Comment("plugin that owns the feature. An empty value switches a message off.")
    @Comment("Formatting: MiniMessage. Placeholders are tags, e.g. <usage>.")
    @Comment("Highlight a value inside a sentence with <yellow>...</yellow>.")
    @Comment("")
    @CustomKey("command-not-found")
    public String commandNotFound = "Unknown command.";

    @Comment("Placeholder: <usage>")
    @CustomKey("command-invalid-usage")
    public String commandInvalidUsage = "Correct usage: <yellow><usage>";

    @Comment("Placeholder: <permission>")
    @CustomKey("command-no-permission")
    public String commandNoPermission = "You don't have permission to use this command.";

    @CustomKey("command-player-only")
    public String commandPlayerOnly = "Only a player can use this command.";

    @CustomKey("command-console-only")
    public String commandConsoleOnly = "Only the console can use this command.";

    @Comment("Placeholder: <time> - the remaining time, e.g. 5s")
    @CustomKey("command-cooldown")
    public String commandCooldown = "Wait <yellow><time></yellow> before using this again.";

    @Comment("Placeholder: <input> - the value that could not be read")
    @CustomKey("command-invalid-argument")
    public String commandInvalidArgument = "Invalid value: <yellow><input>";

    @Comment("")
    @Comment("Placeholder: <player>")
    @CustomKey("player-not-found")
    public String playerNotFound = "Player <yellow><player></yellow> not found.";

    @Comment("Placeholder: <world>")
    @CustomKey("world-not-found")
    public String worldNotFound = "World <yellow><world></yellow> not found.";

    @Comment("Placeholder: <server> - proxy only")
    @CustomKey("server-not-found")
    public String serverNotFound = "Server <yellow><server></yellow> not found.";

    @Comment("")
    @Comment("Sent when a command throws. The details go to the server log.")
    @CustomKey("internal-error")
    public String internalError = "Something went wrong while running this.";

    /** The prefix followed by the notice's own text; an empty body stays empty. */
    @Override
    public String message(PlatformNotice notice) {
        String body = body(notice);
        if (body.isEmpty()) {
            return "";
        }
        return prefixFor(notice) + body;
    }

    /** The label a notice is announced under: usage in yellow, a typo unlabelled, else the error prefix. */
    protected String prefixFor(PlatformNotice notice) {
        return switch (notice) {
            case COMMAND_INVALID_USAGE -> this.usagePrefix;
            case COMMAND_NOT_FOUND -> this.notFoundPrefix;
            default -> this.prefix;
        };
    }

    /** The message text without the prefix, as it appears in YAML. */
    public String body(PlatformNotice notice) {
        return switch (notice) {
            case COMMAND_NOT_FOUND -> this.commandNotFound;
            case COMMAND_INVALID_USAGE -> this.commandInvalidUsage;
            case COMMAND_NO_PERMISSION -> this.commandNoPermission;
            case COMMAND_PLAYER_ONLY -> this.commandPlayerOnly;
            case COMMAND_CONSOLE_ONLY -> this.commandConsoleOnly;
            case COMMAND_COOLDOWN -> this.commandCooldown;
            case COMMAND_INVALID_ARGUMENT -> this.commandInvalidArgument;
            case PLAYER_NOT_FOUND -> this.playerNotFound;
            case WORLD_NOT_FOUND -> this.worldNotFound;
            case SERVER_NOT_FOUND -> this.serverNotFound;
            case INTERNAL_ERROR -> this.internalError;
        };
    }
}
