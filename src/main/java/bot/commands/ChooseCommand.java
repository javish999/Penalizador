package bot.commands;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import bot.services.ChoiceHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChooseCommand implements Command {

    private final ChoiceHistory choiceHistory;

    public ChooseCommand(ChoiceHistory choiceHistory) {
        this.choiceHistory = choiceHistory;
    }

    @Override
    public String getName() {
        return "choose";
    }

    @Override
    public void execute(
            MessageReceivedEvent event,
            List<String> arguments
    ) {
        if (arguments.isEmpty()) {
            event.getChannel()
                    .sendMessage("Please provide at least two options. Use: choose option1 option2 ...")
                    .queue();
            return;
        }
        if (arguments.size() == 1) {
            event.getChannel()
                    .sendMessage("Please provide at least two options. Use: choose option1 option2 ...")
                    .queue();
            return;
        }
        
        int randomIndex = ThreadLocalRandom.current().nextInt(arguments.size());
        choiceHistory.save(event, arguments, randomIndex);
        String chosenOption = arguments.get(randomIndex);

        event.getChannel()
                .sendMessage("I choose: " + chosenOption)
                .queue();
    }

    @Override
    public String getDescription() {
        return "Randomly chooses one of the provided options.";
    }

    @Override
    public String getUsage() {
        return "choose <option1> <option2> [...]";
    }
}
