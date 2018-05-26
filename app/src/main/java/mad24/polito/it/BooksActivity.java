package mad24.polito.it;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import mad24.polito.it.fragments.BooksFragment;
import mad24.polito.it.fragments.ChatFragment;
import mad24.polito.it.fragments.ProfileFragment;
import mad24.polito.it.fragments.SearchFragment;
import mad24.polito.it.fragments.viewbook.ViewBookFragment;
import mad24.polito.it.models.UserStatus;
import mad24.polito.it.registrationmail.LoginActivity;

public class BooksActivity  extends AppCompatActivity
{
    private DatabaseReference MeReference = null;
    public static final String FIREBASE_DATABASE_LOCATION_BOOKS = "books";
    public static final String FIREBASE_DATABASE_LOCATION_BOOKS_LOCATION = "locationBooks";
    public static final String FIREBASE_DATABASE_LOCATION_USERS = "users";

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;

    private BooksFragment booksFragment;
    private SearchFragment searchFragment;
    private ChatFragment chatFragment;
    private ProfileFragment profileFragment;
    private ViewBookFragment ViewBook;

    private enum CurrentFragment{BooksFragment, SearchFragment, ChatFragment, ProfileFragment, ViewBookFragment};

    private CurrentFragment currentFragment;

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {
                case mad24.polito.it.R.id.nav_home:
                    //Log.d("frag", "nav_home pressed");
//                    mTextMessage.setText(R.string.nav_home);
                    setFragment(booksFragment);
                    return true;
                case mad24.polito.it.R.id.nav_search:
                    //Log.d("frag", "nav_search pressed");
//                    mTextMessage.setText(R.string.nav_search);
                    setFragment(searchFragment);
                    return true;
                case R.id.nav_chat:
                    //Log.d("frag", "nav_profile_pressed");
//                    mTextMessage.setText(R.string.nav_profile);
                    setFragment(chatFragment);
                    return true;
                case mad24.polito.it.R.id.nav_profile:
                    //Log.d("frag", "nav_profile pressed");
//                    mTextMessage.setText(R.string.nav_profile);
                    setFragment(profileFragment);
                    return true;
            }
            return false;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mad24.polito.it.R.layout.activity_books);

        //Log.d("frag", "onCreate");
        /*mTextMessage = (TextView) findViewById(R.id.message);*/
        mMainFrame = (FrameLayout) findViewById(mad24.polito.it.R.id.main_frame);
        mMainNav = (BottomNavigationView) findViewById(mad24.polito.it.R.id.main_nav);

        if(savedInstanceState == null) {
            booksFragment = new BooksFragment();
            searchFragment = new SearchFragment();
            chatFragment = new ChatFragment();
            profileFragment = new ProfileFragment();
        } else {
            //Restore the fragment's instance
            /*if(getSupportFragmentManager().getFragment(savedInstanceState, "booksFragment") != null)
                booksFragment = (BooksFragment) getSupportFragmentManager().getFragment(savedInstanceState, "booksFragment");
            else*/
                booksFragment = new BooksFragment();

            if(getSupportFragmentManager().getFragment(savedInstanceState, "searchFragment") != null)
                searchFragment = (SearchFragment) getSupportFragmentManager().getFragment(savedInstanceState, "searchFragment");
            else
                searchFragment = new SearchFragment();

            /*if(getSupportFragmentManager().getFragment(savedInstanceState, "chatFragment") != null)
                chatFragment = (ChatFragment) getSupportFragmentManager().getFragment(savedInstanceState, "chatFragment");
            else*/
                chatFragment = new ChatFragment();

            /*if(getSupportFragmentManager().getFragment(savedInstanceState, "profileFragment") != null)
                profileFragment = (ProfileFragment) getSupportFragmentManager().getFragment(savedInstanceState, "profileFragment");
            else*/
                profileFragment = new ProfileFragment();
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(mad24.polito.it.R.id.main_nav);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //check if logged, if not, go to login activity
        if (userAuth == null) {
            Intent i = new Intent(BooksActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

            finish();
        }

        MeReference = FirebaseDatabase.getInstance().getReference().child("users").child(userAuth.getUid());

       setFragment(booksFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void setFragment(Fragment fragment) {

        currentFragment = CurrentFragment.valueOf(fragment.getClass().getSimpleName());

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(mad24.polito.it.R.id.main_frame, fragment);

        fragmentTransaction.commit();

        setMeOnline();
    }

    /**
     * Sets a fragment with back stack.
     * NOTE: I created this new signature because I didn't want to mess with others' code, and having to rewrite everything.
     * NOTE: this is public because some fragments, like BooksFragment, need to set other fragments
     * and provide a back mechanism.
     * So, if you need to set a back stack, use this method. Otherwise, call Marco's one.
     * @param fragment the fragment to be called
     * @param back the string which identifies the backstack (can also be null)
     */
    public void setFragment(Fragment fragment, String back)
    {
        currentFragment = CurrentFragment.valueOf(fragment.getClass().getSimpleName());

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(mad24.polito.it.R.id.main_frame, fragment)
                            .addToBackStack(back)
                            .commit();

        setMeOnline();
    }

    public void setViewBookFragment(ViewBookFragment Vb)
    {
        ViewBook = Vb;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //  TODO: this needs to be modified in case we were on a ViewBookFragment
        outState.putInt("fragment", currentFragment.ordinal());

        //  Were we on a view book?
        if(ViewBook != null)
        {
            outState.putString("viewbook", ViewBook.getArguments().getString("book"));
        }

        //Save the fragment's instance
        if(getSupportFragmentManager().getFragments().contains(booksFragment))
            getSupportFragmentManager().putFragment(outState, "booksFragment", booksFragment);

        if(getSupportFragmentManager().getFragments().contains(searchFragment))
            getSupportFragmentManager().putFragment(outState, "searchFragment", searchFragment);

        if(getSupportFragmentManager().getFragments().contains(chatFragment))
            getSupportFragmentManager().putFragment(outState, "chatFragment", chatFragment);

        if(getSupportFragmentManager().getFragments().contains(profileFragment))
            getSupportFragmentManager().putFragment(outState, "profileFragment", profileFragment);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Log.d("currFrag", "On restore instance state");

        int cf = (int) savedInstanceState.get("fragment");

        //Log.d("currFrag", "Restoring: "+ cf);
        switch (cf){
            case 0:
                setFragment(booksFragment);
                break;
            case 1:
                setFragment(searchFragment);
                break;
            case 2:
                setFragment(chatFragment);
                break;
            case 3:
                setFragment(profileFragment);
                break;
            case 4:
            {
                if(savedInstanceState.containsKey("viewbook"))
                {
                    Bundle args = new Bundle();
                    args.putString("book", savedInstanceState.getString("viewbook"));

                    final ViewBookFragment b = new ViewBookFragment();
                    b.setArguments(args);

                    setViewBookFragment(b);
                    setFragment(b, "ViewBook");
                }
            }
                break;
        }
    }

    public String getCurrentFragment() {
        return currentFragment.toString();
    }

    public void setCurrentFragment(int fragment) {
        switch(fragment){
            case 1:
                currentFragment = CurrentFragment.ProfileFragment;
                break;
            default:
                currentFragment = CurrentFragment.BooksFragment;
                break;
        }
    }
    private String getCurrentISODate()
    {
        Locale locale = null;

        //  Read the same thing I wrote for this on ChatActivity.kt
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locale = getApplicationContext().getResources().getConfiguration().getLocales().get(0);
        else locale = getApplicationContext().getResources().getConfiguration().locale;

        //  The dateformat
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale);

        return dateFormat.format(Calendar.getInstance(locale).getTime());
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //  Create the notification channel
        createNotificationChannel();

        //  Get the device token of the logged user, so we can send them notifications
        String token = FirebaseInstanceId.getInstance().getToken();

        //  Sometimes the token is null, so we have to check this
        if(token != null && MeReference != null)
        {
            MeReference.child("device_token").setValue(token);
        }

        setMeOnline();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        setMeOffline();
    }

    private void setMeOnline()
    {
        updateMyStatus(true, "", "home");
    }

    private void setMeOffline()
    {
        updateMyStatus(false, getCurrentISODate(), "0");
    }

    private void updateMyStatus(boolean online, String last, String inChat)
    {
        String className = online ? currentFragment.name() : "";

        //  Set my status as online and here
        UserStatus u = new UserStatus(online, last, className, inChat);

        MeReference.child("status").setValue(u).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid)
            {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Log.d("CHAT", "failed updating status");
            }
        });
    }

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_name), name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
