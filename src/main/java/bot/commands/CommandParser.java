package bot.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandParser {

    public ParsedCommand parse(String message, String prefix) {

        if (message == null || !message.startsWith(prefix)) {
            return null;
        }

        String content = message
                .substring(prefix.length())
                .trim();

        if (content.isEmpty()) {
            return null;
        }

        String[] parts = content.split("\\s+");

        String commandName = parts[0]
                .toLowerCase(Locale.ROOT);

        List<String> arguments = Arrays.stream(parts)
                .skip(1)
                .toList();

        return new ParsedCommand(
                commandName,
                arguments
        );
    }
}
