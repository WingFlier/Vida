package wingfly.com.VideoAudioConverter;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.vistrav.ask.Ask;
import com.vistrav.ask.annotations.AskDenied;
import com.vistrav.ask.annotations.AskGranted;

import java.io.IOException;
import java.util.Arrays;

import wingfly.com.VideoAudioConverter.Background.DownloadImageTask;
import wingfly.com.VideoAudioConverter.Background.MakeRequestTask;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener
{

    //TODO account permission part error when permission is not given but already signed in
    // TODO check permissions before every click after login

    private static final int RC_SIGN_IN = 0;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;
    private static final int REQUEST_AUTHORIZATION = 2;

    GoogleAccountCredential mCredential;
    GoogleSignInClient client;

    private static final String TAG = "logging_tag_main";
    private static final String[] SCOPES = {YouTubeScopes.YOUTUBE_READONLY};
    private GoogleSignInAccount account;

    private NavigationView navigationView;
    private TextView noConnectionTextView;
    private ImageView refreshBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        noConnectionTextView = findViewById(R.id.noInternetTextView);
        refreshBtn = findViewById(R.id.refreshBtn);

        refreshBtn.setOnClickListener(this);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(this, gso);
        account = GoogleSignIn.getLastSignedInAccount(this);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        getResultsFromApi();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi()
    {
        if (!isGooglePlayServicesAvailable())
        {
            acquireGooglePlayServices();
        } else if (!isDeviceOnline())
        {
            showNoInternetElements();
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else if (mCredential.getSelectedAccountName() == null && account == null)
        {
            signIn(client);
        } else
        {
            updateNavHeader();
            mCredential.setSelectedAccountName(account.getEmail());
//            new MakeRequestTask(MainActivity.this).execute(YoutubeStuff.CHANNEL);
        }
    }

    private void hideNoInternetElements()
    {
        refreshBtn.setVisibility(View.GONE);
        noConnectionTextView.setVisibility(View.GONE);
    }

    private void showNoInternetElements()
    {
        refreshBtn.setVisibility(View.VISIBLE);
        noConnectionTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline()
    {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices()
    {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode))
        {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable()
    {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void signIn(GoogleSignInClient mGoogleSignInClient)
    {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN)
        {
            Ask.on(MainActivity.this).id(0)
                    .forPermissions(Manifest.permission.GET_ACCOUNTS)
                    .withRationales("In order to save file you will need to grant storage permission")
                    .go();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try
            {
                account = task.getResult(ApiException.class);
            } catch (ApiException e)
            {
                e.printStackTrace();
            }
        } else if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES)
            if (resultCode != RESULT_OK)
            {
                Log.d(TAG, "This app requires Google Play Services. Please install " +
                        "Google Play Services on your device and relaunch this app.");
            } else
            {
                getResultsFromApi();
            }
    }

    @AskGranted(Manifest.permission.GET_ACCOUNTS)
    public void fileAccessGranted(int id)
    {
        Log.i(TAG, "FILE  GRANTED " + hasPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS));

        mCredential.setSelectedAccountName(account.getDisplayName());
        getResultsFromApi();
    }

    @AskDenied(Manifest.permission.GET_ACCOUNTS)
    public void fileAccessDenied(int id)
    {
        Log.i(TAG, "FILE  DENiED");
    }

    private void updateNavHeader()
    {
        View headerView = navigationView.getHeaderView(0);
        TextView accMail = headerView.findViewById(R.id.accMail);
        TextView accName = headerView.findViewById(R.id.accName);
        de.hdodenhof.circleimageview.CircleImageView accImage = headerView.findViewById(R.id.accImage);

        accMail.setText(account.getEmail());
        accName.setText(account.getDisplayName());
        Uri photoUrl = account.getPhotoUrl();
        if (photoUrl == null)
            accImage.setImageDrawable(getResources().getDrawable(R.drawable.photo));
        else
        {
            new DownloadImageTask(accImage)
                    .execute(photoUrl.toString());
//            accImage.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        switch (item.getItemId())
        {
            case R.id.nav_playlists:
                new MakeRequestTask(MainActivity.this).execute(YoutubeStuff.PLAYLIST);
                moveToFragment(
                        wingfly.com.VideoAudioConverter.fragments.ListFragment.newInstance(null, null));
                break;
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public YouTube getYouTubeService() throws IOException
    {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new YouTube.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("YouTube Data API Android Quickstart")
                .build();
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    public void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode)
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.refreshBtn:
                if (isDeviceOnline())
                {
                    hideNoInternetElements();
                    getResultsFromApi();
                } else
                    Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public boolean hasPermission(Context context, String permission)
    {
        return (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }

    public void moveToFragment(Fragment fragment)
    {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_frame, fragment, null)
                .commit();
    }
}