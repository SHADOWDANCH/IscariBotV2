package ua.shadowdan.data;

import org.jetbrains.annotations.NotNull;

/*
 * Created by SHADOWDAN_ on 30.05.2020 for project 'IscariBotV2'
 */
public interface DataStorage {

    /**
     * Поопытаться начать процесс верификации пользователя
     *
     * @param discordId уникальный идентификатор пользователя discord
     * @param fandomId уникальный идентификатор пользователя mediawiki на fandom.com
     * @return результат попытки
     */
    @NotNull
    UserCreationResult beginVerification(long discordId, long fandomId);

    /**
     * Верефицировать пользователя.
     *
     * @param fandomId уникальный идентификатор пользователя fandom.com
     * @param verificationCode уникальный ключ подтвержения верификации
     * @return true - ключ совпал, пользователь верифицирован. false - ключ не совпал или произошла ошибка обработки запроса.
     */
    boolean verifyUser(long fandomId, long verificationCode);

    /**
     * Отмена верификации пользователя
     *
     * @param discordId уникальный идентификатор пользователя discord
     */
    void cancelVerification(long discordId);

    /**
     * Получить идентификатор пользователя fandom.com привязаный к discord
     *
     * @param discordId уникальный идентификатор пользователя discord
     * @return привязаный к этому discordId идентификатор пользователя fandom.com или -1 если пользователь не верефицирован.
     */
    long getFandomIdFromDiscord(long discordId);

    /**
     * Получить идентификатор пользователя discord привязаный к fandom.com
     *
     * @param fandomId уникальный идентификатор пользователя fandom.com
     * @return привязаный к этому fandomId идентификатор пользователя discord или -1 если пользователь не верефицирован.
     */
    long getDiscordIdFromFandom(long fandomId);

    /**
     * Получить статус верификации пользователя discord
     *
     * @param discordId уникальный идентификатор пользователя discord
     * @return true - пользователь верифицирован. false - пользователь не верифицирован или произошла ошибка обработки запроса.
     */
    default boolean isVerified(long discordId) {
        return getFandomIdFromDiscord(discordId) > 0;
    }

    /**
     * Проверить является ли код верификации валидным (т.е ожидает подтверждения)
     *
     * @param verificationCode ключ подтверждения верификации
     * @return результат проверки
     */
    boolean isCodeValid(long verificationCode);

    /**
     * Нужно ли в данный момент проверять стену сообщений
     *
     * @return результат
     */
    boolean shouldCheckWall();
}
