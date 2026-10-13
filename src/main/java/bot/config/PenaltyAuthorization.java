package bot.config;

import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public final class PenaltyAuthorization {

    private static final String CONFIGURED_GUILD_NAME = "Butaneros Puteros";
    private static final String MINIMUM_ROLE_NAME = "Putero Premium";

    private PenaltyAuthorization() {
    }

    public static boolean usesConfiguredGuildPolicy(Guild guild) {
        return guild.getName().equalsIgnoreCase(CONFIGURED_GUILD_NAME);
    }

    public static Result check(Member member) {
        if (!usesConfiguredGuildPolicy(member.getGuild())) {
            return member.hasPermission(Permission.MODERATE_MEMBERS)
                    ? Result.ALLOWED
                    : Result.MISSING_PERMISSION;
        }

        var minimumPosition = getMinimumRolePosition(member);
        if (minimumPosition.isEmpty()) {
            return Result.REQUIRED_ROLE_NOT_FOUND;
        }

        return hasMinimumRole(member, minimumPosition.getAsInt())
                ? Result.ALLOWED
                : Result.MISSING_PERMISSION;
    }

    public static Result checkPenaltyTarget(Member target) {
        if (!usesConfiguredGuildPolicy(target.getGuild())) {
            return Result.ALLOWED;
        }

        var minimumPosition = getMinimumRolePosition(target);
        if (minimumPosition.isEmpty()) {
            return Result.REQUIRED_ROLE_NOT_FOUND;
        }

        return hasMinimumRole(target, minimumPosition.getAsInt())
                ? Result.ALLOWED
                : Result.TARGET_BELOW_MINIMUM_ROLE;
    }

    private static java.util.OptionalInt getMinimumRolePosition(Member member) {
        return member.getGuild().getRolesByName(MINIMUM_ROLE_NAME, true).stream()
                .mapToInt(Role::getPosition)
                .min();
    }

    private static boolean hasMinimumRole(Member member, int minimumPosition) {
        return member.getRoles().stream()
                .anyMatch(role -> role.getPosition() >= minimumPosition);
    }

    public enum Result {
        ALLOWED,
        MISSING_PERMISSION,
        REQUIRED_ROLE_NOT_FOUND,
        TARGET_BELOW_MINIMUM_ROLE
    }
}
