package bot;

import bot.commands.*;
import bot.listeners.MessageListener;
import bot.config.PrefixManager;
import bot.config.PenaltyLanguageManager;
import bot.services.ChoiceHistory;
import bot.services.ReminderRepository;
import bot.services.ReminderService;
import bot.services.PenaltyService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args)
            throws InterruptedException {

        String token = System.getenv("DISCORD_TOKEN");

        if (token == null) {
            System.out.println(
                    "ERROR: DISCORD_TOKEN environment variable not found."
            );
            return;
        }

        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            System.out.println(
                    "ERROR: DATABASE_URL environment variable not found."
            );
            return;
        }

        ReminderRepository reminderRepository = new ReminderRepository(databaseUrl);
        try {
            reminderRepository.initialize();
        } catch (SQLException exception) {
            System.err.println("ERROR: Unable to initialize reminders storage: " + exception.getMessage());
            return;
        }
        ReminderService reminderService = new ReminderService(reminderRepository);

        CommandParser commandParser = new CommandParser();

        CommandManager commandManager =
                new CommandManager();

        PrefixManager prefixManager = new PrefixManager();
        ChoiceHistory choiceHistory = new ChoiceHistory();
        PenaltyService penaltyService = new PenaltyService();
        PenaltyLanguageManager penaltyLanguageManager = new PenaltyLanguageManager();

        commandManager.register(
                new PingCommand()
        );

        commandManager.register(
                new RemindCommand(reminderService)
        );

        commandManager.register(
                new HelpCommand(commandManager, prefixManager)
        );

        commandManager.register(
                new ChooseCommand(choiceHistory)
        );

        commandManager.register(
                new RerollChooseCommand(choiceHistory)
        );

        commandManager.register(
                new BanCommand()
        );

        commandManager.register(
                new PenCommand(penaltyService, penaltyLanguageManager)
        );

        commandManager.register(
                new SetLanguageCommand(penaltyLanguageManager)
        );

        commandManager.register(
                new PrefixCommand(prefixManager)
        );

        MessageListener messageListener =
                new MessageListener(
                        commandParser,
                        commandManager,
                        prefixManager
                );

        JDA jda = JDABuilder
                .createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(messageListener)
                .build();

        jda.awaitReady();
        reminderService.start(jda);
    }
}
