package ua.shadowdan.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.shadowdan.IscariBot;
import ua.shadowdan.util.CommandUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*
 * Created by SHADOWDAN_ on 30.05.2020 for project 'IscariBotV2'
 */
public class MysqlDataStorage implements DataStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlDataStorage.class);

    private final String usersTableName;
    private final MysqlDataSource dataSource;
    private Connection mysqlConnection;

    private long lastConnectionCheck;

    private final Cache<Long, VerificationUser> awaitingVerification = Caffeine.newBuilder()
            .removalListener((RemovalListener<Long, VerificationUser>) (fandomID, verificationUser, removalCause) -> {
                if (removalCause == RemovalCause.EXPIRED) {
                    IscariBot.getJDA().retrieveUserById(verificationUser.getDiscordID()).complete()
                            .openPrivateChannel().complete()
                            .sendMessage(CommandUtil.ERROR + " Ожидания подтверждения вашего аккаунта истекло. Заявка отклонена.").complete();
                }
            })
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

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
    public UserCreationResult beginVerification(long discordId, long fandomId) {
        VerificationUser awaitingVerificationUser = awaitingVerification.getIfPresent(fandomId);
        if (awaitingVerificationUser != null) {
            return new UserCreationResult(
                    UserCreationResult.ResultType.AWAITING_VERIFICATION,
                    awaitingVerificationUser.getVerificationCode(),
                    awaitingVerificationUser.getDiscordID(),
                    awaitingVerificationUser.getFandomID());
        }
        final long verifiedFandomId = getFandomIdFromDiscord(discordId);
        final long verifiedDiscordId = getDiscordIdFromFandom(fandomId);
        if (verifiedFandomId > 0 || verifiedDiscordId > 0) {
            return new UserCreationResult(UserCreationResult.ResultType.ALREADY_VERIFIED, -1, verifiedDiscordId, verifiedFandomId);
        }
        final long verificationKey = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        awaitingVerification.put(fandomId, new VerificationUser(discordId, fandomId, verificationKey));

        return new UserCreationResult(UserCreationResult.ResultType.SUCCESS, verificationKey, discordId, fandomId);
    }

    @Override
    public boolean verifyUser(long fandomId, long verificationCode) {
        VerificationUser verificationUser = awaitingVerification.getIfPresent(fandomId);
        if (verificationUser == null || verificationUser.getVerificationCode() != verificationCode) {
            return false;
        }

        try {
            PreparedStatement insert = getConnection().prepareStatement("INSERT INTO " + this.usersTableName + " (discordid, fandomid) VALUES (?, ?);");
            insert.setLong(1, verificationUser.getDiscordID());
            insert.setLong(2, fandomId);
            insert.execute();
        } catch (SQLException exception) {
            LOGGER.warn("Error while processing SQL request", exception);
            return false;
        }

        awaitingVerification.invalidate(fandomId);
        return true;
    }

    @Override
    public void cancelVerification(long discordId) {
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
                    "SELECT fandomid FROM " + this.usersTableName + " WHERE discordid = ?;"
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
                    "SELECT discordid FROM " + this.usersTableName + " WHERE fandomid = ?;"
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
    public boolean isCodeValid(long verificationCode) {
        for (VerificationUser user : awaitingVerification.asMap().values()) {
            if (user.getVerificationCode() == verificationCode) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldCheckWall() {
        awaitingVerification.cleanUp();
        return awaitingVerification.estimatedSize() > 0;
    }
}
