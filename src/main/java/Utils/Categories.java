package Utils;

import com.jagrosh.jdautilities.command.Command.Category;

public class Categories
{
    public static final Category OWNER = new Category("Owner", event ->
    {
        if (event.isOwner())
            return true;
        else
        {
            event.replyError("Sorry, only the bot's owner can access this command!");
            return false;
        }
    });

    public static final Category GENERAL = new Category("General", event -> true);
    public static final Category EVENT = new Category("Event", event -> true);
}
