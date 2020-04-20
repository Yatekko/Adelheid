package Commands;

import Utils.Categories;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

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
        //waitForConfirmation(event, "This is a test.", () -> event.reply("Test complete."));
        MessageChannel channel = event.getJDA().getTextChannelById("688195963843641443");
        assert channel != null;
        channel.retrieveMessageById(channel.getLatestMessageId()).queue(chanMessage -> message = chanMessage);
        message.editMessage("Edited.").queue();
        return;
    }

   private void waitForConfirmation(CommandEvent event, String message, Runnable confirm)
    {
        new OrderedMenu.Builder()
                .useNumbers()
                .setChoices(CONFIRM, CANCEL, CLOCK, CLAP)
                .setEventWaiter(waiter)
                .setTimeout(1, TimeUnit.MINUTES)
                .setText(message + "\n\n" + CONFIRM + " Continue\n" + CANCEL+" Cancel\n" + CLOCK + "Clock\n" + CLAP + "Clap")
                //.setFinalAction(m -> m.delete().queue(s->{}, f->{}))
                .setUsers(event.getAuthor())
                .setCancel((msg) -> {msg.delete().queue();})
                /*.setAction(re ->
                {
                    if(re.getName().equals(CONFIRM))
                        confirm.run();
                })*/
                .setSelection((msg, i) ->
                {
                    event.replySuccess("Selection " + i);
                })
                .build().display(event.getChannel());
    }
}
