import Utils.SQL;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.sql.SQLException;

public class SearchAll
{
    private SQL sql;
    private MessageChannel channel;

    public SearchAll(JDA bot, String DB_URL, String USER, String PASS)
    {
        this.sql = new SQL(DB_URL, USER, PASS);
        channel = bot.getTextChannelById("688195963843641443");
    }

    public void Update()
    {
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
            channel.editMessageById("688285697404043346","```No players online.```").queue();
        }
        else
        {
            channel.editMessageById("688285697404043346","```" + players + "```").queue();
        }
    }
}
