package mad24.polito.it;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import mad24.polito.it.fragments.BooksFragment;
import mad24.polito.it.fragments.ChatFragment;
import mad24.polito.it.fragments.ProfileFragment;
import mad24.polito.it.fragments.SearchFragment;

public class BooksActivity  extends AppCompatActivity
{
    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;

    private BooksFragment booksFragment;
    private SearchFragment searchFragment;
    private ChatFragment chatFragment;
    private ProfileFragment profileFragment;

    private enum CurrentFragment{BooksFragment, SearchFragment, ChatFragment, ProfileFragment, ViewBookFragment, LoadingScreenFragment};
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

        setFragment(booksFragment);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Log.d("currFrag", "Saving: "+ currentFragment.ordinal());
        //  TODO: this needs to be modified in case we were on a ViewBookFragment
        outState.putInt("fragment", currentFragment.ordinal());
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
        }
    }

}
