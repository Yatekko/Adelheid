package Commands;

import Utils.Categories;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestCommand extends Command
{
    private String DB_URL;
    private String USER;
    private String PASS;
    private final String CANCEL = "\u274C";
    private final String CONFIRM = "\u2611";
    private final String CLOCK = "\u23F0";
    private final String CLAP = "\u1F44F";
    private Message message;

    private final EventWaiter waiter;

    public TestCommand(EventWaiter waiter, String dbUrl, String user, String pass)
    {
        this.name = "test";
        this.help = "Command used to test features currently in development.";
        this.category = Categories.OWNER;
        this.ownerCommand = true;
        this.DB_URL = dbUrl;
        this.USER = user;
        this.PASS = pass;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event)
    {

    }
}
