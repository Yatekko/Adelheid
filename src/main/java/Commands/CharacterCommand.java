package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import Utils.Categories;

import java.awt.*;
import java.sql.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Get own character, user's character, or add own character to bot, depending on arguments.
 * Usages:  - !character
 *          - !character [charName]
 *          - !character [@User]
 *          - !character add [charName]
 */

public class CharacterCommand extends Command
{
    private String DB_URL;
    private String USER;
    private String PASS;

    public CharacterCommand(String dbUrl, String user, String pass)
    {
        this.name = "character";
        this.help = "Show your own character, someone else's character (either by their character name or by pinging them), or add your own character to the bot.  Examples:  !character | !character Adelheid | !character @Adelheid | !character add Adelheid";
        this.category = Categories.GENERAL;
        this.DB_URL = dbUrl;
        this.USER = user;
        this.PASS = pass;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if (event.getMessage().getContentRaw().equalsIgnoreCase("!character"))  // Get user's own character.
        {
            Connection con = null;
            Statement st;
            ResultSet rs;
            String character;
            String nation;
            String race;
            String gender = "";
            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String query = "SELECT chars.charname, char_look.race, chars.nation, char_profile.rank_sandoria, char_profile.rank_bastok, char_profile.rank_windurst, job_list.jobcode, char_stats.mlvl, job2_list.jobcode, char_stats.slvl, char_jobs.* FROM chars, char_look, char_profile, char_stats, job_list, job2_list, discord, char_jobs WHERE chars.charid = char_profile.charid AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid AND chars.charid = discord.charid AND chars.charid = char_look.charid AND char_jobs.charid = chars.charid AND discord.discordid = " + event.getAuthor().getId();
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.reply("SQL.SQL Connection Error.");
                assert con != null;
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            try  // Testing for character
            {
                rs.next();
                character = rs.getString("charname");
                nation = rs.getString("chars.nation");

            } catch (SQLException e)  // No character.
            {
                event.reply("No character found.  Use `\"!character add\"` followed by your character's name to set your character.\nExample:  `!character add Yatekko` if your character name was Yatekko.");
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(Objects.requireNonNull(event.getGuild().getMember(event.getAuthor())).getEffectiveName(), null, event.getAuthor().getAvatarUrl());
            embed.setFooter("This character is owned by Discord user " + Objects.requireNonNull(event.getGuild().getMember(event.getAuthor())).getEffectiveName());
            switch (nation) {
                case "0":  // San d'Oria
                    embed.setColor(Color.decode("#ec5a5a"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/2/2f/Ffxi_flg_03l.jpg");
                    break;
                case "1":  // Bastok
                    embed.setColor(Color.decode("#5b80e9"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/0/07/Ffxi_flg_01l.jpg");
                    break;
                case "2":  // Windurst
                    embed.setColor(Color.decode("#b5f75d"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/b/bf/Ffxi_flg_04l.jpg");
                    break;
                default:
                    break;
            }

            try {
                switch (rs.getInt("race")) {
                    case 1:
                        race = "Hume";
                        gender = "male ";
                        break;
                    case 2:
                        race = "Hume";
                        gender = "female ";
                        break;
                    case 3:
                        race = "Elvaan";
                        gender = "male ";
                        break;
                    case 4:
                        race = "Elvaan";
                        gender = "female ";
                        break;
                    case 5:
                        race = "Tarutaru";
                        gender = "male ";
                        break;
                    case 6:
                        race = "Tarutaru";
                        gender = "female ";
                        break;
                    case 7:
                        race = "Mithra";
                        break;
                    case 8:
                        race = "Galka";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + rs.getInt("race"));
                }
                final String[] jobs = {"WAR", "MNK", "WHM", "BLM", "RDM", "THF", "PLD", "DRK", "BST", "RNG", "SAM", "NIN", "DRG", "SMN", "BLU", "COR", "PUP", "DNC", "SCH"};
                embed.setTitle(rs.getString("chars.charname") + " the " + gender + race);
                for (String job : jobs)
                {
                    if (!rs.getString(job.toLowerCase()).equals("0"))
                        embed.addField(job, rs.getString(job.toLowerCase()), true);
                }
                embed.setTitle(character + " the " + gender + race);
            } catch (SQLException e) {
                event.reply("SQL.SQL Error.");
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            event.reply(embed.build());
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if (event.getArgs().split("\\s+")[0].startsWith("<@!"))  // Get tagged user's character.
        {
            Connection con = null;
            Statement st = null;
            ResultSet rs;
            String character;
            Member member = event.getGuild().getMemberById(event.getArgs().split(" ")[0].substring(1).replaceAll("[^0-9]", ""));
            String nation;
            String race;
            String gender = "";
            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String ping = event.getArgs().split(" ")[0].substring(1).replaceAll("[^0-9]", "");
                String query = "SELECT chars.charname, char_look.race, chars.nation, char_profile.rank_sandoria, char_profile.rank_bastok, char_profile.rank_windurst, job_list.jobcode, char_stats.mlvl, job2_list.jobcode, char_stats.slvl, char_jobs.* FROM chars, char_look, char_profile, char_stats, job_list, job2_list, discord, char_jobs WHERE chars.charid = char_profile.charid AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid AND chars.charid = discord.charid AND chars.charid = char_look.charid AND char_jobs.charid = chars.charid AND discord.discordid = " + ping;
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.reply("SQL.SQL Connection Error.");
                return;
            }
            try {
                rs.next();
                character = rs.getString("charname");
                nation = rs.getString("chars.nation");
            } catch (SQLException e)  // No character.
            {
                event.reply("No character found for that user.");
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            assert member != null;
            embed.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
            embed.setFooter("This character is owned by Discord user " + member.getEffectiveName());
            switch (nation) {
                case "0":  // San d'Oria
                    embed.setColor(Color.decode("#ec5a5a"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/2/2f/Ffxi_flg_03l.jpg");
                    break;
                case "1":  // Bastok
                    embed.setColor(Color.decode("#5b80e9"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/0/07/Ffxi_flg_01l.jpg");
                    break;
                case "2":  // Windurst
                    embed.setColor(Color.decode("#b5f75d"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/b/bf/Ffxi_flg_04l.jpg");
                    break;
                default:
                    break;
            }

            try {
                switch (rs.getInt("race")) {
                    case 1:
                        race = "Hume";
                        gender = "male ";
                        break;
                    case 2:
                        race = "Hume";
                        gender = "female ";
                        break;
                    case 3:
                        race = "Elvaan";
                        gender = "male ";
                        break;
                    case 4:
                        race = "Elvaan";
                        gender = "female ";
                        break;
                    case 5:
                        race = "Tarutaru";
                        gender = "male ";
                        break;
                    case 6:
                        race = "Tarutaru";
                        gender = "female ";
                        break;
                    case 7:
                        race = "Mithra";
                        break;
                    case 8:
                        race = "Galka";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + rs.getInt("race"));
                }
                final String[] jobs = {"WAR", "MNK", "WHM", "BLM", "RDM", "THF", "PLD", "DRK", "BST", "RNG", "SAM", "NIN", "DRG", "SMN", "BLU", "COR", "PUP", "DNC", "SCH"};
                embed.setTitle(rs.getString("chars.charname") + " the " + gender + race);
                for (String job : jobs)
                {
                    if (!rs.getString(job.toLowerCase()).equals("0"))
                        embed.addField(job, rs.getString(job.toLowerCase()), true);
                }
                embed.setTitle(character + " the " + gender + race);
            } catch (SQLException e) {
                event.reply("SQL.SQL Error.");
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            event.reply(embed.build());
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        if (!event.getArgs().toLowerCase().split("\\s+")[0].equalsIgnoreCase("add") && !event.getArgs().split("\\s+")[0].startsWith("<@"))  // Get Discord tag of character.
        {
            Connection con;
            Statement st;
            ResultSet rs;
            Member member;
            int nation;
            String race;
            String gender = "";
            String charSearch = event.getArgs().split(" ")[0].replaceAll("[^a-zA-Z]", "");
            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String query = "SELECT discord.discordid, chars.charname, char_look.race, chars.nation, char_profile.rank_sandoria, char_profile.rank_bastok, char_profile.rank_windurst, job_list.jobcode, char_stats.mlvl, job2_list.jobcode, char_stats.slvl, char_jobs.* FROM chars, char_look, char_profile, char_stats, job_list, job2_list, discord, char_jobs WHERE chars.charid = char_profile.charid AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid AND chars.charid = discord.charid AND chars.charid = char_look.charid AND char_jobs.charid = chars.charid AND discord.charname = '" + charSearch + "'";
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL.SQL Error.").queue();
                return;
            }
            try {
                rs.next();
                nation = rs.getInt("chars.nation");
                member = event.getGuild().getMemberById(rs.getString("discordid"));
            } catch (SQLException e)  // No character.
            {
                event.getChannel().sendMessage("No user has claimed that character yet.").queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            assert member != null;
            embed.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
            embed.setFooter("This character is owned by Discord user " + member.getEffectiveName());
            switch (nation) {
                case 0:  // San d'Oria
                    embed.setColor(Color.decode("#ec5a5a"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/2/2f/Ffxi_flg_03l.jpg");
                    break;
                case 1:  // Bastok
                    embed.setColor(Color.decode("#5b80e9"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/0/07/Ffxi_flg_01l.jpg");
                    break;
                case 2:  // Windurst
                    embed.setColor(Color.decode("#b5f75d"));
                    embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/b/bf/Ffxi_flg_04l.jpg");
                    break;
                default:
                    break;
            }

            try {
                switch (rs.getInt("race")) {
                    case 1:
                        race = "Hume";
                        gender = "male ";
                        break;
                    case 2:
                        race = "Hume";
                        gender = "female ";
                        break;
                    case 3:
                        race = "Elvaan";
                        gender = "male ";
                        break;
                    case 4:
                        race = "Elvaan";
                        gender = "female ";
                        break;
                    case 5:
                        race = "Tarutaru";
                        gender = "male ";
                        break;
                    case 6:
                        race = "Tarutaru";
                        gender = "female ";
                        break;
                    case 7:
                        race = "Mithra";
                        break;
                    case 8:
                        race = "Galka";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + rs.getInt("race"));
                }
                final String[] jobs = {"WAR", "MNK", "WHM", "BLM", "RDM", "THF", "PLD", "DRK", "BST", "RNG", "SAM", "NIN", "DRG", "SMN", "BLU", "COR", "PUP", "DNC", "SCH"};
                embed.setTitle(rs.getString("chars.charname") + " the " + gender + race);
                for (String job : jobs)
                {
                    if (!rs.getString(job.toLowerCase()).equals("0"))
                        embed.addField(job, rs.getString(job.toLowerCase()), true);
                }
            } catch (SQLException e) {
                event.reply("SQL.SQL Error.");
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            event.reply(embed.build());
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        if (event.getArgs().toLowerCase().split("\\s+")[0].equalsIgnoreCase("add"))  // Adding the current user's character.
        {
            Connection con = null;
            Statement st;
            ResultSet rs;
            String character;
            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String query = "SELECT * from dspdb.discord, dspdb.chars, dspdb.accounts where discord.accid = accounts.id and chars.charid=discord.charid and discord.discordid = " + event.getAuthor().getId();
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL.SQL Connection Error.").queue();
                try {
                    assert con != null;
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }

            try {
                if (rs.next()) {
                    character = rs.getString("charname");
                    event.getChannel().sendMessage("Character \"" + character + "\" already registered under this Discord ID.  Contact a GM if a change needs to be made.").queue();
                    con.close();
                    return;
                }
                else
                    {
                    try
                    {
                        if (event.getArgs().split("\\s+")[1] != null)
                        {  // (if they actually said what character to add)
                            try  // Check if the character specified exists.
                            {
                                String query = "SELECT * FROM chars WHERE charname = '" + event.getArgs().split("\\s+")[1] + "';";
                                st = con.createStatement();
                                rs = st.executeQuery(query);
                                if (!rs.next())
                                {
                                    event.getChannel().sendMessage("Specified character does not exist.").queue();
                                    return;
                                }
                                try
                                {
                                    String chartemp = event.getArgs().split("\\s+")[1].replaceAll("[^a-zA-Z0-9]", "");
                                    if (chartemp.length() > 20)
                                        chartemp = chartemp.substring(0, 19);
                                    query = "INSERT INTO discord VALUES (" + event.getAuthor().getId() + ", null, null, null, null, '" + chartemp + "', 0)";
                                    st = con.createStatement();
                                    st.execute(query);
                                } catch (SQLIntegrityConstraintViolationException e)
                                {
                                    String chartemp = event.getArgs().split("\\s+")[1].replaceAll("[^a-zA-Z0-9]", "");
                                    if (chartemp.length() > 20)
                                        chartemp = chartemp.substring(0, 19);
                                    query = "UPDATE discord SET chartemp = '" + chartemp + "' WHERE discordid = " + event.getAuthor().getId();
                                    st = con.createStatement();
                                    st.execute(query);
                                } catch (SQLException f)
                                {
                                    f.printStackTrace();
                                }
                            }
                            catch (SQLException e)
                            {
                                event.getChannel().sendMessage("Unexpected SQL.SQL Error.").queue();
                                e.printStackTrace();
                                return;
                            }
                            event.getMessage().delete().queueAfter(15, TimeUnit.SECONDS);
                            event.replyInDm("For verification, what is your Dawnbreak account name? (case-sensitive)");
                            con.close();
                        }
                    } catch (ArrayIndexOutOfBoundsException f)
                    {
                        return;
                    }
                    catch (ErrorResponseException e)
                    {
                        event.reply("I can't send you a private message for the verification process.  Please change your settings or contact a GM for help.");
                        return;
                    }
                    event.reply("We'll talk privately.", (message) -> message.delete().queueAfter(15, TimeUnit.SECONDS));
                }
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
}
