package com.example.elisl.mylab1;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {
    private int PICK_IMAGE_REQUEST = 1;

    Toolbar toolbar;

    TextView saveText;
    ImageView cancelImage;

    ImageView imageProfile;

    EditText name;
    EditText mail;
    EditText bio;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Bitmap newProfileImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        saveText = (TextView) findViewById(R.id.saveEdit);
        cancelImage = (ImageView) findViewById(R.id.cancelEdit);

        //image profile
        imageProfile = (ImageView) findViewById(R.id.editImageProfile);

        //get edit fields
        name = (EditText) findViewById(R.id.editName);
        mail = (EditText) findViewById(R.id.editMail);
        bio = (EditText) findViewById(R.id.editBio);

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
        if (str != null) {
            imageProfile.setImageURI(Uri.fromFile(new File(str)) );
        } else {
            //default image
            Drawable d = getResources().getDrawable(R.drawable.unknown_user);
            imageProfile.setImageDrawable(d);
        }

        //save changes if "Save" is pressed and load showProfile
        saveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = prefs.edit();

                String newName = name.getText().toString();
                String newMail = mail.getText().toString();
                String newBio = bio.getText().toString();

                //store new image profile
                String pathProfileImage = new String();
                if(newProfileImage != null)
                    pathProfileImage = saveToInternalStorage(newProfileImage);

                // Save strings in SharedPref
                editor.putString("profileName", newName);
                editor.putString("profileMail", newMail);
                editor.putString("profileBio", newBio);
                if(newProfileImage != null)
                    editor.putString("profileImage", pathProfileImage);

                editor.commit();

                //create activity show profile
                Intent intent = new Intent(getApplicationContext(), ShowProfileActivity.class);
                startActivity(intent);
            }
        });

        //don't save changes and load showProfile
        cancelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ShowProfileActivity.class);
                startActivity(intent);
            }
        });

        //select image if image profile is clicked
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }

        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));
                newProfileImage = bitmap;
                imageProfile.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        String imageName = "profile.jpg";

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory, imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath() +"/"+ imageName;
    }
}
