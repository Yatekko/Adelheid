package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import net.dv8tion.jda.api.EmbedBuilder;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AHCommand extends Command  //TODO:  Filter out single and double quotes in search terms.
{
    private String DB_URL;
    private String USER;
    private String PASS;
    private EventWaiter waiter;
    private int stack;
    private String search;

    public AHCommand(EventWaiter waiter, String DB_URL, String USER, String PASS)  // TODO:  Implement OrderedMenu in case of multiple item matches.
    {
        this.name = "ah";
        this.DB_URL = DB_URL;
        this.USER = USER;
        this.PASS = PASS;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Connection con;
        try
        {
            con = DriverManager.getConnection(DB_URL, USER, PASS);
        }
        catch (SQLException e)
        {
            event.reply("Cannot connect to SQL server.");
            return;
        }

        if (event.getArgs().split(" ")[0].isEmpty())
        {
            try
            {
                con.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            return;
        }

        if (stack > 0)
            search = event.getArgs().replaceAll(" ", "_");

        if (event.getArgs().split(" ")[0].toLowerCase().equals("single") || stack == 1)
        {
            if (stack == 0)
                search = event.getArgs().substring(7).replaceAll(" ", "_");
            try
            {
                if (!isRealItem(search, con))
                {
                    event.reply("Could not find an item with that name.");
                    try
                    {
                        con.close();
                    }
                    catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            catch (SQLException e)
            {
                event.reply("Cannot connect to SQL server.");
                return;
            }
            try
            {
                searchSingle(event, search, con);
            }
            catch (SQLException e)
            {
                event.reply("Cannot connect to SQL server.");
                return;
            }
        } else if (event.getArgs().split(" ")[0].toLowerCase().equals("stack") || stack == 2)
        {
            if (stack == 0)
                search = event.getArgs().substring(6).replaceAll(" ", "_");
            try
            {
                if (!isRealItem(search, con))
                {
                    event.getChannel().sendMessage("Could not find an item with that name.").queue();
                    try
                    {
                        con.close();
                    }
                    catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            catch (SQLException e)
            {
                event.reply("Cannot connect to SQL server.");
                return;
            }
            try
            {
                searchStack(event, search, con);
            }
            catch (SQLException e)
            {
                event.reply("Cannot connect to SQL server.");
                return;
            }
        } else
        {
                new OrderedMenu.Builder()
                        .setText("Would you like to search for a single item or a stack?")
                        .setChoices("Single", "Stack")
                        .allowTextInput(false)
                        .setEventWaiter(waiter)
                        .setUsers(event.getAuthor())
                        .setCancel((message ->
                        {
                            message.delete().queue();
                            event.reply("Response took too long.", message1 -> message1.delete().queueAfter(10, TimeUnit.SECONDS));
                        }))
                        .setSelection((message, integer) ->
                        {
                            setStack(integer);
                            execute(event);
                        })
                        .build().display(event.getChannel());
        }
    }

    private void searchSingle(CommandEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale = 0 and stack = 0 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 200";
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
        }
        if (!rs.next())
        {
            if (count != 0)
                event.reply("No sale history.  Currently " + count + " item(s) on sale for an average of " + (subtotal/count) + " gil each.");
            else
            {
                event.reply("No sale history and no items currently on sale.");
            }
            stack = 0;
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        if (count == 0)
            embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        else
            embed.setTitle("Sale history for " + search.replaceAll("_", " ") + "\nCurrently " + count + " item(s) on sale.");
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        stack = 0;
        event.reply(embed.build());
    }

    private void searchStack(CommandEvent event, String search, Connection con) throws SQLException
    {
        String query = "SELECT item_basic.itemid, name, sale, seller_name, buyer_name, price, sell_date FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale > 0 and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 15";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(query);
        query = "SELECT item_basic.itemid, price FROM dspdb.item_basic, auction_house where item_basic.itemid = auction_house.itemid and sale = 0 and stack = 1 and name like '%" + search + "%' order by auction_house.sell_date DESC limit 200";
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
        }
        if (!rs.next())
        {
            if (count != 0)
                event.reply("No sale history.  Currently " + count + " stacks(s) on sale for an average of " + (subtotal/count) + " gil each.");
            else
            {
                event.reply("No sale history and no stacks currently on sale.");
            }
            stack = 0;
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        if (count == 0)
            embed.setTitle("Sale history for " + search.replaceAll("_", " "));
        else
            embed.setTitle("Sale history for " + search.replaceAll("_", " ") + "\nCurrently " + count + " stacks(s) on sale.");
        do
            embed.addField("Sold on " + DateTimeFormatter.ofPattern("MM-dd").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalDate()) + " at " + DateTimeFormatter.ofPattern("h:mm a").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(rs.getInt("sell_date")), TimeZone.getDefault().toZoneId()).toLocalTime()), rs.getString("seller_name") + " sold to " + rs.getString("buyer_name") + " for " + String.format("%,d", rs.getInt("sale")), true);
        while (rs.next());
        stack = 0;
        event.reply(embed.build());
    }

    private boolean isRealItem(String search, Connection con) throws SQLException
    {
        Statement st = con.createStatement();
        String query = "SELECT * FROM dspdb.item_basic where name like '%" + search + "%'";
        ResultSet rs = st.executeQuery(query);
        return rs.next();
    }

    private void setStack(Integer i)
    {
        this.stack = i;
    }
}
