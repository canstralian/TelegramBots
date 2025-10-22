package org.telegram.telegrambots.extensions.bots.commandbot.commands;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Representation of a command, which can be executed
 *
 * <p>Commands are identified by a command string (e.g., "start", "help")
 * and can be invoked by users with the "/" prefix (e.g., "/start").
 *
 * <p>This class is thread-safe as all fields are immutable after construction.
 *
 * @author Timo Schulz (Mit0x2)
 */
public abstract class BotCommand implements IBotCommand {
    /**
     * The character that initiates a command.
     */
    public static final String COMMAND_INIT_CHARACTER = "/";
    
    /**
     * Regular expression pattern for separating command parameters.
     * Matches one or more whitespace characters.
     * @deprecated Use {@link #COMMAND_PARAMETER_SEPARATOR_PATTERN} for better performance
     */
    @Deprecated
    public static final String COMMAND_PARAMETER_SEPARATOR_REGEXP = "\\s+";
    
    /**
     * Pre-compiled pattern for separating command parameters.
     * Matches one or more whitespace characters for optimal performance.
     */
    public static final Pattern COMMAND_PARAMETER_SEPARATOR_PATTERN = Pattern.compile("\\s+");
    
    /**
     * Maximum length of a command identifier (excluding the "/" prefix).
     * Based on Telegram's command length limitations.
     */
    private static final int COMMAND_MAX_LENGTH = 32;

    private final String commandIdentifier;
    private final String description;

    /**
     * Constructs a new bot command with the specified identifier and description.
     *
     * <p>The command identifier will be normalized by:
     * <ul>
     *   <li>Removing the leading "/" if present</li>
     *   <li>Converting to lowercase using ROOT locale</li>
     *   <li>Validating length constraints</li>
     * </ul>
     *
     * @param commandIdentifier the unique identifier for this command (e.g., "start" or "/start")
     * @param description a human-readable description of what this command does
     * @throws IllegalArgumentException if commandIdentifier is null, empty, or exceeds maximum length
     * @throws NullPointerException if description is null
     */
    public BotCommand(String commandIdentifier, String description) {
        Objects.requireNonNull(commandIdentifier, "commandIdentifier for command cannot be null");
        Objects.requireNonNull(description, "description for command cannot be null");

        String processedIdentifier = commandIdentifier;
        if (processedIdentifier.isEmpty()) {
            throw new IllegalArgumentException("commandIdentifier for command cannot be empty");
        }

        if (processedIdentifier.startsWith(COMMAND_INIT_CHARACTER)) {
            processedIdentifier = processedIdentifier.substring(1);
        }

        if (processedIdentifier.isEmpty()) {
            throw new IllegalArgumentException("commandIdentifier cannot be empty after removing prefix");
        }

        if (processedIdentifier.length() + 1 > COMMAND_MAX_LENGTH) {
            throw new IllegalArgumentException("commandIdentifier cannot be longer than " + COMMAND_MAX_LENGTH + " (including " + COMMAND_INIT_CHARACTER + ")");
        }

        this.commandIdentifier = processedIdentifier.toLowerCase(Locale.ROOT);
        this.description = description;
    }

    /**
     * Get the identifier of this command
     *
     * @return the identifier
     */
    public final String getCommandIdentifier() {
        return commandIdentifier;
    }

    /**
     * Get the description of this command
     *
     * @return the description as String
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Escapes HTML special characters to prevent injection attacks.
     *
     * @param text the text to escape
     * @return the escaped text safe for HTML output
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    @Override
    public String toString() {
        return "<b>" + COMMAND_INIT_CHARACTER + escapeHtml(getCommandIdentifier()) +
                "</b>\n" + escapeHtml(getDescription());
    }

    /**
     * Process the message and execute the command
     *
     * @param absSender absSender to send messages over
     * @param message   the message to process
     * @param arguments passed arguments
     * @throws NullPointerException if any parameter is null
     */
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        Objects.requireNonNull(absSender, "absSender cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(arguments, "arguments cannot be null");
        
        execute(absSender, message.getFrom(), message.getChat(), arguments);
    }

    /**
     * Execute the command
     *
     * @param absSender absSender to send messages over
     * @param user      the user who sent the command
     * @param chat      the chat, to be able to send replies
     * @param arguments passed arguments
     */
    public abstract void execute(AbsSender absSender, User user, Chat chat, String[] arguments);
}
