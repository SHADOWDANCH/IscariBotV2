package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.User;
import ua.shadowdan.IscariBot;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.data.UserCreationResult;
import ua.shadowdan.util.FandomSimpleAPI;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
//
public class VerifyCommand extends Command {

    private static final String FANDOM_NICKNAME_EXTRACTOR_PATTERN = "https?://(?:[a-zA-Z]{2}\\.)?[a-zA-Z-]+\\.fandom\\.com/wiki/(?:Стена_обсуждения|Участник|User|User_talk):(?:%20)?";

    public VerifyCommand() {
        this.name = "verify";
        this.help = "привязка аккаунта fandom к аккаунту discord";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError("Используйте `" + event.getClient().getPrefix() + this.name + " [ссылка на профиль fandom.com]`");
            return;
        }
        event.async(() -> {
            String[] args = event.getArgs().split(" ");
            String username = args[0].replaceAll(FANDOM_NICKNAME_EXTRACTOR_PATTERN, "");
            DataStorage dataStorage = IscariBot.getDataStorage();
            long discordId = event.getAuthor().getIdLong();
            long fandomId = FandomSimpleAPI.getUserId(username);
            if (dataStorage.isVerified(discordId)) {
                event.replySuccess("Ваш аккаунт уже верифицирован.");
                return;
            }
            UserCreationResult result = dataStorage.createUser(discordId, fandomId);
            switch (result.getType()) {
                case ALREADY_VERIFIED: {
                    User accountOwner = IscariBot.getJDA().retrieveUserById(result.getDiscordId()).complete();
                    event.replyError(String.format("Аккаунт %s уже верефицирован пользователем %s", args[0], accountOwner.getName()));
                    return;
                }
                case ERROR: {
                    event.replyError("Произошла неизвестная ошибка обработки запроса. Сообщите персоналу или повторите позже.");
                    return;
                }
                case SUCCESS:
                case SUCCESS_OVERWRITTEN: {
                    event.replySuccess(
                            "Запрос на верицикацию принят.\n"
                                    + "Что бы подтвердить что вы являетесь владельцем аккаунта напишите на странице https://community.fandom.com/wiki/Message_Wall:" + IscariBot.getPropertiesManager().getFandomBotUser() + "\n"
                                    + "Код: " + result.getVerificationCode()
                    );
                    return;
                }
                default: {
                    event.replyWarning("Ошибка обработки запроса: неизвестный результат. Сообщите персоналу");
                }
            }
        });
    }
}
