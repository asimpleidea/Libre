package com.example.elisl.mylab1;

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

public class BooksActivity extends AppCompatActivity {

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;

    private BooksFragment booksFragment;
    private SearchFragment searchFragment;
    private ProfileFragment profileFragment;

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    Log.d("frag", "nav_home pressed");
//                    mTextMessage.setText(R.string.nav_home);
                    setFragment(booksFragment);
                    return true;
                case R.id.nav_search:
                    Log.d("frag", "nav_search pressed");
//                    mTextMessage.setText(R.string.nav_search);
                    setFragment(searchFragment);
                    return true;
                case R.id.nav_profile:
                    Log.d("frag", "nav_profile pressed");
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
        setContentView(R.layout.activity_books);
        Log.d("frag", "onCreate");
        /*mTextMessage = (TextView) findViewById(R.id.message);*/
        mMainFrame = (FrameLayout) findViewById(R.id.main_frame);
        mMainNav = (BottomNavigationView) findViewById(R.id.main_nav);

        booksFragment = new BooksFragment();
        searchFragment = new SearchFragment();
        profileFragment = new ProfileFragment();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.main_nav);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.main_frame, fragment);

        fragmentTransaction.commit();
    }

}
