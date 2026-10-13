package bot.commands;

import bot.services.Reminder;
import bot.services.ReminderService;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RemindCommand implements Command {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uu HH:mm");

    private final ReminderService reminderService;
    private final ZoneId zoneId;

    public RemindCommand(ReminderService reminderService) {
        this(reminderService, ZoneId.systemDefault());
    }

    RemindCommand(ReminderService reminderService, ZoneId zoneId) {
        this.reminderService = reminderService;
        this.zoneId = zoneId;
    }

    @Override
    public String getName() {
        return "remind";
    }

    @Override
    public String getDescription() {
        return "Creates a reminder for a date/time or a relative delay.";
    }

    @Override
    public String getUsage() {
        return "remind <message> <dd/MM/yy> <HH:mm> | remind <message> <amount> <h|min>";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        if (!event.isFromGuild()) {
            event.getChannel()
                    .sendMessage("Reminders can only be created in a server channel.")
                    .queue();
            return;
        }

        if (arguments.size() < 3) {
            sendUsage(event);
            return;
        }

        OffsetDateTime remindAt;
        String message;
        try {
            if (isRelativeReminder(arguments)) {
                long amount = Long.parseLong(arguments.get(arguments.size() - 2));
                if (amount <= 0) {
                    throw new IllegalArgumentException();
                }
                remindAt = createRelativeTime(amount, arguments.getLast());
                message = String.join(" ", arguments.subList(0, arguments.size() - 2));
            } else {
                remindAt = createAbsoluteTime(
                        arguments.get(arguments.size() - 2),
                        arguments.getLast()
                );
                message = String.join(" ", arguments.subList(0, arguments.size() - 2));
            }
        } catch (DateTimeException | IllegalArgumentException exception) {
            event.getChannel()
                    .sendMessage("Invalid reminder time. Use: " + getUsage())
                    .queue();
            return;
        }

        if (message.isBlank() || !remindAt.isAfter(OffsetDateTime.now(zoneId))) {
            event.getChannel()
                    .sendMessage("The reminder message must not be empty and the time must be in the future.")
                    .queue();
            return;
        }

        try {
            Reminder reminder = new Reminder(
                    0,
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong(),
                    message,
                    remindAt
            );
            reminderService.createReminder(reminder);
            event.getChannel()
                    .sendMessage("Reminder set for " + DISPLAY_FORMATTER.format(remindAt) + ".")
                    .queue();
        } catch (SQLException exception) {
            System.err.println("Unable to save reminder: " + exception.getMessage());
            event.getChannel()
                    .sendMessage("Unable to save the reminder right now. Please try again later.")
                    .queue();
        }
    }

    private boolean isRelativeReminder(List<String> arguments) {
        String unit = arguments.getLast();
        return unit.equalsIgnoreCase("h") || unit.equalsIgnoreCase("min");
    }

    private OffsetDateTime createRelativeTime(long amount, String unit) {
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        if (unit.equalsIgnoreCase("h")) {
            return now.plusHours(amount);
        }
        return now.plusMinutes(amount);
    }

    private OffsetDateTime createAbsoluteTime(String date, String time) {
        LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
        LocalTime localTime = LocalTime.parse(time, TIME_FORMATTER);
        return LocalDateTime.of(localDate, localTime)
                .atZone(zoneId)
                .toOffsetDateTime();
    }

    private void sendUsage(MessageReceivedEvent event) {
        event.getChannel().sendMessage("Usage: " + getUsage()).queue();
    }
}
