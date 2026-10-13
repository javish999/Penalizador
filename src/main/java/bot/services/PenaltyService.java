package bot.services;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import static java.time.OffsetDateTime.now;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps Wednesday penalty counts in memory, separated by guild and member.
 */
public class PenaltyService {

    private static final Duration[] TIMEOUT_OPTIONS = {
            Duration.ofMinutes(1),
            Duration.ofMinutes(2),
            Duration.ofMinutes(5),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofDays(1),
            Duration.ofDays(7),
            Duration.ofDays(28)
    };

    private final Map<PenaltyKey, Integer> penalties = new ConcurrentHashMap<>();
    private final ZoneId zoneId;
    private LocalDate penaltyDate;

    public PenaltyService() {
        this(ZoneId.systemDefault());
    }

    public PenaltyService(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public boolean isPenaltyDay() {
        return now(zoneId).getDayOfWeek() == DayOfWeek.WEDNESDAY;
    }

    public synchronized Penalty registerPenalty(long guildId, long memberId) {
        LocalDate today = LocalDate.now(zoneId);
        if (!today.equals(penaltyDate)) {
            penalties.clear();
            penaltyDate = today;
        }

        PenaltyKey key = new PenaltyKey(guildId, memberId);
        int infringementCount = penalties.merge(key, 1, Integer::sum);
        return new Penalty(infringementCount, getTimeoutDuration(infringementCount));
    }

    private Duration getTimeoutDuration(int infringementCount) {
        Duration requestedDuration = Duration.ofMinutes(getRequestedMinutes(infringementCount));
        Duration closestDuration = TIMEOUT_OPTIONS[0];
        long smallestDifference = Math.abs(requestedDuration.minus(closestDuration).toMinutes());

        for (Duration option : TIMEOUT_OPTIONS) {
            long difference = Math.abs(requestedDuration.minus(option).toMinutes());
            if (difference < smallestDifference) {
                closestDuration = option;
                smallestDifference = difference;
            }
        }

        return closestDuration;
    }

    private long getRequestedMinutes(int infringementCount) {
        long requestedMinutes = 1;
        for (int index = 1; index < infringementCount; index++) {
            if (requestedMinutes >= Duration.ofDays(28).toMinutes() / 2) {
                return Duration.ofDays(28).toMinutes();
            }
            requestedMinutes *= 2;
        }
        return requestedMinutes;
    }

    private record PenaltyKey(long guildId, long memberId) {
    }

    public record Penalty(int infringementCount, Duration timeoutDuration) {
    }
}
