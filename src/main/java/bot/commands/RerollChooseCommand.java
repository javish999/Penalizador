package bot.commands;

import bot.services.ChoiceHistory;
import bot.services.ChoiceSelection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RerollChooseCommand implements Command {

    private final ChoiceHistory choiceHistory;

    public RerollChooseCommand(ChoiceHistory choiceHistory) {
        this.choiceHistory = choiceHistory;
    }

    @Override
    public String getName() {
        return "reroll";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        ChoiceSelection previousSelection = choiceHistory.get(event);
        if (previousSelection == null) {
            event.getChannel()
                    .sendMessage("No previous options to reroll. Use choose first.")
                    .queue();
            return;
        }

        List<String> options = previousSelection.options();
        int randomIndex = ThreadLocalRandom.current().nextInt(options.size());

        choiceHistory.save(event, options, randomIndex);
        event.getChannel()
                .sendMessage("I rerolled and chose: " + options.get(randomIndex))
                .queue();
    }

    @Override
    public String getDescription() {
        return "Randomly chooses from the last choose options in this server.";
    }

    @Override
    public String getUsage() {
        return "reroll";
    }
}
