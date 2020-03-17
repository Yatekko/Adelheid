import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Linkshell
{
    private String DB_URL;
    private String USER;
    private String PASS;
    private MessageChannel channel;

    public Linkshell(JDA bot, String DB_URL, String USER, String PASS)
    {
        this.DB_URL = DB_URL;
        this.USER = USER;
        this.PASS = PASS;
        channel = bot.getTextChannelById("672185258053206026");
    }

    public void LStoDiscord()
    {
        try
        {
            Connection con = DriverManager.getConnection(DB_URL, USER, PASS);
            String query = "SELECT * FROM dspdb.audit_chat";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next())
            {
                do {
                    StringBuilder message = new StringBuilder(rs.getString("message"));
                    for (int i = 0; i < message.length(); i++) {
                        if (message.charAt(i) != 'ý')  // Translates 4th and 5th bytes of autotranslate phrases into decimals, then sends them to AutoTranslate().
                            continue;
                        String AT = message.substring(i + 3, message.indexOf("ý", i + 1));
                        String ATHex = String.format("%040x", new BigInteger(1, AT.getBytes("ISO_8859_1")));
                        ATHex = ATHex.substring(ATHex.length()-4);
                        message.delete(i, i + 6);
                        message.insert(i, "{" + AutoTranslate(Integer.parseInt(ATHex, 16)) + "}");
                    }
                    assert channel != null;
                    channel.sendMessage("__**" + rs.getString("speaker") + ":**__```" + message.toString() + "```").queue();
                    con.createStatement().execute("DELETE FROM dspdb.audit_chat WHERE lineID = " + rs.getInt("lineID"));
                }
                while (rs.next());
            }
            con.close();
        }
        catch (CJCommunicationsException | CommunicationsException e)
        {
            System.out.println(LocalDateTime.now() + ":  Connection to server failed.  Server probably offline.");
            return;
        }
        catch (SQLException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
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
}