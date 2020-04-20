package Commands;

import Utils.Categories;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;

public class PingCommand extends Command
{
    public PingCommand()
    {
        this.name = "ping";
        this.help = "Tests for latency.";
        this.category = Categories.GENERAL;
        this.cooldown = 15;
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.reply("Pong!");
    }
}
