package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class AHCommand extends Command  //TODO:  Filter out single and double quotes in search terms.
{
    private String DB_URL;
    private String USER;
    private String PASS;

    public AHCommand(String DB_URL, String USER, String PASS)  // TODO:  Implement Menu Builder and Event Waiter of JDA-Utilities to create a list to select from, both asking for single/stack and in case of multiple item matches.
    {
        this.name = "ah";
        this.DB_URL = DB_URL;
        this.USER = USER;
        this.PASS = PASS;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Connection con;
        try {
            con = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            event.reply("Cannot connect to SQL.SQL server.");
            return;
        }

        if (event.getArgs().split(" ")[0].isEmpty())
        {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        switch (event.getArgs().split(" ")[0].toLowerCase())
        {
            case "single":
                if (!event.getArgs().split(" ")[1].isEmpty())
                {
                    try {
                        if (!isRealItem(event.getArgs().substring(7).replaceAll(" ", "_"), con))
                        {
                            event.reply("Could not find an item with that name.");
                            try {
                                con.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    } catch (SQLException e) {
                        event.reply("Cannot connect to SQL.SQL server.");
                        return;
                    }
                    try {
                        searchSingle(event, event.getArgs().substring(7).replaceAll(" ", "_"), con);
                    } catch (SQLException e) {
                        event.reply("Cannot connect to SQL.SQL server.");
                        return;
                    }
                }
                else
                {
                    event.reply("No item to search for.");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                break;
            case "stack":
                if (!event.getArgs().split(" ")[1].isEmpty())
                {
                    try {
                        if (!isRealItem(event.getArgs().substring(6).replaceAll(" ", "_"), con))
                        {
                            event.getChannel().sendMessage("Could not find an item with that name.").queue();
                            try {
                                con.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    } catch (SQLException e) {
                        event.reply("Cannot connect to SQL.SQL server.");
                        return;
                    }
                    try {
                        searchStack(event, event.getArgs().substring(6).replaceAll(" ", "_"), con);
                    } catch (SQLException e) {
                        event.reply("Cannot connect to SQL.SQL server.");
                        return;
                    }
                }
                else
                {
                    event.reply("No item to search for.");
                    try {
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                break;
            default:
                // TODO: Add single/stack menu choice here.
                break;
        }

    }

    private void searchSingle(CommandEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (!rs.next())
        {
            query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 200";
            st = con.createStatement();
            ResultSet rs2 = st.executeQuery(query);

            int subtotal = 0;
            int count = 0;
            if (rs2.next())
            {
                do
                {
                    subtotal += rs2.getInt("price");
                    count++;
                }
                while (rs2.next());
                event.reply("No sale history.  Currently " + count + " item(s) on sale for an average of " + (subtotal/count) + " gil each.");
            }
            else
            {
                event.reply("No sale history and no items currently on sale.");
                return;
            }
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        event.reply(embed.build());
    }

    private void searchStack(CommandEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (!rs.next())
        {
            query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 200";
            st = con.createStatement();
            ResultSet rs2 = st.executeQuery(query);

            int subtotal = 0;
            int count = 0;
            if (rs2.next())
            {
                do
                {
                    subtotal += rs2.getInt("price");
                    count++;
                }
                while (rs2.next());
                event.reply("No sale history.  Currently " + count + " stack(s) on sale for an average of " + (subtotal/count) + " gil each.");
            }
            else
            {
                event.reply("No sale history and no stacks currently on sale.");
                return;
            }
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        event.reply(embed.build());

    }

    public boolean isRealItem(String search, Connection con) throws SQLException
    {
        Statement st = con.createStatement();
        String query = "SELECT * FROM dspdb.item_basic where name like '%" + search + "%'";
        ResultSet rs = st.executeQuery(query);
        return rs.next();
    }
}
