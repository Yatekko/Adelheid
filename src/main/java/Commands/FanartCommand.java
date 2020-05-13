package Commands;

import Utils.Categories;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FanartCommand extends Command
{
    MessageChannel messageChannel;

    public FanartCommand()
    {
        this.name = "fanart";
        this.help = "Submit a piece of fanart for the event.  Image must be attached to the command.";
        this.category = Categories.EVENT;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        messageChannel = event.getGuild().getTextChannelById("704894292682080287");

        if (event.getMessage().getAttachments().isEmpty())  // No attachment.
            event.replyError("Must attach the image directly to the message with the command.");
        else
        {
            if (!event.getMessage().getAttachments().get(0).isImage()) // Attachment is not a picture
                event.replyError("Attachment must be an image.");
            else
            {
                int submissionNum = (int) (Math.random() * 100) + 1;
                messageChannel.sendMessage("Submission #" + submissionNum + ":").complete();
                try
                {
                    File file = new File("image.jpg");
                    messageChannel.sendFile(event.getMessage().getAttachments().get(0).downloadToFile(file).get()).queue(message ->
                    {
                        file.delete();
                        try
                        {
                            FileWriter writer = new FileWriter("Submissions.txt");
                            writer.write(submissionNum + ":  " + event.getMember().getEffectiveName() + "  (" + event.getMember().getUser().getName() + "#" + event.getMember().getUser().getDiscriminator() + ")\n");
                            writer.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        event.getMessage().delete().queue();
                        event.getMessage().getChannel().sendMessage("Thank you for your submission!").queue(response -> response.delete().queueAfter(15, TimeUnit.SECONDS));
                    });
                }
                catch (InterruptedException | ExecutionException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
