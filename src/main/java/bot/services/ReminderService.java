package bot.services;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;

public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ScheduledExecutorService scheduler;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public long createReminder(Reminder reminder) throws SQLException {
        return reminderRepository.save(
                reminder.channelId(),
                reminder.userId(),
                reminder.message(),
                reminder.remindAt()
        );
    }

    public void start(JDA jda) {
        scheduler.scheduleWithFixedDelay(
                () -> deliverDueReminders(jda),
                0,
                15,
                TimeUnit.SECONDS
        );
    }

    private void deliverDueReminders(JDA jda) {
        try {
            for (Reminder reminder : reminderRepository.claimDueReminders()) {
                jda.getTextChannelById(reminder.channelId())
                        .sendMessage("<@" + reminder.userId() + "> Reminder: " + reminder.message())
                        .queue(
                                ignored -> markDelivered(reminder.id()),
                                ignored -> markPending(reminder.id())
                        );
            }
        } catch (SQLException exception) {
            System.err.println("Unable to check due reminders: " + exception.getMessage());
        }
    }

    private void markDelivered(long reminderId) {
        try {
            reminderRepository.markDelivered(reminderId);
        } catch (SQLException exception) {
            System.err.println("Unable to mark reminder as delivered: " + exception.getMessage());
        }
    }

    private void markPending(long reminderId) {
        try {
            reminderRepository.markPending(reminderId);
        } catch (SQLException exception) {
            System.err.println("Unable to retry reminder: " + exception.getMessage());
        }
    }
}
