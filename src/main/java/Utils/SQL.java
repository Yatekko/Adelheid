package Utils;

import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SQL extends ListenerAdapter
{
    private static String DB_URL, USER, PASS;


    public SQL(String url, String user, String pass)
    {
        DB_URL = url;
        USER = user;
        PASS = pass;
    }

    public int countPlayers(int count) throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs;
        try {
            con = DriverManager.getConnection(DB_URL, USER, PASS);
            String query = "SELECT chars.charname, zone_settings.name FROM accounts_sessions, chars, zone_settings WHERE accounts_sessions.charid = chars.charid AND zone_settings.zoneid = chars.pos_zone order by chars.charname;";
            st = con.createStatement();
            rs = st.executeQuery(query);
            while (rs.next())
                count++;
            con.close();
            return count;
        }
        catch (SQLNonTransientConnectionException e)
        {
            if (e.getLocalizedMessage().equals("Too many connections")) {
                return -1;
            }
        }
        catch (CJCommunicationsException | CommunicationsException e)
        {
            System.out.println(LocalDateTime.now() + ":  Connection to server failed.  Server probably offline.");
            return -1;
        } catch (SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
            return -1;
        } catch (Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }
        finally {
            //finally block used to close resources
            try {
                if (st != null)
                    st.close();
            } catch (SQLException ignored) {
            }// nothing we can do
            try {
                if (con != null)
                    con.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }//end finally try
        }//end try
        return count;
    }

    public String searchPlayers() throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs;
        try {
            con = DriverManager.getConnection(DB_URL, USER, PASS);
            String query = "SELECT chars.charname \"character\", zone_settings.name \"location\", job_list.jobcode \"job\", char_stats.mlvl \"level\", job2_list.jobcode \"subjob\", char_stats.slvl \"sublevel\", chars.gmlevel \"gm\" FROM accounts_sessions, chars, zone_settings, char_stats, job_list, job2_list WHERE accounts_sessions.charid = chars.charid AND zone_settings.zoneid = chars.pos_zone AND char_stats.charid = chars.charid AND char_stats.mjob = job_list.jobid AND char_stats.sjob = job2_list.jobid order by chars.charname";
            st = con.createStatement();
            rs = st.executeQuery(query);
            String players = "*-----------*-------------------*------------------------------*\n|    Job    |     Character     |           Location           |\n*-----------*-------------------*------------------------------*";
            while (rs.next())  // Add each character's data.
            {
                StringBuilder character = new StringBuilder();
                StringBuilder location = new StringBuilder();
                StringBuilder job = new StringBuilder();
                StringBuilder subjob = new StringBuilder();
                StringBuilder gm = new StringBuilder();

                character.append(rs.getString("character"));  // Assign character, location, and job.
                location.append(rs.getString("location"));
                job.append(rs.getString("job")).append(rs.getString("level"));
                subjob.append(rs.getString("subjob")).append(rs.getString("sublevel"));
                gm.append(rs.getString("gm"));
                if (!subjob.toString().equals("NON0"))
                    job.append("/").append(subjob);
                if (!gm.toString().equals("0") && !gm.toString().equals("1"))
                    character.insert(0, "[GM]");

                players = players.concat("\n|" + String.format("%-11s", job.toString()) + "|" + String.format("%-19s", character.toString()) + "|" + String.format("%-30s", location.toString()).replaceAll("_", " ") + "|");  // Add the new row.
            }
            players = players.concat("\n*-----------*-------------------*------------------------------*");
            con.close();
            return players;

        } catch (SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
            return "Could not connect to SQL database.";
        } catch (Exception e) {
            // Server (Brian's computer) is down most likely
            System.out.println("Could not connect to SQL database.  Server most likely offline.");
        } finally {
            //finally block used to close resources
            try {
                if (st != null)
                    st.close();
            } catch (SQLException ignored) {
            }// nothing we can do
            try {
                if (con != null)
                    con.close();
            } catch (SQLException se) {
                se.printStackTrace();
                return "SQL Error.";
            }//end finally try
        }//end try
        assert con != null;
        con.close();
        return "";
    }

    public static void UpdateActivePlayer(Member member) throws SQLException {
        Connection con = null;
        Statement st = null;
        ResultSet rs;
        try
        {
            con = DriverManager.getConnection(DB_URL, USER, PASS);
            String query = "SELECT accounts.id, timelastmodify, discordid, accid FROM dspdb.accounts, discord WHERE accounts.id = discord.accid AND discordid = '" + member.getId() + "';";
            st = con.createStatement();
            rs = st.executeQuery(query);
            if (!rs.next())
            {
                con.close();
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime lastLoggedIn = LocalDateTime.parse(rs.getString("timelastmodify"), formatter);
            System.out.println("Checking status of " + member.getEffectiveName() + "...");
            if (now.minusDays(14).isBefore(lastLoggedIn))
            {
                if (!member.getGuild().getRoles().contains(member.getGuild().getRoleById("661433805441335297")))
                {
                    member.getGuild().addRoleToMember(member, Objects.requireNonNull(member.getGuild().getRoleById("661433805441335297"))).queue();
                    System.out.println(member.getEffectiveName() + " added to active players.");
                }
            }
            else if (member.getRoles().contains(member.getGuild().getRoleById("661433805441335297")))
            {
                member.getGuild().removeRoleFromMember(member, Objects.requireNonNull(member.getGuild().getRoleById("661433805441335297"))).queue();
                System.out.println(member.getEffectiveName() + " removed from active players.");
            }
            con.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            assert con != null;
            con.close();
        }
    }
}