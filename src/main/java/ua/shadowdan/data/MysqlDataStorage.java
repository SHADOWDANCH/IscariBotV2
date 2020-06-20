package ua.shadowdan.data;

import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Created by SHADOWDAN_ on 30.05.2020 for project 'IscariBotV2'
 */
public class MysqlDataStorage implements DataStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDataStorage.class);

    private final String usersTableName;
    private final MysqlDataSource dataSource;
    private Connection mysqlConnection;

    private long lastConnectionCheck;

    @SneakyThrows
    public MysqlDataStorage(String serverAddress, String user, String password, String databaseName, String tablePrefix) {
        dataSource = new MysqlDataSource();
        dataSource.setServerName(serverAddress);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setDatabaseName(databaseName);
        dataSource.setServerTimezone("UTC");
        this.usersTableName = tablePrefix + "users";

        try {
            getConnection().createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS " + this.usersTableName
                            + " (discordid BIGINT NOT NULL, fandomid BIGINT NOT NULL, "
                            + "verified BOOLEAN NOT NULL DEFAULT FALSE, code BIGINT NOT NULL, "
                            + "UNIQUE(discordid), UNIQUE(fandomid));");
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create tables.", ex);
        }
    }


    @SneakyThrows(SQLException.class)
    private Connection getConnection() {
        long current = System.currentTimeMillis();
        if (mysqlConnection == null || (current - lastConnectionCheck >= 300_000L && !mysqlConnection.isValid(1))) {
            LOGGER.warn("Database connection invalid! Reconnecting...");
            mysqlConnection = dataSource.getConnection();
            this.lastConnectionCheck = current;
        }
        return mysqlConnection;
    }

    @NotNull
    @Override
    public UserCreationResult createUser(long discordId, long fandomId) {
        final long verifiedFandomId = getFandomIdFromDiscord(discordId);
        final long verifiedDiscordId = getDiscordIdFromFandom(fandomId);
        if (verifiedFandomId > 0 || verifiedDiscordId > 0) {
            return new UserCreationResult(UserCreationResult.ResultType.ALREADY_VERIFIED, -1, verifiedDiscordId, verifiedFandomId);
        }
        final long verificationKey = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        try {
            PreparedStatement insert = getConnection().prepareStatement(
                    "INSERT INTO " + this.usersTableName + " (discordid, fandomid, verified, code) "
                            + "VALUES (?, ?, false, ?) "
                            + "ON DUPLICATE KEY UPDATE "
                            + "discordid = CASE WHEN verified = 0 THEN VALUES(discordid) ELSE discordid END, "
                            + "fandomid = CASE WHEN verified = 0 THEN VALUES(fandomid) ELSE fandomid END, "
                            + "code = CASE WHEN verified = 0 THEN VALUES(code) ELSE code END;"
            );
            insert.setLong(1, discordId);
            insert.setLong(2, fandomId);
            insert.setLong(3, verificationKey);

            int result = insert.executeUpdate();
            if (result == 2) { // update
                return new UserCreationResult(UserCreationResult.ResultType.SUCCESS_OVERWRITTEN, verificationKey, discordId, fandomId);
            } else if (result == 1) { // insert or insert without modification
                return new UserCreationResult(UserCreationResult.ResultType.SUCCESS, verificationKey, discordId, fandomId);
            }
        } catch (SQLException exception) {
            LOGGER.warn("Error while creating user.", exception);
        }
        return new UserCreationResult(UserCreationResult.ResultType.ERROR, -1, -1, -1);
    }

    @Override
    public boolean verifyUser(long fandomId, long verificationCode, boolean force) {
        try {
            long result = 0;
            if (!force) {
                PreparedStatement select = getConnection().prepareStatement(
                        "SELECT code FROM " + this.usersTableName + " WHERE fandomId = ?;"
                );
                select.setLong(1, fandomId);
                ResultSet resultSet = select.executeQuery();
                result = !resultSet.next() ? 0 : resultSet.getLong("code");
            }
            if (force || result == verificationCode) {
                PreparedStatement update = getConnection().prepareStatement(
                        "UPDATE " + this.usersTableName + " SET verified = true WHERE fandomId = ? AND code = ?;"
                );
                update.setLong(1, fandomId);
                update.setLong(2, verificationCode);
                return update.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            LOGGER.warn("Error while verifying user", exception);
        }
        return false;
    }

    @Override
    public void deleteUser(long discordId) {
        try {
            PreparedStatement delete = getConnection().prepareStatement(
                    "DELETE FROM " + this.usersTableName + " WHERE discordid = ?;"
            );
            delete.setLong(1, discordId);
            delete.execute();
        } catch (SQLException exception) {
            LOGGER.warn("Error while deleting user.", exception);
        }
    }

    @Override
    public long getFandomIdFromDiscord(long discordId) {
        try {
            PreparedStatement select = getConnection().prepareStatement(
                    "SELECT fandomid FROM " + this.usersTableName + " WHERE discordid = ? AND verified = 1;"
            );
            select.setLong(1, discordId);
            ResultSet resultSet = select.executeQuery();
            long fandomId = resultSet.next() ? resultSet.getLong("fandomid") : 0;
            return fandomId <= 0 ? -1 : fandomId;
        } catch (SQLException exception) {
            LOGGER.warn("Error retrieving fandomid.", exception);
        }
        return -1;
    }

    @Override
    public long getDiscordIdFromFandom(long fandomId) {
        try {
            PreparedStatement select = getConnection().prepareStatement(
                    "SELECT discordid FROM " + this.usersTableName + " WHERE fandomid = ? AND verified = 1;"
            );
            select.setLong(1, fandomId);
            ResultSet resultSet = select.executeQuery();
            long discordId = resultSet.next() ? resultSet.getLong("discordid") : 0;
            return discordId <= 0 ? -1 : discordId;
        } catch (SQLException exception) {
            LOGGER.warn("Error retrieving discordid.", exception);
        }
        return -1;
    }

    @Override
    public boolean isCodeVerified(long verificationCode) {
        try {
            PreparedStatement select = getConnection().prepareStatement(
                    "SELECT verified FROM " + this.usersTableName
                            + " WHERE code = ?;"
            );
            select.setLong(1, verificationCode);
            ResultSet resultSet = select.executeQuery();
            return resultSet.next() && resultSet.getBoolean("verified");
        } catch (SQLException exception) {
            LOGGER.warn("Error checking code verification.", exception);
        }
        return false;
    }
}
