package bot.commands;

import bot.config.PenaltyLanguageManager;
import bot.config.PenaltyAuthorization;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetLanguageCommand implements Command {

    private final PenaltyLanguageManager languageManager;

    public SetLanguageCommand(PenaltyLanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public String getName() {
        return "setidioma";
    }

    @Override
    public String getDescription() {
        return "Sets the language used by penalty messages: 1 for English or 2 for Spanish.";
    }

    @Override
    public String getUsage() {
        return "setidioma <1|2>";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("This command can only be used in a server.").queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.getChannel().sendMessage("You need the Moderate Members permission to use this command.").queue();
            return;
        }

        PenaltyAuthorization.Result authorization = PenaltyAuthorization.check(member);
        if (authorization == PenaltyAuthorization.Result.REQUIRED_ROLE_NOT_FOUND) {
            event.getChannel().sendMessage("The required role Putero Premium was not found on this server.").queue();
            return;
        }
        if (authorization != PenaltyAuthorization.Result.ALLOWED) {
            event.getChannel().sendMessage("You need the Putero Premium role or a higher role to use this command.").queue();
            return;
        }

        if (arguments.size() != 1) {
            event.getChannel().sendMessage("Usage: " + getUsage()).queue();
            return;
        }

        String value = arguments.getFirst();
        int language;
        if ("1".equals(value)) {
            language = PenaltyLanguageManager.ENGLISH;
        } else if ("2".equals(value)) {
            language = PenaltyLanguageManager.SPANISH;
        } else {
            event.getChannel().sendMessage("Choose 1 for English or 2 for Spanish.").queue();
            return;
        }

        languageManager.setLanguage(event.getGuild().getIdLong(), language);
        String languageName = language == PenaltyLanguageManager.SPANISH ? "Spanish" : "English";
        event.getChannel().sendMessage("Penalty message language set to " + languageName + ".").queue();
    }
}
