package bot.commands;

import java.util.List;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand implements Command {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public void execute(
            MessageReceivedEvent event,
            List<String> arguments
    ) {

        event.getChannel()
                .sendMessage("Pong!")
                .queue();
    }

    @Override
    public String getDescription() {
        return "Checks the bot's status.";
    }

    @Override
    public String getUsage() {
        return "ping";
    }
}
