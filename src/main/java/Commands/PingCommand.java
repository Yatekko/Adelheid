package Commands;

import Utils.Categories;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class PingCommand extends Command
{
    public PingCommand()
    {
        this.name = "ping";
        this.help = "Tests for latency.";
        this.category = Categories.GENERAL;
        this.cooldown = 15;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.reply("Pong!");
    }
}
