package ua.shadowdan;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.util.FandomSimpleAPI;

import java.util.concurrent.TimeUnit;

/*
 * Created by SHADOWDAN_ on 19.06.2020 for project 'IscariBotV2'
 */
public class VerificationThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationThread.class);

    private final JDA jda;
    private final PropertiesManager propertiesManager;
    private final DataStorage dataStorage;

    protected VerificationThread(JDA jda, PropertiesManager propertiesManager, DataStorage dataStorage) {
        this.jda = jda;
        this.propertiesManager = propertiesManager;
        this.dataStorage = dataStorage;
    }

    @Override
    public void run() {
        try {
            while (true) {
                FandomSimpleAPI.getAllMessagesOnWall(propertiesManager.getFandomBotUser()).forEach(wallMessage -> {
                    String messageBody = wallMessage.getBody();

                    LOGGER.debug("Performing scheduled wall posts check");

                    final long verificationCode;
                    try {
                        verificationCode = Long.parseLong(messageBody);
                    } catch (NumberFormatException ex) {
                        return;
                    }

                    if (dataStorage.isCodeVerified(verificationCode)) {
                        return;
                    }

                    String editedBy = wallMessage.getEditedBy();
                    long editorId = FandomSimpleAPI.getUserId(editedBy.substring(editedBy.lastIndexOf(":") + 1));

                    if (dataStorage.verifyUser(editorId, verificationCode, false)) {
                        long discordID = dataStorage.getDiscordIdFromFandom(editorId);
                        if (discordID <= 0) {
                            return;
                        }

                        jda.getGuilds().forEach(guild -> {
                            Role role = guild.getRoleById(propertiesManager.getVerifiedRoleId());
                            if (role == null) {
                                LOGGER.warn("Verification role not exists on guild.");
                                return;
                            }
                            guild.addRoleToMember(discordID, role).complete();
                        });

                        jda.retrieveUserById(discordID).complete()
                                .openPrivateChannel().complete()
                                .sendMessage("Ваш аккаунт fandom.com успешно подтверждён").complete()
                                .addReaction("\u2705").complete();

                        LOGGER.debug("Verified user " + editorId + " with code " + verificationCode);
                    }
                });
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            }
        } catch (InterruptedException ignored) { }
    }
}
