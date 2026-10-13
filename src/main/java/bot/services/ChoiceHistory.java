package bot.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChoiceHistory {

    private final Map<String, ChoiceSelection> selections = new ConcurrentHashMap<>();

    public void save(
            MessageReceivedEvent event,
            java.util.List<String> options,
            int selectedIndex
    ) {
        selections.put(getKey(event), new ChoiceSelection(options, selectedIndex));
    }

    public ChoiceSelection get(MessageReceivedEvent event) {
        return selections.get(getKey(event));
    }

    private String getKey(MessageReceivedEvent event) {
        return event.isFromGuild()
                ? event.getGuild().getId()
                : event.getChannel().getId();
    }
}
