package com.example.elisl.mylab1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ShowProfileActivity extends AppCompatActivity {

    ImageView editImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    TextView name;
    TextView mail;
    TextView bio;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_profile);

        //button to edit profile
        editImage = (ImageView) findViewById(R.id.imageEdit);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) findViewById(R.id.showImageProfile);

        //get edit fields
        name = (TextView) findViewById(R.id.showName);
        mail = (TextView) findViewById(R.id.showMail);
        bio = (TextView) findViewById(R.id.showBio);

        //listener onClick for editing
        editImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), EditProfileActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("state", "OnResume - show");
        //get preferences
        prefs = getSharedPreferences("profile", MODE_PRIVATE);

        //get name if already inserted
        String str = prefs.getString("profileName", null);
        if (str != null) {
            name.setText(str);
        }

        //get mail if already inserted
        str = prefs.getString("profileMail", null);
        if (str != null) {
            mail.setText(str);
        }

        //get bio if already inserted
        str = prefs.getString("profileBio", null);
        if (str != null) {
            bio.setText(str);
        }

        //get image profile if already inserted
        str = prefs.getString("profileImage", null);
        Log.i("state", "show: "+str);
        if (str != null) {
            imageProfile.setImageURI(Uri.fromFile(new File(str)) );
        } else {
            //default image
            Drawable d = getResources().getDrawable(R.drawable.unknown_user);
            imageProfile.setImageDrawable(d);
        }

    }
}
