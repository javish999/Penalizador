package bot.commands;

import java.util.List;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {

    String getName();

    void execute(
            MessageReceivedEvent event,
            List<String> arguments
    );

    String getDescription();

    String getUsage();
}
