package bot.services;

import java.time.OffsetDateTime;

public record Reminder(
        long id,
        long channelId,
        long userId,
        String message,
        OffsetDateTime remindAt
) {}
