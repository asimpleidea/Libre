package com.example.elisl.mylab1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewProfile extends AppCompatActivity
{
    String UserName = "SunSince90";
    String UserFullName = "Elis Lulja";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.toolbar_view_your_profile);
        setSupportActionBar(toolbar);

        ImageView picture = (ImageView) findViewById(R.id.user_picture);
        picture.setImageResource(R.drawable.ic_launcher_background);

        TextView userFullName = (TextView) findViewById(R.id.user_full_name);
        userFullName.setText(UserFullName);

        TextView username = (TextView) findViewById(R.id.user_name);
        username.setText(UserName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_profile)
        {
            Intent editProfile = new Intent(ViewProfile.this, NewEditProfile.class);

            Bundle out = new Bundle();
            out.putString("user_name", UserName );
            out.putString("user_full_name", UserFullName);
            editProfile.putExtras(out);
            startActivity(editProfile);
        }

        return super.onOptionsItemSelected(item);
    }
}
