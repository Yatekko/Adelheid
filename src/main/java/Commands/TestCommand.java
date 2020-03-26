package Commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class TestCommand extends Command
{
    private String DB_URL;
    private String USER;
    private String PASS;

    public TestCommand(String dbUrl, String user, String pass)
    {
        this.name = "test";
        this.ownerCommand = true;
        this.DB_URL = dbUrl;
        this.USER = user;
        this.PASS = pass;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        System.out.println("This is a test");
        return;
    }
}
