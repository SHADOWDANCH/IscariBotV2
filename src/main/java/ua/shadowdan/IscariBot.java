package ua.shadowdan;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.shadowdan.command.PingCommand;
import ua.shadowdan.command.VerifyCommand;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.data.MysqlDataStorage;

import javax.security.auth.login.LoginException;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
public class IscariBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(IscariBot.class);

    private static JDA JDA;
    private static final PropertiesManager propertiesManager = new PropertiesManager();
    private static DataStorage dataStorage;

    public static void main(String[] args) throws LoginException {
        CommandClientBuilder client = new CommandClientBuilder();

        client.setPrefix(propertiesManager.getCommandPrefix());
        client.useDefaultGame();
        client.setEmojis("\u2705", "\u26A0\uFE0F", "\u26D4");
        client.setOwnerId(propertiesManager.getBotOwnerId());
        client.addCommands(
                new PingCommand(),
                new VerifyCommand()
        );

        JDA = JDABuilder.createDefault(propertiesManager.getBotToken())
                .setActivity(Activity.playing("Loading..."))
                .addEventListeners(client.build())
                .build();
        dataStorage = new MysqlDataStorage(
                propertiesManager.getDatabaseAddress(),
                propertiesManager.getDatabaseUsername(),
                propertiesManager.getDatabasePassword(),
                propertiesManager.getDatabaseName(),
                propertiesManager.getDatabaseTablePrefix()
        );

        LOGGER.info("Logged in as '" + JDA.getSelfUser().getName() + "'");

        new VerificationThread(JDA, propertiesManager, dataStorage).start();
    }

    public static JDA getJDA() {
        return JDA;
    }

    public static PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }

    public static DataStorage getDataStorage() {
        return dataStorage;
    }
}
