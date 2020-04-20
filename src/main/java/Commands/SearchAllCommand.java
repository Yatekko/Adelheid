package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import Utils.SQL;

import java.sql.SQLException;

/**
 * Display characters online, levels, and location.  Owner-only command.
 * Usage:  !search
 */

public class SearchAllCommand extends Command
{
    private String DB_URL;
    private String USER;
    private String PASS;

    public SearchAllCommand(String dbUrl, String user, String pass)
    {
        this.name = "search";
        this.ownerCommand = true;
        this.DB_URL = dbUrl;
        this.USER = user;
        this.PASS = pass;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        SQL sql = new SQL(DB_URL, USER, PASS);
        String players = "";

        try
        {
            players = sql.searchPlayers();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        if (players.equals("*-----------*-------------------*------------------------------*\n|    Job    |     Character     |           Location           |\n*-----------*-------------------*------------------------------*\n*-----------*-------------------*------------------------------*"))
        {
            event.reply("No players online.");
        }
        else
        {
            event.reply("```" + players + "```");
        }
    }
}
