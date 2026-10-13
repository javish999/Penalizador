package bot.commands;

import java.util.List;

public record ParsedCommand(
        String name,
        List<String> arguments
) {
}
