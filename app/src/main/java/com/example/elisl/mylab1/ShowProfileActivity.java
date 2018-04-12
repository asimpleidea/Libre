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
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class ShowProfileActivity extends AppCompatActivity {

    ImageView editImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    private TextView name;
    private TextView phone;
    private TextView mail;
    private TextView bio;
    private TextView city;

    private String[] genresList;
    private LinearLayout genres;

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
        phone = (TextView) findViewById(R.id.showPhone);
        mail = (TextView) findViewById(R.id.showMail);
        bio = (TextView) findViewById(R.id.showBio);
        city = (TextView) findViewById(R.id.showCity);

        genres = (LinearLayout) findViewById(R.id.show_favourite_genres_list);
        genresList = getResources().getStringArray(R.array.genres);

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
        if (str != null)
            name.setText(str);

        str = prefs.getString("profilePhone", null);
        if(str != null)
            phone.setText(str);

        //get mail if already inserted
        str = prefs.getString("profileMail", null);
        if (str != null)
            mail.setText(str);

        //get bio if already inserted
        str = prefs.getString("profileBio", null);
        if (str != null)
            bio.setText(str);

        //get city if already inserted
        str = prefs.getString("profileCity", null);
        if (str != null)
            city.setText(str);

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

        //get saved selectedItems
        str = prefs.getString("profileGenres", null);
        genres.removeAllViews();

        if(str != null && !str.isEmpty()) {
            String[] strArray = str.split(",");

            for(int i = 0; i < strArray.length; i++) {
                genres.addView(BuildGenreLayout(genresList[Integer.parseInt(strArray[i])] ) );
            }
        }

    }

    private TextView BuildGenreLayout(final String name) {
        TextView genre = new TextView(getApplicationContext());
        genre.setText(name);
        genre.setTextSize(this.getResources().getDimension(R.dimen.genre_item));
        genre.setTextColor(this.getResources().getColor(R.color.black));

        return genre;
    }

}
