package bot.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(
                command.getName().toLowerCase(Locale.ROOT),
                command
        );
    }

    public void execute(
            String commandName,
            List<String> arguments,
            MessageReceivedEvent event
    ) {

        Command command = commands.get(commandName);

        if (command == null) {
            event.getChannel()
                    .sendMessage("Command " + commandName + " not found. Use the help command to see the available commands.")
                    .queue();

            return;
        }

        command.execute(
                event,
                arguments
        );
    }

    public Collection<Command> getCommands() {
        return commands.values();
    }

    public Command getCommand(String name) {
        return commands.get(name.toLowerCase(Locale.ROOT));
    }
}
