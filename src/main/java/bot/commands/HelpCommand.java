package bot.commands;

import bot.config.PrefixManager;
import java.util.Comparator;
import java.util.List;

public class HelpCommand implements Command {

    private final CommandManager commandManager;
    private final PrefixManager prefixManager;

    public HelpCommand(
            CommandManager commandManager,
            PrefixManager prefixManager
    ) {
        this.commandManager = commandManager;
        this.prefixManager = prefixManager;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void execute(
            net.dv8tion.jda.api.events.message.MessageReceivedEvent event,
            List<String> arguments
    ) {
        String prefix = event.isFromGuild()
                ? prefixManager.getPrefix(event.getGuild().getIdLong())
                : prefixManager.getDefaultPrefix();

        if (arguments.isEmpty()) {
            sendGeneralHelp(event, prefix);
            return;
        }

        Command command = commandManager.getCommand(arguments.getFirst());
        if (command == null) {
            event.getChannel()
                    .sendMessage("Command " + arguments.getFirst() + " not found.")
                    .queue();
            return;
        }

        event.getChannel()
                .sendMessage(formatCommandHelp(command, prefix))
                .queue();
    }

    @Override
    public String getDescription() {
        return "Shows available commands or details about one command.";
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }

    private void sendGeneralHelp(
            net.dv8tion.jda.api.events.message.MessageReceivedEvent event,
            String prefix
    ) {
        StringBuilder helpMessage = new StringBuilder("Available commands:\n");
        commandManager.getCommands().stream()
                .sorted(Comparator.comparing(Command::getName))
                .forEach(command -> helpMessage
                        .append(prefix)
                        .append(command.getUsage())
                        .append(" - ")
                        .append(command.getDescription())
                        .append('\n'));

        event.getChannel().sendMessage(helpMessage.toString()).queue();
    }

    private String formatCommandHelp(Command command, String prefix) {
        return "Command: " + command.getName()
                + "\nDescription: " + command.getDescription()
                + "\nUsage: " + prefix + command.getUsage();
    }
}
