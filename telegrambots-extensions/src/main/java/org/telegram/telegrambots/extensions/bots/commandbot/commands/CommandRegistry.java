package org.telegram.telegrambots.extensions.bots.commandbot.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * This class manages all the commands for a bot. You can register and deregister commands on demand
 *
 * <p>This class is thread-safe and can be safely accessed from multiple threads.
 * All operations are atomic and consistent.
 *
 * @author Timo Schulz (Mit0x2)
 */
public final class CommandRegistry implements ICommandRegistry {

    private final Map<String, IBotCommand> commandRegistryMap = new ConcurrentHashMap<>();
    private final boolean allowCommandsWithUsername;
    private final Supplier<String> botUsernameSupplier;
    private volatile BiConsumer<AbsSender, Message> defaultConsumer;

    /**
     * Creates a Command registry
     * @param allowCommandsWithUsername True to allow commands with username, false otherwise
     * @param botUsernameSupplier       Bot username supplier
     */
    public CommandRegistry(boolean allowCommandsWithUsername, Supplier<String> botUsernameSupplier) {
        this.allowCommandsWithUsername = allowCommandsWithUsername;
        this.botUsernameSupplier = Objects.requireNonNull(botUsernameSupplier, "botUsernameSupplier cannot be null");
    }

    /**
     * Registers a default action to be executed when a command is not found.
     *
     * @param defaultConsumer the consumer to handle unrecognized commands
     */
    @Override
    public void registerDefaultAction(BiConsumer<AbsSender, Message> defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
    }

    /**
     * Registers a command in the registry.
     *
     * @param botCommand the command to register
     * @return true if the command was registered successfully, false if a command with the same identifier already exists
     * @throws NullPointerException if botCommand is null
     */
    @Override
    public final boolean register(IBotCommand botCommand) {
        Objects.requireNonNull(botCommand, "botCommand cannot be null");
        return commandRegistryMap.putIfAbsent(botCommand.getCommandIdentifier(), botCommand) == null;
    }

    /**
     * Registers multiple commands at once.
     *
     * @param botCommands the commands to register
     * @return a map indicating which commands were successfully registered
     * @throws NullPointerException if botCommands is null
     */
    @Override
    public final Map<IBotCommand, Boolean> registerAll(IBotCommand... botCommands) {
        Objects.requireNonNull(botCommands, "botCommands cannot be null");
        Map<IBotCommand, Boolean> resultMap = new ConcurrentHashMap<>(botCommands.length);
        for (IBotCommand botCommand : botCommands) {
            resultMap.put(botCommand, register(botCommand));
        }
        return resultMap;
    }

    /**
     * Deregisters a command from the registry.
     *
     * @param botCommand the command to deregister
     * @return true if the command was found and removed, false otherwise
     * @throws NullPointerException if botCommand is null
     */
    @Override
    public final boolean deregister(IBotCommand botCommand) {
        Objects.requireNonNull(botCommand, "botCommand cannot be null");
        return commandRegistryMap.remove(botCommand.getCommandIdentifier()) != null;
    }

    /**
     * Deregisters multiple commands at once.
     *
     * @param botCommands the commands to deregister
     * @return a map indicating which commands were successfully deregistered
     * @throws NullPointerException if botCommands is null
     */
    @Override
    public final Map<IBotCommand, Boolean> deregisterAll(IBotCommand... botCommands) {
        Objects.requireNonNull(botCommands, "botCommands cannot be null");
        Map<IBotCommand, Boolean> resultMap = new ConcurrentHashMap<>(botCommands.length);
        for (IBotCommand botCommand : botCommands) {
            resultMap.put(botCommand, deregister(botCommand));
        }
        return resultMap;
    }

    /**
     * Returns an unmodifiable view of all registered commands.
     *
     * @return an unmodifiable collection of registered commands
     */
    @Override
    public final Collection<IBotCommand> getRegisteredCommands() {
        return Collections.unmodifiableCollection(commandRegistryMap.values());
    }

    /**
     * Retrieves a specific registered command by its identifier.
     *
     * @param commandIdentifier the identifier of the command to retrieve
     * @return the command if found, null otherwise
     */
    @Override
    public final IBotCommand getRegisteredCommand(String commandIdentifier) {
        return commandRegistryMap.get(commandIdentifier);
    }

    /**
     * Executes a command action if the command is registered.
     *
     * @apiNote  If the command is not registered and there is a default consumer,
     * that action will be performed
     *
     * @param absSender absSender
     * @param message input message
     * @return True if a command or default action is executed, false otherwise
     */
    public final boolean executeCommand(AbsSender absSender, Message message) {
        if (message.hasText()) {
            String text = message.getText();
            if (text.startsWith(BotCommand.COMMAND_INIT_CHARACTER)) {
                String commandMessage = text.substring(1);
                String[] commandSplit = BotCommand.COMMAND_PARAMETER_SEPARATOR_PATTERN.split(commandMessage);

                String command = removeUsernameFromCommandIfNeeded(commandSplit[0]);

                if (commandRegistryMap.containsKey(command)) {
                    String[] parameters = Arrays.copyOfRange(commandSplit, 1, commandSplit.length);
                    commandRegistryMap.get(command).processMessage(absSender, message, parameters);
                    return true;
                } else if (defaultConsumer != null) {
                    defaultConsumer.accept(absSender, message);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * if {@link #allowCommandsWithUsername} is enabled, the username of the bot is removed from
     * the command
     * @param command Command to simplify
     * @return Simplified command
     * @throws java.lang.NullPointerException if {@code allowCommandsWithUsername} is {@code true}
     *                                        and {@code botUsernameSupplier} returns {@code null}
     */
    private String removeUsernameFromCommandIfNeeded(String command) {
        if (allowCommandsWithUsername) {
            String botUsername = Objects.requireNonNull(botUsernameSupplier.get(), "Bot username must not be null");
            // Efficient string-based username removal instead of regex
            int atIndex = command.indexOf('@');
            if (atIndex > 0) {
                String username = command.substring(atIndex + 1);
                if (username.equalsIgnoreCase(botUsername)) {
                    return command.substring(0, atIndex);
                }
            }
        }
        return command;
    }
}
