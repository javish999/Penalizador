package bot.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrefixManager {

    private static final String DEFAULT_PREFIX = "!";

    private final Map<Long, String> prefixes = new ConcurrentHashMap<>();

    public String getPrefix(long guildId) {
        return prefixes.getOrDefault(guildId, DEFAULT_PREFIX);
    }

    public String getDefaultPrefix() {
        return DEFAULT_PREFIX;
    }

    public void setPrefix(long guildId, String prefix) {
        prefixes.put(guildId, prefix);
    }
}
