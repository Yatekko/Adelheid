package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.Paginator;
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
    private Member member;

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
            Statement st, st2;
            ResultSet rs, rs2;
            member = event.getMember();

            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String query = "SELECT chars.charname, char_look.race, chars.nation, char_profile.rank_sandoria, char_profile.rank_bastok, char_profile.rank_windurst, job_list.jobcode, char_stats.mlvl, job2_list.jobcode, char_stats.slvl, char_jobs.* FROM chars, char_look, char_profile, char_stats, job_list, job2_list, discord, char_jobs WHERE chars.charid = char_profile.charid AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid AND chars.charid = discord.charid AND chars.charid = char_look.charid AND char_jobs.charid = chars.charid AND discord.discordid = " + event.getAuthor().getId();
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.reply("SQL Connection Error.");
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
            try
            {
                String query = "SELECT chars.charid 'ID', chars.charname 'Character', craft_names.name 'Skill', char_skills.value / 10 'Value' FROM char_skills, chars, craft_names, discord WHERE char_skills.skillid = craft_names.skillid AND chars.charid = char_skills.charid AND chars.charid = discord.charid AND discord.discordid = " + event.getAuthor().getId();
                st2 = con.createStatement();
                rs2 = st2.executeQuery(query);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                event.replyError("SQL Connection Error.");
                try
                {
                    con.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                }
                return;
            }

            EmbedBuilder[] embed = null;
            try
            {
                embed = getCharacterBlurb(rs, rs2);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            assert embed != null;
            event.reply(embed[0].build());
            if (!embed[1].getFields().isEmpty())
                event.reply(embed[1].build());

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if (event.getArgs().split("\\s+")[0].startsWith("<@!"))  // Get tagged user's character.
        {
            Connection con;
            Statement st, st2;
            ResultSet rs, rs2;
            String ping;
            member = event.getGuild().getMemberById(event.getArgs().split(" ")[0].substring(1).replaceAll("[^0-9]", ""));

            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                ping = event.getArgs().split(" ")[0].substring(1).replaceAll("[^0-9]", "");
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
            } catch (SQLException e)  // No character.
            {
                event.reply("No character found for that user.");
                return;
            }

            try
            {
                String query = "SELECT chars.charid 'ID', chars.charname 'Character', craft_names.name 'Skill', char_skills.value / 10 'Value' FROM char_skills, chars, craft_names, discord WHERE char_skills.skillid = craft_names.skillid AND chars.charid = char_skills.charid AND chars.charid = discord.charid AND discord.discordid = " + ping;
                st2 = con.createStatement();
                rs2 = st2.executeQuery(query);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                event.replyError("SQL Connection Error.");
                try
                {
                    con.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                }
                return;
            }

            EmbedBuilder[] embed = null;
            try
            {
                embed = getCharacterBlurb(rs, rs2);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            assert embed != null;
            event.reply(embed[0].build());
            if (!embed[1].getFields().isEmpty())
                event.reply(embed[1].build());
            return;
        }

        if (!event.getArgs().toLowerCase().split("\\s+")[0].equalsIgnoreCase("add") && !event.getArgs().split("\\s+")[0].startsWith("<@"))  // Get Discord tag of character.
        {
            Connection con;
            Statement st, st2;
            ResultSet rs, rs2;
            String charSearch = event.getArgs().split(" ")[0].replaceAll("[^a-zA-Z]", "");
            try {
                con = DriverManager.getConnection(DB_URL, USER, PASS);
                String query = "SELECT discord.discordid, chars.charname, char_look.race, chars.nation, char_profile.rank_sandoria, char_profile.rank_bastok, char_profile.rank_windurst, job_list.jobcode, char_stats.mlvl, job2_list.jobcode, char_stats.slvl, char_jobs.* FROM chars, char_look, char_profile, char_stats, job_list, job2_list, discord, char_jobs WHERE chars.charid = char_profile.charid AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid AND chars.charid = discord.charid AND chars.charid = char_look.charid AND char_jobs.charid = chars.charid AND discord.charname = '" + charSearch + "'";
                st = con.createStatement();
                rs = st.executeQuery(query);
            } catch (SQLException e) {
                e.printStackTrace();
                event.getChannel().sendMessage("SQL Error.").queue();
                return;
            }
            try {
                rs.next();
                member = event.getGuild().getMemberById(rs.getString("discordid"));
            } catch (SQLException e)  // No character.
            {
                event.getChannel().sendMessage("No user has claimed that character yet.").queue();
                return;
            }

            try
            {
                String query = "SELECT chars.charid 'ID', chars.charname 'Character', craft_names.name 'Skill', char_skills.value / 10 'Value' FROM char_skills, chars, craft_names, discord WHERE char_skills.skillid = craft_names.skillid AND chars.charid = char_skills.charid AND chars.charid = discord.charid AND discord.charname = '" + charSearch + "'";
                st2 = con.createStatement();
                rs2 = st2.executeQuery(query);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                event.replyError("SQL Connection Error.");
                try
                {
                    con.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                }
                return;
            }

            EmbedBuilder[] embed = null;
            try
            {
                embed = getCharacterBlurb(rs, rs2);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            assert embed != null;
            event.reply(embed[0].build());
            if (!embed[1].getFields().isEmpty())
                event.reply(embed[1].build());
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

    private EmbedBuilder[] getCharacterBlurb(ResultSet rs, ResultSet rs2) throws SQLException
    {
        String character = rs.getString("charname");
        String nation = rs.getString("chars.nation");
        String race = "";
        String gender = "";
        Color color = null;
        String thumbnail = "";
        EmbedBuilder embed = new EmbedBuilder();

        assert member != null;
        embed.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
        embed.setFooter("This character is owned by Discord user " + member.getEffectiveName());
        switch (nation) {
            case "0":  // San d'Oria
                embed.setColor(Color.decode("#ec5a5a"));
                color = Color.decode("#ec5a5a");
                embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/2/2f/Ffxi_flg_03l.jpg");
                thumbnail = "https://vignette.wikia.nocookie.net/ffxi/images/2/2f/Ffxi_flg_03l.jpg";
                break;
            case "1":  // Bastok
                embed.setColor(Color.decode("#5b80e9"));
                color = Color.decode("#5b80e9");
                embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/0/07/Ffxi_flg_01l.jpg");
                thumbnail = "https://vignette.wikia.nocookie.net/ffxi/images/0/07/Ffxi_flg_01l.jpg";
                break;
            case "2":  // Windurst
                embed.setColor(Color.decode("#b5f75d"));
                color = Color.decode("#b5f75d");
                embed.setThumbnail("https://vignette.wikia.nocookie.net/ffxi/images/b/bf/Ffxi_flg_04l.jpg");
                thumbnail = "https://vignette.wikia.nocookie.net/ffxi/images/b/bf/Ffxi_flg_04l.jpg";
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
            for (String job : jobs)
            {
                if (!rs.getString(job.toLowerCase()).equals("0"))
                    embed.addField(job, rs.getString(job.toLowerCase()), true);
            }
            embed.setTitle(character + " the " + gender + race + "\n__**Jobs**__");
        } catch (SQLException e)
        {
           e.printStackTrace();
        }

        EmbedBuilder embed2 = new EmbedBuilder();
        embed2.setAuthor(member.getEffectiveName(), null, member.getUser().getAvatarUrl());
        embed2.setFooter("This character is owned by Discord user " + member.getEffectiveName());
        embed2.setTitle(rs.getString("chars.charname") + " the " + gender + race + "\n__**Crafts**__");
        embed2.setColor(color);
        embed2.setThumbnail(thumbnail);
        while(rs2.next())
            embed2.addField(capitalize(rs2.getString("Skill")), String.valueOf(rs2.getInt("Value")), true);

        return new EmbedBuilder[]{embed, embed2};
    }

    public static String capitalize(String str)
    {
        if(str == null) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
