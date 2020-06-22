package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.shadowdan.PropertiesManager;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.util.CommandUtil;

import java.util.List;

/*
 * Created by SHADOWDAN_ on 21.06.2020 for project 'IscariBotV2'
 */
public class UnVerifyCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnVerifyCommand.class);

    private final PropertiesManager propertiesManager;
    private final DataStorage dataStorage;

    public UnVerifyCommand(PropertiesManager propertiesManager, DataStorage dataStorage) {
        this.name = "unverify";
        this.arguments = "[упоминание пользователя]";
        this.help = "отменить верификацию пользователя";
        this.guildOnly = true;
        this.requiredRole = propertiesManager.getModeratorRoleName();

        this.propertiesManager = propertiesManager;
        this.dataStorage = dataStorage;
    }

    @Override
    protected void execute(CommandEvent event) {
//        if (event.getMember().getRoles().stream().noneMatch(role -> role.getIdLong() == propertiesManager.getModeratorRoleId())) {
//            event.reply("У вас нету прав на использование этой комманды", message ->
//                    message.addReaction(CommandUtil.ERROR).queue()
//            );
//            return;
//        }
        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();
        if (mentionedMembers.isEmpty()) {
            CommandUtil.replyHelp(event, this);
            return;
        }
        Member member = mentionedMembers.get(0);

        event.reply("Обработка...", message -> event.async(() -> {
            long discordID = member.getIdLong();
            long fandomID = dataStorage.getFandomIdFromDiscord(discordID);
            if (fandomID <= 0) {
                message.editMessage(CommandUtil.ERROR + " Этот пользователь ещё не верифицировал свой аккаунт").queue();
                return;
            }

            dataStorage.cancelVerification(discordID);

            Guild guild = event.getGuild();
            Role role = guild.getRoleById(propertiesManager.getVerifiedRoleId());
            if (role != null) {
                guild.removeRoleFromMember(member, role).queue();
            } else {
                LOGGER.warn("Verification role not exists on guild");
            }

            message.editMessage(CommandUtil.SUCCESS + " Верификация пользователя успешно удалена").queue();
        }));
    }
}
