package com.example.elisl.mylab1;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class NewEditProfile extends AppCompatActivity {

    String  UserName,
            UserFullName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_edit_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.edit_profile_toolbar_title);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle != null && !bundle.isEmpty())
        {
            UserName = bundle.getString("user_name");
            UserFullName = bundle.getString("user_full_name");
        }

        ((TextView)findViewById(R.id.user_name)).setText(UserName);
        ((TextView)findViewById(R.id.user_full_name)).setText(UserFullName);

        ((EditText)findViewById(R.id.user_name_field)).setText(UserName);
        ((EditText)findViewById(R.id.user_full_name_field)).setText(UserFullName);

    }

}
