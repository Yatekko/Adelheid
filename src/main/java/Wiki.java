import com.google.gson.Gson;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Wiki
{
    public Wiki(GuildMessageReceivedEvent event) throws IOException, ArrayIndexOutOfBoundsException
    {
        String SearchTerm = event.getMessage().getContentRaw().substring(6);
        SearchTerm = SearchTerm.replaceAll("\\s+", "+");
        URL url = new URL("https://ffxiclopedia.fandom.com/api/v1/Search/List?query=" + SearchTerm + "&limit=1&minArticleQuality=10&batch=1&namespaces=0%2C14");
        URLConnection connect = url.openConnection();
        InputStreamReader reader = new InputStreamReader(connect.getInputStream());
        WikiSearch Search = new Gson().fromJson(reader, WikiSearch.class);
        event.getChannel().sendMessage(Search.items[0].url).queue();
        reader.close();
    }

    static class WikiSearch
    {
        private int batches;
        private int currentBatch;
        private int next;
        private int total;
        Result[] items;
    }

    static class Result
    {
       private int id;
       private String title;
       private String snippet;
       private String url;
       private int ns;
    }
}

