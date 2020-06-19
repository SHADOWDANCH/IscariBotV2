package ua.shadowdan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
public class PropertiesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManager.class);

    private final Properties properties = new Properties();

    protected PropertiesManager() {
        try (InputStream inputStream = IscariBot.class.getResourceAsStream("/config.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Failed to load properties!", e);
        }
    }

    public String getBotToken() {
        return properties.getProperty("bot.token");
    }

    public String getCommandPrefix() {
        return properties.getProperty("command.prefix");
    }

    public String getBotOwnerId() {
        return properties.getProperty("bot.owner.id");
    }

    public String getDatabaseAddress() {
        return properties.getProperty("database.address");
    }

    public String getDatabaseUsername() {
        return properties.getProperty("database.username");
    }

    public String getDatabasePassword() {
        return properties.getProperty("database.password");
    }

    public String getDatabaseName() {
        return properties.getProperty("database.name");
    }

    public String getDatabaseTablePrefix() {
        return properties.getProperty("database.table.prefix");
    }

    public String getFandomBotUser() {
        return properties.getProperty("fandom.bot.user");
    }

    public long getVerifiedRoleId() {
        return Long.parseLong(properties.getProperty("verified.role.id"));
    }
}
