package bot.commands;

import bot.config.PrefixManager;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PrefixCommand implements Command {

    private final PrefixManager prefixManager;

    public PrefixCommand(PrefixManager prefixManager) {
        this.prefixManager = prefixManager;
    }

    @Override
    public String getName() {
        return "prefix";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        if (!event.isFromGuild()) {
            event.getChannel()
                    .sendMessage("The prefix can only be changed in a server.")
                    .queue();
            return;
        }

        if (arguments.size() != 1 || arguments.getFirst().isBlank()) {
            event.getChannel()
                    .sendMessage("Please provide exactly one non-empty prefix.")
                    .queue();
            return;
        }

        String prefix = arguments.getFirst();
        prefixManager.setPrefix(event.getGuild().getIdLong(), prefix);
        event.getChannel()
                .sendMessage("Command prefix updated to: " + prefix)
                .queue();
    }

    @Override
    public String getDescription() {
        return "Changes this server's command prefix.";
    }

    @Override
    public String getUsage() {
        return "prefix <new-prefix>";
    }
}
