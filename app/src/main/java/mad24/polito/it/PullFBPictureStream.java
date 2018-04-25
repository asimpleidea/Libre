package mad24.polito.it;

import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class PullFBPictureStream extends AsyncTask<String, Integer, InputStream>
{
    /**
     * UID
     */
    private String UID = null;

    PullFBPictureStream(String uid)
    {
        UID = uid;
    }

    @Override
    protected InputStream doInBackground(String... pic)
    {
        try
        {
            URL url = new URL(pic[0]);
            InputStream stream = url.openStream();
            return stream;
        }
        catch(MalformedURLException m)
        {
            return null;
        }
        catch(IOException i)
        {
            return null;
        }
    }
}
