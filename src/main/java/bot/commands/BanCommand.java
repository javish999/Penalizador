package bot.commands;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class BanCommand implements Command {

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        if (!event.isFromGuild()) {
            event.getChannel().sendMessage("This command can only be used in a server.").queue();
            return;
        }

        Member moderator = event.getMember();
        if (moderator == null || !moderator.hasPermission(Permission.BAN_MEMBERS)) {
            event.getChannel().sendMessage("You need the Ban Members permission to use this command.").queue();
            return;
        }

        List<Member> mentionedMembers = event.getMessage().getMentions().getMembers();
        if (mentionedMembers.size() != 1) {
            event.getChannel().sendMessage("Please mention exactly one member. Use: ban @member [reason]").queue();
            return;
        }

        Member target = mentionedMembers.getFirst();
        if (target.equals(moderator)) {
            event.getChannel().sendMessage("You cannot ban yourself.").queue();
            return;
        }
        if (!moderator.canInteract(target)) {
            event.getChannel().sendMessage("You cannot ban a member with an equal or higher role.").queue();
            return;
        }
        if (!event.getGuild().getSelfMember().canInteract(target)) {
            event.getChannel().sendMessage("My role must be higher than the member's role to ban them.").queue();
            return;
        }

        String reason = arguments.size() > 1
                ? String.join(" ", arguments.subList(1, arguments.size()))
                : "No reason provided";

        event.getGuild().ban(target.getUser(), 0, TimeUnit.DAYS)
                .reason(reason)
                .queue(
                        ignored -> event.getChannel().sendMessage("Banned " + target.getAsMention() + ".").queue(),
                        error -> event.getChannel().sendMessage("I could not ban that member. Check my permissions and role position.").queue()
                );
    }

    @Override
    public String getDescription() {
        return "Bans a member if you and the bot have a higher role.";
    }

    @Override
    public String getUsage() {
        return "ban <@member> [reason]";
    }
}
