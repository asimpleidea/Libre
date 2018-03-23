package com.example.elisl.mylab1;

import android.content.Intent;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class EditUserProfile extends AppCompatActivity {

    String  UserName,
            UserFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_profile);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.edit_profile_toolbar);
        toolbar.setTitle(R.string.edit_profile_toolbar_title);
        setSupportActionBar(toolbar);*/

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null && !extras.isEmpty())
        {
            UserName = extras.getString("user_name");
            UserFullName = extras.getString("user_full_name");
        }

        ((TextView)findViewById(R.id.user_name)).setText(UserName);
        ((TextView)findViewById(R.id.user_full_name)).setText( UserFullName);

    }
}
