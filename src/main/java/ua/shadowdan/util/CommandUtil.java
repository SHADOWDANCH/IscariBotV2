package ua.shadowdan.util;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

/*
 * Created by SHADOWDAN_ on 21.06.2020 for project 'IscariBotV2'
 */
public class CommandUtil {

    public static final String SUCCESS = "\u2705";
    public static final String WARNING = "\u26A0\uFE0F";
    public static final String ERROR = "\u26D4";
    public static final String BLANK = "\u17B5";

    private CommandUtil() { }

    public static void replyHelp(CommandEvent event, Command command) {
        event.reply(WARNING + " Используйте: `" + event.getClient().getPrefix() + command.getName() + " " + command.getArguments() + "`");
    }
}
