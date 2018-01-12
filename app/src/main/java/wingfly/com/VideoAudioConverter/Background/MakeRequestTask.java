package wingfly.com.VideoAudioConverter.Background;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wingfly.com.VideoAudioConverter.MainActivity;
import wingfly.com.VideoAudioConverter.YoutubeStuff;
import wingfly.com.VideoAudioConverter.fragments.ListFragment;

/**
 * An asynchronous task that handles the YouTube Data API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
public class MakeRequestTask extends AsyncTask<YoutubeStuff, Void, List<String>>
{
    private com.google.api.services.youtube.YouTube mService = null;
    private Exception mLastError = null;
    private static final String TAG = "logging_tag_main";
    private MainActivity activity;
    private static final int REQUEST_AUTHORIZATION = 2;


    public MakeRequestTask(MainActivity mainActivity)
    {
        try
        {
            activity = mainActivity;
            mService = mainActivity.getYouTubeService();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Background task to call YouTube Data API.
     *
     * @param params no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(YoutubeStuff... params)
    {
        try
        {
            switch (params[0])
            {
                case CHANNEL:
                    return getChannelInfo();
                case PLAYLIST:
                    return getChannelPlaylists();
                default:
                    return null;
            }

        } catch (Exception e)
        {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    /**
     * Fetch information about the "GoogleDevelopers" YouTube channel.
     *
     * @return List of Strings containing information about the channel.
     * @throws IOException
     */
    private List<String> getChannelInfo() throws IOException
    {
        // Get a list of up to 10 files.
        List<String> channelInfo = new ArrayList<String>();
        ChannelListResponse result = mService.channels().list("snippet,contentDetails")
                .setMine(true)
                .execute();
        List<Channel> channels = result.getItems();
        if (channels != null)
        {
            Channel channel = channels.get(0);
            channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                    "Its title is '" + channel.getSnippet().getTitle());
        }
        return channelInfo;
    }

    private List<String> getChannelPlaylists() throws IOException
    {
        List<String> channelInfo = new ArrayList<>();
        PlaylistListResponse result = mService.playlists().list("snippet,contentDetails")
                .setMine(true)
                .execute();
        List<Playlist> playlists = result.getItems();
        if (playlists != null)
        {
            Playlist playlist = playlists.get(0);
            channelInfo.add("This ");
        }
        return channelInfo;
    }


    @Override
    protected void onPreExecute()
    {
    }

    @Override
    protected void onPostExecute(List<String> output)
    {
        if (output == null || output.size() == 0)
        {
            Log.d(TAG, "No results returned.");
        } else
        {
            output.add(0, "Data retrieved using the YouTube Data API:");
            Log.d(TAG, TextUtils.join("\n", output));
        }
        ListFragment.getData(output);
    }

    @Override
    protected void onCancelled()
    {
        if (mLastError != null)
        {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException)
            {
                activity.showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException)
            {
                activity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        REQUEST_AUTHORIZATION);
            } else
            {
                Log.d(TAG, "The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else
        {
            Log.d(TAG, "Request cancelled.");
        }
    }
}
