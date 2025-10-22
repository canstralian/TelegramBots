package org.telegram.telegrambots.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base class for Telegram long polling bots with session management support.
 *
 * <p>This class integrates Apache Shiro session management with Telegram bot updates,
 * providing automatic session creation and retrieval based on chat IDs.
 *
 * <p>Sessions are automatically created for new chats and retrieved for existing ones.
 * The session is passed to the {@link #onUpdateReceived(Update, Optional)} method for
 * processing.
 *
 * <p><b>Thread Safety:</b> This class is thread-safe if the underlying SessionManager
 * and ChatIdConverter implementations are thread-safe.
 *
 * @author TelegramBots Contributors
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TelegramLongPollingSessionBot extends TelegramLongPollingBot {
    private final DefaultSessionManager sessionManager;
    private final ChatIdConverter chatIdConverter;
    /**
     * Constructs a bot using default options.
     * If this is used, getBotToken() must be overridden to return the bot token.
     *
     * @deprecated Overwriting the getBotToken() method is deprecated. Use {@link #TelegramLongPollingSessionBot(String)} instead
     */
    @Deprecated
    public TelegramLongPollingSessionBot(){
        this(new DefaultChatIdConverter());
    }

    /**
     * Constructs a bot with a custom chat ID converter.
     * If this is used, getBotToken() must be overridden to return the bot token.
     *
     * @param chatIdConverter the converter for chat IDs to session IDs
     * @deprecated Overwriting the getBotToken() method is deprecated. Use {@link #TelegramLongPollingSessionBot(ChatIdConverter, String)} instead
     */
    @Deprecated
    public TelegramLongPollingSessionBot(ChatIdConverter chatIdConverter){
        this(chatIdConverter, new DefaultBotOptions());
    }

    /**
     * Constructs a bot with custom options and chat ID converter.
     * If this is used, getBotToken() must be overridden to return the bot token.
     *
     * @param chatIdConverter the converter for chat IDs to session IDs
     * @param defaultBotOptions bot configuration options
     * @deprecated Overwriting the getBotToken() method is deprecated. Use {@link #TelegramLongPollingSessionBot(ChatIdConverter, DefaultBotOptions, String)} instead
     */
    @Deprecated
    public TelegramLongPollingSessionBot(ChatIdConverter chatIdConverter, DefaultBotOptions defaultBotOptions){
        this(chatIdConverter, defaultBotOptions, null);
    }

    /**
     * Constructs a bot with the specified token using default options.
     *
     * @param botToken the bot authentication token
     * @throws NullPointerException if botToken is null
     */
    public TelegramLongPollingSessionBot(String botToken){
        this(new DefaultChatIdConverter(), botToken);
    }

    /**
     * Constructs a bot with custom chat ID converter and token.
     *
     * @param chatIdConverter the converter for chat IDs to session IDs
     * @param botToken the bot authentication token
     * @throws NullPointerException if chatIdConverter is null
     */
    public TelegramLongPollingSessionBot(ChatIdConverter chatIdConverter, String botToken){
        this(chatIdConverter, new DefaultBotOptions(), botToken);
    }

    /**
     * Main constructor with full configuration.
     *
     * @param chatIdConverter the converter for chat IDs to session IDs
     * @param defaultBotOptions bot configuration options
     * @param botToken the bot authentication token (may be null for deprecated constructors)
     * @throws NullPointerException if chatIdConverter or defaultBotOptions is null
     */
    public TelegramLongPollingSessionBot(ChatIdConverter chatIdConverter, DefaultBotOptions defaultBotOptions, String botToken){
        super(defaultBotOptions, botToken);
        this.chatIdConverter = Objects.requireNonNull(chatIdConverter, "chatIdConverter cannot be null");
        this.sessionManager = initializeSessionManager();
    }

    /**
     * Initializes and configures the session manager.
     *
     * @return configured DefaultSessionManager
     */
    private DefaultSessionManager initializeSessionManager() {
        DefaultSessionManager manager = new DefaultSessionManager();
        SessionDAO sessionDAO = manager.getSessionDAO();
        if (!(sessionDAO instanceof AbstractSessionDAO)) {
            throw new IllegalStateException("SessionDAO must be an instance of AbstractSessionDAO");
        }
        ((AbstractSessionDAO) sessionDAO).setSessionIdGenerator(this.chatIdConverter);
        return manager;
    }

    /**
     * Processes incoming updates and manages session lifecycle.
     *
     * <p>This method extracts the chat information from the update, retrieves or creates
     * a session for the chat, and delegates to {@link #onUpdateReceived(Update, Optional)}.
     *
     * @param update the incoming update from Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        Optional<Session> chatSession;
        Message message;
        if (update.hasMessage()) {
            message = update.getMessage();
        } else if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
        } else {
            onUpdateReceived(update, Optional.empty());
            return;
        }
        chatIdConverter.setSessionId(message.getChatId());
        chatSession = getSession(message);
        onUpdateReceived(update, chatSession);
    }

    /**
     * Retrieves or creates a session for the given message.
     *
     * @param message the message containing chat information
     * @return an Optional containing the session
     */
    public Optional<Session> getSession(Message message){
        Objects.requireNonNull(message, "message cannot be null");
        
        try {
            return Optional.of(sessionManager.getSession(chatIdConverter));
        } catch (UnknownSessionException e) {
            SessionContext botSession = new DefaultChatSessionContext(
                message.getChatId(), 
                message.getFrom().getUserName()
            );
            return Optional.of(sessionManager.start(botSession));
        }
    }

    /**
     * Processes the update with the associated session.
     *
     * <p>Implementations should handle the business logic for processing updates.
     *
     * @param update the incoming update
     * @param botSession the session associated with the chat, or empty if no session context is available
     */
    public abstract void onUpdateReceived(Update update, Optional<Session> botSession);
}
