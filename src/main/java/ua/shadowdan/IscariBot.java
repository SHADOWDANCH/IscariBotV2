package ua.shadowdan;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.shadowdan.command.AboutCommand;
import ua.shadowdan.command.PingCommand;
import ua.shadowdan.command.UnVerifyCommand;
import ua.shadowdan.command.VerifyCommand;
import ua.shadowdan.command.WhoIsCommand;
import ua.shadowdan.data.DataStorage;
import ua.shadowdan.data.MysqlDataStorage;
import ua.shadowdan.util.CommandUtil;

import java.util.Objects;

/*
 * Created by SHADOWDAN_ on 29.05.2020 for project 'IscariBotV2'
 */
public class IscariBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(IscariBot.class);

    private static JDA JDA;
    private static PropertiesManager propertiesManager;
    private static DataStorage dataStorage;

    @SneakyThrows
    public static void main(String[] args) {
        propertiesManager = new PropertiesManager();
        dataStorage = new MysqlDataStorage(
                propertiesManager.getDatabaseAddress(),
                propertiesManager.getDatabaseUsername(),
                propertiesManager.getDatabasePassword(),
                propertiesManager.getDatabaseName(),
                propertiesManager.getDatabaseTablePrefix()
        );

        CommandClientBuilder client = new CommandClientBuilder();

        client.setPrefix(propertiesManager.getCommandPrefix());
        client.useDefaultGame();
        client.setEmojis(CommandUtil.SUCCESS, CommandUtil.WARNING, CommandUtil.ERROR);
        client.setOwnerId(propertiesManager.getBotOwnerId());
        client.setHelpConsumer((event) -> {
            CommandClient eventClient = event.getClient();
            StringBuilder builder = new StringBuilder("Список команд **" + event.getSelfUser().getName() + "**:\n");
            Command.Category category = null;
            for (Command command : eventClient.getCommands()) {
                if (command.isHidden()
                        || (command.isOwnerCommand() && !event.isOwner())
                        || (command.getRequiredRole() != null && event.getMember().getRoles().stream()
                        .noneMatch(role -> role.getName().equals(command.getRequiredRole())))) {
                    continue;
                }
                if (!Objects.equals(category, command.getCategory())) {
                    category = command.getCategory();
                    builder.append("\n\n  __")
                            .append(category == null ? "Без категории" : category.getName())
                            .append("__:\n");
                }
                builder.append("\n`")
                        .append(eventClient.getTextualPrefix())
                        .append(eventClient.getPrefix() == null ? " " : "")
                        .append(command.getName())
                        .append(command.getArguments() == null ? "`" : " " + command.getArguments() + "`")
                        .append(" - ")
                        .append(command.getHelp());

            }
            User owner = event.getJDA().getUserById(eventClient.getOwnerId());
            if (owner != null) {
                builder.append("\n\nЧто бы получит дополнительную информацию напишите **")
                        .append(owner.getName())
                        .append("**#")
                        .append(owner.getDiscriminator());
                if (eventClient.getServerInvite() != null) {
                    builder.append(" или зайдите на сервер ").append(eventClient.getServerInvite());
                }
            }
            event.replyInDm(builder.toString(), unused -> {
                if (event.isFromType(ChannelType.TEXT)) {
                    event.reactSuccess();
                }
            }, t -> event.replyWarning("Help cannot be sent because you are blocking Direct Messages."));
        });
        client.addCommands(
                new PingCommand(),
                new AboutCommand(),
                new VerifyCommand(propertiesManager, dataStorage),
                new WhoIsCommand(dataStorage),
                new UnVerifyCommand(propertiesManager, dataStorage)
        );

        JDA = JDABuilder.createDefault(propertiesManager.getBotToken())
                .setActivity(Activity.playing("Loading..."))
                .addEventListeners(client.build())
                .build();

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
