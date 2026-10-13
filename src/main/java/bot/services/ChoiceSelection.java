package bot.services;

import java.util.List;

public record ChoiceSelection(List<String> options, int selectedIndex) {

    public ChoiceSelection {
        options = List.copyOf(options);
    }
}
