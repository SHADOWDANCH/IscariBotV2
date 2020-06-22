package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.util.CommandUtil;
import ua.shadowdan.util.FandomSimpleAPI;

import java.awt.*;
import java.util.List;

/*
 * Created by SHADOWDAN_ on 20.06.2020 for project 'IscariBotV2'
 */
public class WhoIsCommand extends Command {

    private final DataStorage dataStorage;

    public WhoIsCommand(DataStorage dataStorage) {
        this.name = "whois";
        this.help = "Получить информацию о профиле пользователя на fandom.com";
        this.guildOnly = true;
        this.arguments = "[упоминание пользователя]";

        this.dataStorage = dataStorage;
    }

    @Override
    protected void execute(CommandEvent event) {
        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();
        final Member member;
        if (mentionedMembers.size() >= 1) {
            member = mentionedMembers.get(0);
        } else {
            member = event.getMember();
        }
        if (member.getUser().isBot()) {
            event.replyWarning("1110110");
            return;
        }
        event.reply("Обработка...", message -> event.async(() -> {
            long fandomID = dataStorage.getFandomIdFromDiscord(member.getIdLong());
            if (fandomID <= 0) {
                message.editMessage("Этот пользователь ещё не привязал аккаунт fandom.com").queue();
                return;
            }
//            List<FandomSimpleAPI.FandomUserSimple> basicUserInfo = FandomSimpleAPI.getBasicUserInfo(FandomSimpleAPI.getUserName(fandomID));
//            FandomSimpleAPI.FandomUserSimple firstUser = basicUserInfo.get(0);

            message.editMessage(CommandUtil.SUCCESS + " https://community.fandom.com/wiki/User:" + FandomSimpleAPI.getUserName(fandomID)).queue();

/*            MessageEmbed embed = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setAuthor(firstUser.getName(), "https://community.fandom.com/wiki/User:" + firstUser.getName(), firstUser.getAvatarUrl())
                    .addField("Внутренний ид:", "" + firstUser.getUserID(), false)
                    .addField("Кол-во правок:", "" + firstUser.getEditCount(), false)
                    .addField("Дата регистрации:", firstUser.getRegistrationDate(), false)
                    .addField("Гендер:", firstUser.getGender(), false)
                    .build();

            message.editMessage(
                    new MessageBuilder()
                            .setContent(CommandUtil.BLANK)
                            .setEmbed(embed)
                            .build()
            ).queue();
            message.addReaction(CommandUtil.SUCCESS).queue();*/
        }));
    }
}
