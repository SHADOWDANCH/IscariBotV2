package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.User;
import ua.shadowdan.PropertiesManager;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.data.UserCreationResult;
import ua.shadowdan.util.CommandUtil;
import ua.shadowdan.util.FandomSimpleAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
//
public class VerifyCommand extends Command {

    private final PropertiesManager propertiesManager;
    private final DataStorage dataStorage;

    public VerifyCommand(PropertiesManager propertiesManager, DataStorage dataStorage) {
        this.name = "verify";
        this.help = "привязка аккаунта fandom к аккаунту discord";
        this.guildOnly = false;
        this.arguments = "[ссылка на профиль fandom.com]";

        this.propertiesManager = propertiesManager;
        this.dataStorage = dataStorage;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            CommandUtil.replyHelp(event, this);
            return;
        }
        event.reply("Обработка...", message -> event.async(() -> {
            String[] args = event.getArgs().split(" ");
            String decodedUrl;
            try {
                decodedUrl = URLDecoder.decode(args[0], StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                decodedUrl = "";
            }
            Matcher matcher = FandomSimpleAPI.NICK_PATTERN.matcher(decodedUrl);

            if (decodedUrl.isEmpty() || !matcher.lookingAt()) {
                message.editMessage(CommandUtil.ERROR + " Ссылка профиля имеет неверный формат.\nПример ссылки: `https://terraria.fandom.com/ru/wiki/Участник:SHADOWDAN66`").queue();
                return;
            }

            String username = matcher.replaceAll("");
            long discordId = event.getAuthor().getIdLong();
            long fandomId = FandomSimpleAPI.getUserId(username);
            if (fandomId <= 0) {
                message.editMessage(CommandUtil.ERROR + " Указаный аккаунт не найден").queue();
                return;
            }
            if (dataStorage.isVerified(discordId)) {
                message.editMessage(CommandUtil.SUCCESS + " Ваш аккаунт уже верифицирован.").queue();
                return;
            }
            UserCreationResult result = dataStorage.beginVerification(discordId, fandomId);
            switch (result.getType()) {
                case ALREADY_VERIFIED: {
                    User accountOwner = event.getJDA().retrieveUserById(result.getDiscordId()).complete();
                    message.editMessage(String.format(CommandUtil.ERROR + " Аккаунт %s уже верефицирован пользователем <@%s>", decodedUrl, accountOwner.getIdLong())).queue();
                    return;
                }
                case ERROR: {
                    message.editMessage(CommandUtil.ERROR + " Произошла неизвестная ошибка обработки запроса. Сообщите персоналу или повторите позже.").queue();
                    return;
                }
                case SUCCESS: {
                    message.editMessage(
                            CommandUtil.SUCCESS + " Запрос на верификацию принят.\n"
                                    + "Чтобы подтвердить, что вы являетесь владельцем аккаунта, напишите на странице https://community.fandom.com/wiki/Message_Wall:" + propertiesManager.getFandomUserName() + "\n"
                                    + "Сообщение с текстом: `" + result.getVerificationCode() + "`. Ожидайте. Среднее время ожидания: около 1-й минуты"
                    ).queue();
                    return;
                }
                case AWAITING_VERIFICATION: {
                    message.editMessage(CommandUtil.SUCCESS + " Этот аккаут ожидает подтверждения. Попробуйте через 5 минут").queue();
                    return;
                }
                default: {
                    message.editMessage(CommandUtil.ERROR + " Ошибка обработки запроса: неизвестный результат. Сообщите персоналу").queue();
                }
            }
        }));
    }
}
