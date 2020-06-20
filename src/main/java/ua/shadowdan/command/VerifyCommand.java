package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.User;
import ua.shadowdan.IscariBot;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.data.UserCreationResult;
import ua.shadowdan.util.FandomSimpleAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
//
public class VerifyCommand extends Command {

    private static final Pattern NICK_PATTERN = Pattern.compile("https?://(?:[a-zA-Z]{2}\\.)?[a-zA-Z-]+\\.fandom\\.com(?:/[a-z]{2,3})?/wiki/(?:Стена_обсуждения|Участник|User|User_talk):(?:%20)?");

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
            String decodedUrl;
            try {
                decodedUrl = URLDecoder.decode(args[0], StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                decodedUrl = null;
            }
            if (decodedUrl == null || !NICK_PATTERN.matcher(decodedUrl).lookingAt()) {
                event.replyError("Ссылка профиля имеет неверный формат");
                return;
            }
            String username = decodedUrl.replaceAll(NICK_PATTERN.pattern(), "");
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
                    event.replyError(String.format("Аккаунт %s уже верефицирован пользователем %s", decodedUrl, accountOwner.getName()));
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
                                    + "Сообщение с текстом: `" + result.getVerificationCode() + "`. И ожидайте. Среднее время ожидания: около 1й минуты"
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
