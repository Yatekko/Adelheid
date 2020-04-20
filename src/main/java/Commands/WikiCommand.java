package Commands;

import Utils.Categories;
import com.google.gson.Gson;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Searches https://ffxiclopedia.fandom.com for command args, returns first result.
 * Usage:  !wiki [searchTerm]
 */

public class WikiCommand extends Command
{
    public WikiCommand() throws ArrayIndexOutOfBoundsException
    {
        this.name = "wiki";
        this.help = "Searches ffxiclopedia and gives you the first result of your search.  Example:  `!wiki Red Mage`";
        this.category = Categories.GENERAL;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String SearchTerm = event.getArgs();
        SearchTerm = SearchTerm.replaceAll("\\s+", "+");
        URL url = null;
        try {
            url = new URL("https://ffxiclopedia.fandom.com/api/v1/Search/List?query=" + SearchTerm + "&limit=1&minArticleQuality=10&batch=1&namespaces=0%2C14");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection connect = null;
        try {
            assert url != null;
            connect = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStreamReader reader = null;
        try {
            assert connect != null;
            reader = new InputStreamReader(connect.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert reader != null;
        WikiSearch Search = new Gson().fromJson(reader, WikiSearch.class);
        event.reply(Search.items[0].url);
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

