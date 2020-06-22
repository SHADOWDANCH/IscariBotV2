package ua.shadowdan.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import ua.shadowdan.IscariBot;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Created by SHADOWDAN_ on 20.06.2020 for project 'IscariBotV2'
 */
public class AboutCommand extends Command {

    private static final SimpleDateFormat UPTIME_FORMAT = new SimpleDateFormat("HH часов mm минут ss секунд");

    public AboutCommand() {
        this.name = "about";
        this.help = "информация о боте";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        final User selfUser = event.getSelfUser();

        final String uptime = UPTIME_FORMAT.format(new Date(ManagementFactory.getRuntimeMXBean().getUptime() + 10098000000L)); // LOL IDK
        final String version = IscariBot.class.getPackage().getImplementationVersion();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .setAuthor(selfUser.getName(), null, selfUser.getEffectiveAvatarUrl())
                .addField("Версия:", version == null ? "undefined" : version, false)
                .addField("Время работы:", uptime, false)
                .addField("Автор:", "SHADOWDAN#0252", false)
                .setFooter("Powered by JDA")
                .build();

        event.reply(embed);
    }
}
