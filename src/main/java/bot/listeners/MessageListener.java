package bot.listeners;

import bot.commands.CommandManager;
import bot.commands.CommandParser;
import bot.commands.ParsedCommand;
import bot.config.PrefixManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    private final CommandParser commandParser;
    private final CommandManager commandManager;
    private final PrefixManager prefixManager;

    public MessageListener(
            CommandParser commandParser,
            CommandManager commandManager,
            PrefixManager prefixManager
    ) {
        this.commandParser = commandParser;
        this.commandManager = commandManager;
        this.prefixManager = prefixManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event
                .getMessage()
                .getContentRaw();

        String prefix = event.isFromGuild()
                ? prefixManager.getPrefix(event.getGuild().getIdLong())
                : prefixManager.getDefaultPrefix();

        ParsedCommand parsedCommand = commandParser.parse(
                message,
                prefix
        );

        if (parsedCommand == null) {
            return;
        }

        commandManager.execute(
                parsedCommand.name(),
                parsedCommand.arguments(),
                event
        );
    }
}
