package mad24.polito.it;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import mad24.polito.it.fragments.BooksFragment;
import mad24.polito.it.fragments.ChatFragment;
import mad24.polito.it.fragments.ProfileFragment;
import mad24.polito.it.fragments.SearchFragment;
import mad24.polito.it.fragments.viewbook.ViewBookFragment;
import mad24.polito.it.registrationmail.LoginActivity;

public class BooksActivity  extends AppCompatActivity
{
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

        booksFragment = new BooksFragment();
        searchFragment = new SearchFragment();
        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();

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
}
