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
        event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Welcome to Dawnbreak!  I'm Adelheid, this server's helper bot!  There is an installer located in the `#info` channel.  If you already have the base game installed and updated, you're all set and can skip the installation.  The server's connection info to be used in Ashita or Windower is also in the `#info` channel.\n\nOnce you've made an account and a character, go ahead and type in any Dawnbreak channel `!character add [name]` command, replacing `[name]` with your character's name to link your Discord and account - for example, if your character name was Adelheid, type `!character add Adelheid`.  If you have any questions, feel free to ask for help in the `#general` channel!").queue());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
    {
        if (event.getGuild().getId().equals("656526482037800970") && !event.getChannel().getId().equals("669344148846936065"))
            Log.info(event.getChannel().getName() + " - " + Objects.requireNonNull(event.getGuild().getMemberById(event.getAuthor().getId())).getEffectiveName() + ": " + event.getMessage().getContentDisplay());
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
        
        if (lastSent.equalsIgnoreCase("For verification, what is your Dawnbreak account name? (case-sensitive)") || lastSent.equalsIgnoreCase("Character and account do not match.  Please try again."))
        {
            Connection con;
            Statement st;
            ResultSet rs;
            String character;
            String charID;
            String accountID = "";
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
                        Guild guild = event.getJDA().getGuildById("656526482037800970");
                        charID = rs.getString("charid");
                        nation = rs.getString("nation");
                        Verified = true;
                        assert guild != null;
                        String name = Objects.requireNonNull(guild.getMemberById(event.getAuthor().getId())).getEffectiveName();
                        name = name.replaceAll("'", "''");
                        name = name.replaceAll("\"", "\"");
                        String query = "UPDATE discord SET chartemp = null, accid = " + accountID + ", charid = " + charID + ", charname = '" + character + "', dawnbreaknickname = '" + name + "', verified = 1 WHERE discordid = " + event.getAuthor().getId();
                        st = con.createStatement();
                        st.execute(query);
                        event.getChannel().sendMessage("Thank you.  Your account and character are now linked to your Discord ID.").queue();
                        guild.removeRoleFromMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825325874053131"))).queue();  // Bastok
                        guild.removeRoleFromMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825325559611416"))).queue();  // San d'Oria
                        guild.removeRoleFromMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825326738079765"))).queue();  // Windurst
                        switch (nation)
                        {
                            case "0":
                                guild.addRoleToMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825325559611416"))).queue();
                                event.getChannel().sendMessage("You have been assigned the San d'Oria role.").queue();
                                break;
                            case "1":
                                guild.addRoleToMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825325874053131"))).queue();
                                event.getChannel().sendMessage("You have been assigned the Bastok role.").queue();
                                break;
                            case "2":
                                guild.addRoleToMember((Objects.requireNonNull(guild.getMember(event.getAuthor()))), Objects.requireNonNull(guild.getRoleById("668825326738079765"))).queue();
                                event.getChannel().sendMessage("You have been assigned the Windurst role.").queue();
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
                if (!Verified) {
                    event.getChannel().sendMessage("Character and account do not match.  Please try again.").queue();
                    return;
                }
            }
            catch (SQLException e)  // No character.
            {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL Error.").queue();
                return;
            }
        }
    }
}
