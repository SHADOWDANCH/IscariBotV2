package ua.shadowdan.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * Created by SHADOWDAN_ on 19.06.2020 for project 'IscariBotV2'
 */
@Getter
@RequiredArgsConstructor
public class VerificationUser {

    private final long discordID;
    private final long fandomID;
    private final long verificationCode;
}
