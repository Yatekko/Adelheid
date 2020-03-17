import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class AH  //TODO:  Filter out single and double quotes in search terms.
{
    public AH(@NotNull GuildMessageReceivedEvent event, String DB_URL, String USER, String PASS) throws SQLException {
        String message = event.getMessage().getContentDisplay();
        Connection con = DriverManager.getConnection(DB_URL, USER, PASS);

        if (message.split(" ")[1].isEmpty())
        {
            con.close();
            return;
        }

        switch (message.split(" ")[1].toLowerCase())
        {
            case "single":
                if (!message.split(" ")[2].isEmpty())
                {
                    if (!isRealItem(message.split(" ")[2].replaceAll(" ", "_"), con))
                    {
                        event.getChannel().sendMessage("Could not find an item with that name.").queue();
                        con.close();
                        return;
                    }
                    searchSingle(event, message.substring(11).replaceAll(" ", "_"), con);
                }
                else
                {
                    event.getChannel().sendMessage("No item to search for.").queue();
                    con.close();
                    return;
                }
                break;
            case "stack":
                if (!message.split(" ")[2].isEmpty())
                {
                    if (!isRealItem(message.split(" ")[2].replaceAll(" ", "_"), con))
                    {
                        event.getChannel().sendMessage("Could not find an item with that name.").queue();
                        con.close();
                        return;
                    }
                    searchStack(event, message.substring(10).replaceAll(" ", "_"), con);
                }
                else
                {
                    event.getChannel().sendMessage("No item to search for.").queue();
                    con.close();
                    return;
                }
                break;
            default:
                // TODO:  Write asynchronous logic to ask if they want single or stack.  If the item doesn't stack, simply display. - https://www.baeldung.com/java-completablefuture
                break;
        }

    }

    private void searchSingle(GuildMessageReceivedEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (!rs.next())
        {
            query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date limit 200";
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
                event.getChannel().sendMessage("No sale history.  Currently " + count + " item(s) on sale for an average of " + (subtotal/count) + " gil each.").queue();
            }
            else
            {
                event.getChannel().sendMessage("No sale history and no items currently on sale.").queue();
                return;
            }
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        event.getChannel().sendMessage(embed.build()).queue();
    }

    private void searchStack(GuildMessageReceivedEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (!rs.next())
        {
            query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date limit 200";
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
                event.getChannel().sendMessage("No sale history.  Currently " + count + " stack(s) on sale for an average of " + (subtotal/count) + " gil each.").queue();
            }
            else
            {
                event.getChannel().sendMessage("No sale history and no stacks currently on sale.").queue();
                return;
            }
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        event.getChannel().sendMessage(embed.build()).queue();
    }

    public boolean isRealItem(String search, Connection con) throws SQLException
    {
        Statement st = con.createStatement();
        String query = "SELECT * FROM dspdb.item_basic where name like '%" + search + "%'";
        ResultSet rs = st.executeQuery(query);
        return rs.next();
    }
}
