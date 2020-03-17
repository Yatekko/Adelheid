import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/*
 TODO:  Make !help command, and possibly alternate bot's status between online players and use of help command.
 TODO:  Replace "SQL Error" in branches of code where the server is offline with an offline message.
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
        event.getMember().getUser().openPrivateChannel().queue((channel) -> channel.sendMessage("Welcome to Dawnbreak!  I'm Adelheid, this server's helper bot!  There is an installer located in the `#info` channel.  If you already have the base game installed and updated, you're all set and can skip the installation.  The server's connection info to be used in Ashita or Windower is also in the `#info` channel.\n\nOnce you've made an account and a character, go ahead and type in any Dawnbreak channel `!character add [name]` command, replacing `[name]` with your character's name to link your Discord and account!  If you have any questions, feel free to ask for help in the `#general` channel!").queue());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
    {
        if (event.getGuild().getId().equals("656526482037800970") && !event.getChannel().getId().equals("669344148846936065"))
            Log.info(event.getChannel().getName() + " - " + Objects.requireNonNull(event.getGuild().getMemberById(event.getAuthor().getId())).getEffectiveName() + ": " + event.getMessage().getContentDisplay());

        if (!event.getMessage().getContentDisplay().startsWith("!") || event.getAuthor().isBot()) return;    // Message is from a bot (possibly ourself) or message is not a command.

        // getRawContent() is an atomic getter
        // getContent() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)

        if (event.getMessage().getContentRaw().toLowerCase().startsWith("!ah "))
        {
            try {
                AH ah = new AH(event, DB_URL, USER, PASS);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        if (event.getMessage().getContentRaw().equalsIgnoreCase("!search all") && event.getAuthor().getId().equals("396208002949971972"))  // Display all players online.  Admin use only.
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
                event.getChannel().sendMessage("No players online.").queue();
            }
            else
            {
                event.getChannel().sendMessage("```" + players + "```").queue();
            }
            return;
        }

        if (event.getMessage().getContentRaw().toLowerCase().startsWith("!wiki"))  // Search the FFXI wiki
        {
            try
            {
                Wiki wiki = new Wiki(event);  // Search the FFXI wiki for the term(s) after "!wiki"
            } catch (MalformedURLException e)
            {
                event.getChannel().sendMessage("Invalid search term.").queue();
            } catch (IOException e)
            {
                event.getChannel().sendMessage("Something broke. (IOException)").queue();
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                event.getChannel().sendMessage("No results.").queue();
            }
            return;
        }

        if (event.getMessage().getContentRaw().toLowerCase().startsWith("!character"))  // Get or set character of user or get character of another based on usage.
        {
            try {
                Character character = new Character(event, DB_URL, USER, PASS);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (event.getMessage().getContentDisplay().equals("!test") && event.getAuthor().getId().equals("396208002949971972"))
        {
            StringBuilder message = new StringBuilder("ý\u0002\u0002\b\u0088ý");
            String AT = message.substring(3, message.indexOf("ý", 1));
            //String ATHex = "";
            //for (byte b : AT.getBytes())
            //    ATHex = ATHex.concat(String.format("%02X", b));
            String ATHex = null;
            /*try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("ISO-8859-1")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage(message.toString()).queue();*/

            try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("UTF-16BE")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using UTF-16BE:  " + Integer.parseInt(ATHex, 16)).queue();

            try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("UTF-16LE")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using UTF-16LE:  " + Integer.parseInt(ATHex, 16)).queue();

            try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("UTF-16")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using UTF-16:  " + Integer.parseInt(ATHex, 16)).queue();

            try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("ISO-8859-1")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using ISO-8859-1:  " + Integer.parseInt(ATHex, 16)).queue();

            ATHex = String.format("%040x", new BigInteger(1, AT.getBytes(StandardCharsets.ISO_8859_1)));
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using Standard ISO-8859-1:  " + Integer.parseInt(ATHex, 16)).queue();

            try {
                ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("US-ASCII")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            message.delete(0, 6);
            if (ATHex.length()>4)
                ATHex = ATHex.substring(ATHex.length()-4);
            //message.insert(0, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
            event.getChannel().sendMessage("Using US-ASCII:  " + Integer.parseInt(ATHex, 16)).queue();

            return;
        }


    }
    public String AutoTranslate(int num)  // Takes the decimal version of the 4th and 5th bytes and translates it into the phrase using a search file.
    {
        String autotranslate = "";
        try
        {
            String line;
            Scanner scanner = new Scanner(new File("auto_translates.lua"));
            do
            {
                line = scanner.nextLine();
                if (line.contains("id="+num))
                    break;
            }
            while (scanner.hasNext());
            autotranslate = line.substring(line.indexOf("en=")+4, line.indexOf("ja=")-2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return autotranslate;
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
            Connection con = null;
            Statement st = null;
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
                        assert name != null;
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
                                guild.addRoleToMember(Objects.requireNonNull(guild.getMember(event.getAuthor())), Objects.requireNonNull(guild.getRoleById("668825326738079765"))).queue();
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
