// https://discordapp.com/api/oauth2/authorize?client_id=661731281037688863&scope=bot&permissions=8

import Commands.*;
import Utils.SQL;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Adelheid extends ListenerAdapter
{
    private static String DB_URL;
    private static String USER;
    private static String PASS;
    private static EventWaiter waiter;

    public static void main(String[] args) throws LoginException
    {
        Config conf = ConfigFactory.parseFile(new File("Adelheid.conf"));
        DB_URL = conf.getString("Adelheid.dburl");
        USER = conf.getString("Adelheid.user");
        PASS = conf.getString("Adelheid.pass");
        waiter = new EventWaiter();

        CommandClientBuilder builder = new CommandClientBuilder()
                .setPrefix("!")
                .setOwnerId("396208002949971972")
                .setActivity(Activity.playing("loading..."))
                .addCommands(
                        // Owner
                        new SearchAllCommand(DB_URL, USER, PASS),
                        new TestCommand(waiter, DB_URL, USER, PASS),

                        // General
                        new AHCommand(waiter, DB_URL, USER, PASS),
                        new CharacterCommand(DB_URL, USER, PASS),
                        new PingCommand(),
                        new WikiCommand()
                );
        CommandClient client = builder.build();

        JDABuilder botbuilder = new JDABuilder(AccountType.BOT)
                .setToken(conf.getString("Adelheid.token"))
                .addEventListeners(client, waiter);
        JDA Bot = botbuilder.build();
        Guild guild = Bot.getGuildById(conf.getLong("Adelheid.guild"));
        RegisterListeners(Bot);

        assert guild != null;
        Linkshell LS = new Linkshell(Bot, DB_URL, USER, PASS);
        SearchAll Search = new SearchAll(Bot, DB_URL, USER, PASS);

        ScheduledExecutorService Scheduler = Executors.newScheduledThreadPool(3);

        ////////////////////////////////////
        //  Print LS chat to #linkshell   //
        ////////////////////////////////////
        Scheduler.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                LS.LStoDiscord();
            }
        }, 5, 3, TimeUnit.SECONDS);

        ////////////////////////////////////
        //        Update Bot Status       //
        ////////////////////////////////////
        Scheduler.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                int count = 0;
                try
                {
                    count = UpdatePlayerCount(count);
                } catch (SQLException e)
                {
                    e.printStackTrace();
                }
                if (count != -1)
                    Bot.getPresence().setPresence(Activity.watching(count + " player(s) online"), false);
                else
                    Bot.getPresence().setPresence(Activity.watching("Error retrieving player count"), false);
            }
        }, 5, 10, TimeUnit.SECONDS);

        ///////////////////////////////////
        //     Update Active Players     //
        ///////////////////////////////////
        Scheduler.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                guild.getMembers().forEach(member ->
                {
                    try
                    {
                        SQL.UpdateActivePlayer(member);
                    }
                    catch (SQLException ignored) {}
                });
                System.out.println("Finished updating active players.");
            }
        }, getHoursUntilTarget(6), 24, TimeUnit.HOURS);

        //////////////////////////////////
        //      Update #whos-online     //
        //////////////////////////////////
        Scheduler.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                Search.Update();
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    private static void RegisterListeners(JDA Bot)
    {
        Bot.addEventListener(new Events(DB_URL, USER, PASS));
    }
    private static int UpdatePlayerCount(int count) throws SQLException
    {
        SQL sql = new SQL(DB_URL, USER, PASS);
        count = sql.countPlayers(count);
        return count;
    }
    private static int getHoursUntilTarget(int targetHour)
    {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour < targetHour ? targetHour - hour : targetHour - hour + 24;
    }
}