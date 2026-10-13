package bot.commands;

import java.time.Duration;
import java.util.List;

import bot.config.PenaltyAuthorization;
import bot.config.PenaltyLanguageManager;
import bot.services.PenaltyService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PenCommand implements Command {

    private static final String BUTANEROS_PUTEROS_GUILD_NAME = "Butaneros Puteros";
    private static final String PENALTY_CHANNEL_NAME = "bot-nhd";
    private static final String PENALTY_LOG_CHANNEL_NAME = "log-nhd";

    private final PenaltyService penaltyService;
    private final PenaltyLanguageManager languageManager;

    public PenCommand(PenaltyService penaltyService, PenaltyLanguageManager languageManager) {
        this.penaltyService = penaltyService;
        this.languageManager = languageManager;
    }

    @Override
    public String getName() {
        return "pen";
    }

    @Override
    public String getDescription() {
        return "Applies an escalating Wednesday timeout to a member.";
    }

    @Override
    public String getUsage() {
        return "pen <@member>";
    }

    @Override
    public void execute(MessageReceivedEvent event, List<String> arguments) {
        if (!event.isFromGuild()) {
            sendMessage(event, "This command can only be used in a server.", "Este comando solo se puede usar en un servidor.");
            return;
        }
        if (!penaltyService.isPenaltyDay()) {
            sendMessage(event, "Penalties can only be issued on Wednesdays.", "Las penalizaciones solo se pueden aplicar los miércoles.");
            return;
        }

        Member moderator = event.getMember();
        if (moderator == null) {
            sendMessage(event,
                    "You are not allowed to use this command.",
                    "No tienes permiso para usar este comando.");
            return;
        }

        PenaltyAuthorization.Result authorization = PenaltyAuthorization.check(moderator);
        if (authorization == PenaltyAuthorization.Result.REQUIRED_ROLE_NOT_FOUND) {
            sendMessage(event,
                    "The required role Putero Premium was not found on this server.",
                    "No se encontró el rol requerido Putero Premium en este servidor.");
            return;
        }
        if (authorization != PenaltyAuthorization.Result.ALLOWED) {
            sendMessage(event,
                    "You need the Putero Premium role or a higher role to use this command.",
                    "Necesitas el rol Putero Premium o uno superior para usar este comando.");
            return;
        }

        List<Member> mentionedMembers = event.getMessage().getMentions().getMembers();
        if (mentionedMembers.size() != 1) {
            sendMessage(event,
                    "Please mention exactly one member. Use: pen @member",
                    "Menciona exactamente a un miembro. Uso: pen @miembro");
            return;
        }

        Member target = mentionedMembers.get(0);
        PenaltyAuthorization.Result targetAuthorization = PenaltyAuthorization.checkPenaltyTarget(target);
        if (targetAuthorization == PenaltyAuthorization.Result.REQUIRED_ROLE_NOT_FOUND) {
            sendMessage(event,
                    "The required role Putero Premium was not found on this server.",
                    "No se encontró el rol requerido Putero Premium en este servidor.");
            return;
        }
        if (targetAuthorization == PenaltyAuthorization.Result.TARGET_BELOW_MINIMUM_ROLE) {
            sendMessage(event,
                    "The member must have the Putero Premium role or a higher role to receive a penalty here.",
                    "El miembro debe tener el rol Putero Premium o uno superior para recibir una penalización aquí.");
            return;
        }
        if (target.getUser().isBot()) {
            sendMessage(event, "Bots cannot receive penalties.", "Los bots no pueden recibir penalizaciones.");
            return;
        }
        if (target.equals(moderator)) {
            sendMessage(event, "You cannot penalize yourself.", "No puedes penalizarte a ti mismo.");
            return;
        }
        if (!PenaltyAuthorization.usesConfiguredGuildPolicy(event.getGuild())
                && !moderator.canInteract(target)) {
            sendMessage(event,
                    "You cannot penalize a member with an equal or higher role.",
                    "No puedes penalizar a un miembro con un rol igual o superior.");
            return;
        }
        if (!event.getGuild().getSelfMember().canInteract(target)) {
            sendMessage(event,
                    "My role must be higher than the member's role to apply a timeout.",
                    "Mi rol debe estar por encima del rol del miembro para aplicar un timeout.");
            return;
        }

        PenaltyService.Penalty penalty = penaltyService.registerPenalty(
                event.getGuild().getIdLong(),
                target.getIdLong()
        );
        target.timeoutFor(penalty.timeoutDuration())
                .reason(getReason(penalty.infringementCount(), isSpanish(event)))
                .queue(
                        ignored -> sendConfirmation(event, target, penalty),
                        error -> sendMessage(event,
                                "I could not apply the timeout. Check my permissions and role position.",
                                "No he podido aplicar el timeout. Comprueba mis permisos y la posición de mi rol.")
                );
    }

    private void sendConfirmation(MessageReceivedEvent event, Member target, PenaltyService.Penalty penalty) {
        String confirmation;
        if (isSpanish(event)) {
            confirmation = target.getAsMention() + " recibió la penalización "
                    + penalty.infringementCount() + ": timeout de "
                    + formatDuration(penalty.timeoutDuration(), true) + ".";
        } else {
            confirmation = target.getAsMention() + " received penalty "
                    + penalty.infringementCount() + ": timeout for "
                    + formatDuration(penalty.timeoutDuration(), false) + ".";
        }

        if (!usesButanerosPuterosChannels(event)) {
            event.getChannel().sendMessage(confirmation).queue();
            return;
        }

        sendToNamedChannel(event, PENALTY_CHANNEL_NAME, confirmation);
        sendToNamedChannel(event, PENALTY_LOG_CHANNEL_NAME, getPenaltyCountMessage(target, penalty, isSpanish(event)));
    }

    private boolean usesButanerosPuterosChannels(MessageReceivedEvent event) {
        return event.isFromGuild()
                && event.getGuild().getName().equalsIgnoreCase(BUTANEROS_PUTEROS_GUILD_NAME);
    }

    private void sendToNamedChannel(MessageReceivedEvent event, String channelName, String message) {
        List<TextChannel> channels = event.getGuild().getTextChannelsByName(channelName, true);
        if (channels.isEmpty()) {
            System.err.println("Penalty channel not found: " + channelName);
            return;
        }
        channels.getFirst().sendMessage(message).queue();
    }

    private String getPenaltyCountMessage(Member target, PenaltyService.Penalty penalty, boolean spanish) {
        if (spanish) {
            return "Conteo de penalizaciones de " + target.getAsMention() + ": "
                    + penalty.infringementCount() + ".";
        }
        return "Penalty count for " + target.getAsMention() + ": "
                + penalty.infringementCount() + ".";
    }

    private void sendMessage(MessageReceivedEvent event, String englishMessage, String spanishMessage) {
        event.getChannel().sendMessage(isSpanish(event) ? spanishMessage : englishMessage).queue();
    }

    private boolean isSpanish(MessageReceivedEvent event) {
        return event.isFromGuild()
                && languageManager.getLanguage(event.getGuild().getIdLong()) == PenaltyLanguageManager.SPANISH;
    }

    private String getReason(int infringementCount, boolean spanish) {
        return spanish ? "Penalización de miércoles " + infringementCount : "Wednesday penalty " + infringementCount;
    }

    private String formatDuration(Duration duration, boolean spanish) {
        long value;
        String unit;
        if (duration.toDays() > 0) {
            value = duration.toDays();
            unit = spanish ? (value == 1 ? "día" : "días") : "day(s)";
        } else if (duration.toHours() > 0) {
            value = duration.toHours();
            unit = spanish ? (value == 1 ? "hora" : "horas") : "hour(s)";
        } else {
            value = duration.toMinutes();
            unit = spanish ? (value == 1 ? "minuto" : "minutos") : "minute(s)";
        }
        return value + " " + unit;
    }
}
