package ua.shadowdan.data;

import org.jetbrains.annotations.NotNull;

/*
 * Created by SHADOWDAN_ on 30.05.2020 for project 'IscariBotV2'
 */
public interface DataStorage {

    /**
     * Создать пользователя в базе данных.
     * Если не верифицированый пользователь с совпадающими идентификаторами уже сущетсвует он будет преезаписан
     * Созданный пользователь будет не верефицирован (т.е изменение данных привязки аккаунтов ещё возможно).
     *
     * @param discordId уникальный идентификатор пользователя discord
     * @param fandomId уникальный идентификатор пользователя mediawiki на fandom.com
     * @return результат создания пользователя
     */
    @NotNull
    UserCreationResult createUser(long discordId, long fandomId);

    /**
     * Верефицировать пользователя.
     *
     * @param fandomId уникальный идентификатор пользователя fandom.com
     * @param verificationCode уникальный ключ подтвержения верификации, полученый из {@code DataStorage#createUser(long, long)}
     * @param force верифицировать пользователя игнорируя коректность ключа
     * @return true - ключ совпал, пользователь верифицирован. false - ключ не совпал или произошла ошибка обработки запроса.
     */
    boolean verifyUser(long fandomId, long verificationCode, boolean force);

    /**
     * Полное удаление данных о пользователе из базы данных.
     *
     * @param discordId уникальный идентификатор пользователя discord
     */
    void deleteUser(long discordId);

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

    boolean isCodeVerified(long verificationCode);
}
