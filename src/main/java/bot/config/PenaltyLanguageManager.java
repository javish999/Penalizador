package bot.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PenaltyLanguageManager {

    public static final int ENGLISH = 1;
    public static final int SPANISH = 2;

    private final Map<Long, Integer> languages = new ConcurrentHashMap<>();

    public int getLanguage(long guildId) {
        return languages.getOrDefault(guildId, ENGLISH);
    }

    public void setLanguage(long guildId, int language) {
        languages.put(guildId, language);
    }
}
