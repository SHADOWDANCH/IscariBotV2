package ua.shadowdan.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * Created by SHADOWDAN_ on 30.05.2020 for project 'IscariBotV2'
 */
@Getter
@RequiredArgsConstructor
public class UserCreationResult {

    /* Тип результата */
    private final ResultType type;
    /* Код верификации. -1 если тип результата ALREADY_VERIFIED или ERROR */
    private final long verificationCode;
    /* Дискорд ид новго пользователя. -1 если тип результата ERROR.
       Если типа результата ALREADY_VERIFIED ид пользователя которому уже привязан аккаунт */
    private final long discordId;
    /* fandom.com ид новго пользователя. -1 если тип результата ERROR.
       Если типа результата ALREADY_VERIFIED ид пользователя которому уже привязан аккаунт */
    private final long fandomId;

    public enum ResultType {
        SUCCESS,
        ALREADY_VERIFIED,
        SUCCESS_OVERWRITTEN,
        ERROR
    }
}
