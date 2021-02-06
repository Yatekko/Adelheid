import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.List;
import java.util.Objects;

/*
 TODO:  Make a function to show which nation is in the lead for a given area (e.g. !nation [zone])
 TODO:  Add nation rank in character command, possibly as a non-inline thing between the name and jobs.
*/

public class Events extends ListenerAdapter
{
    private static String DB_URL, USER, PASS;
    private static final Logger Log = LogManager.getLogger("RollingFileLogger");

    public Events(String dbUrl, String user, String pass)
    {
        DB_URL = dbUrl;
        USER = user;
        PASS = pass;
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event)
    {
        event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Welcome to Tantalus!  I'm Adelheid, this server's helper bot!  All you need to get started is in `#server-info!`\n\nOnce you've made an account and a character, go ahead and type in any channel `!character add [name]` command, replacing `[name]` with your character's name to link your Discord and account - for example, if your character name was Adelheid, type `!character add Adelheid`.  If you have any questions, feel free to ask for help in the `#general` channel!").queue());
        Objects.requireNonNull(event.getGuild().getTextChannelById("731564877415579671")).sendMessage("Welcome to Tantalus!  I'm Adelheid, this server's helper bot!  All you need to get started is in <#731568319706038307>\n\nOnce you've made an account and a character, go ahead and type in any channel `!character add [name]` command, replacing `[name]` with your character's name to link your Discord and account - for example, if your character name was Adelheid, type `!character add Adelheid`.  If you have any questions, feel free to ask for help!").queue();
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
    {
        if (event.getGuild().getId().equals("731564877415579668"))
            Log.info(event.getChannel().getName() + " - " + Objects.requireNonNull(event.getGuild().retrieveMemberById(event.getAuthor().getId())).complete().getEffectiveName() + ": " + event.getMessage().getContentDisplay());
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event)
    {
        Log.info("[DM] " + event.getChannel().getName() + " - " + event.getAuthor().getName() + ": " + event.getMessage().getContentDisplay());
        if (event.getAuthor().isBot())
            return;

        MessageHistory history = event.getChannel().getHistory();
        List<Message> retrieved = history.retrievePast(2).complete();
        String lastSent = "";
        try {
            lastSent = retrieved.get(1).getContentDisplay();  // The last message sent by the bot.
        }
        catch (IndexOutOfBoundsException ignored) {}
        
        if (lastSent.equalsIgnoreCase("For verification, what is your Tantalus account name? (case-sensitive)") || lastSent.equalsIgnoreCase("Character and account do not match.  Please try again."))
        {
            Connection con;
            Statement st;
            ResultSet rs;
            String character;
            String charID;
            String accountID;
            String temp;
            String nation;
            try
            {
                con = DriverManager.getConnection(DB_URL,USER,PASS);
                String query = "SELECT * FROM dspdb.chars, dspdb.discord, dspdb.accounts WHERE chars.accid = accounts.id AND discordid = " + event.getAuthor().getId() + " AND login = '" + event.getMessage().getContentDisplay() + "'";
                st = con.createStatement();
                rs = st.executeQuery(query);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL Connection Error.").queue();
                return;
            }
            try
            {
                boolean Verified = false;
                while (rs.next())
                {
                    character = rs.getString("charname");
                    accountID = rs.getString("accid");
                    temp = rs.getString("chartemp");
                    if (character.equalsIgnoreCase(temp))
                    {
                        Guild guild = event.getJDA().getGuildById("731564877415579668");
                        charID = rs.getString("charid");
                        nation = rs.getString("nation");
                        Verified = true;
                        assert guild != null;
                        String name = Objects.requireNonNull(guild.retrieveMemberById(event.getAuthor().getId()).complete()).getEffectiveName();
                        name = name.replaceAll("'", "''");
                        name = name.replaceAll("\"", "\"");
                        String query = "UPDATE discord SET chartemp = null, accid = " + accountID + ", charid = " + charID + ", charname = '" + character + "', dawnbreaknickname = '" + name + "', verified = 1 WHERE discordid = " + event.getAuthor().getId();
                        st = con.createStatement();
                        st.execute(query);
                        event.getChannel().sendMessage("Thank you.  Your account and character are now linked to your Discord ID.").queue();
                        guild.addRoleToMember(guild.retrieveMember(event.getAuthor()).complete(), Objects.requireNonNull(guild.getRoleById("731566814747689051"))).queue();
                        guild.retrieveMember(event.getAuthor()).complete().modifyNickname(character).queue();
                        break;
                    }
                }
                if (!Verified) {
                    event.getChannel().sendMessage("Character and account do not match.  Please try again.").queue();
                }
            }
            catch (SQLException e)  // No character.
            {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL Error.").queue();
            }
        }
    }
}
