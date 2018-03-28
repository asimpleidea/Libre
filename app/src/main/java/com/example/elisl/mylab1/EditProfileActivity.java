package com.example.elisl.mylab1;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {
    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;

    Toolbar toolbar;

    TextView saveText;
    ImageView cancelImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    EditText name;
    EditText mail;
    EditText bio;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Bitmap newProfileImage = null;

    Uri uri = null;

    private String userChoosenTask;
    private boolean isPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("state", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        saveText = (TextView) findViewById(R.id.saveEdit);
        cancelImage = (ImageView) findViewById(R.id.cancelEdit);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) findViewById(R.id.editImageProfile);

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
            if(uri == null)
                uri = Uri.parse(str);
        } else {
            //default image
            Drawable d = getResources().getDrawable(R.drawable.unknown_user);
            imageProfile.setImageDrawable(d);
            if(uri == null)
                uri = Uri.parse("android.resource://"+ getApplicationContext().getPackageName() +"/drawable/unknown_user.png");
        }

        //save changes if "Save" is pressed and load showProfile
        saveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("saving", "Saving...");
                Toast.makeText(getApplicationContext(), "Saving...", Toast.LENGTH_SHORT).show();

                editor = prefs.edit();

                String newName = name.getText().toString();
                String newMail = mail.getText().toString();
                String newBio = bio.getText().toString();

                //store new image profile
                String pathProfileImage = new String();
                if(newProfileImage != null) {
                    pathProfileImage = saveToInternalStorage(newProfileImage);
                }

                // Save strings in SharedPref
                editor.putString("profileName", newName);
                editor.putString("profileMail", newMail);
                editor.putString("profileBio", newBio);
                if(newProfileImage != null) {
                    String oldImage = prefs.getString("profileImage", null);

                    editor.putString("profileImage", pathProfileImage);

                    //delete the previous profile image
                    if(oldImage != null) {
                        File file = new File(oldImage);
                        boolean deleted = file.delete();
                        Log.i("state", "Old profile image deleted: " + deleted);
                    }
                }

                Log.i("state", "content saved");

                editor.commit();


                Log.i("saving", "Saved");
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();

                /*//create activity show profile
                Intent intent = new Intent(getApplicationContext(), ShowProfileActivity.class);
                startActivity(intent);*/
                finish();
            }
        });

        //don't save changes and load showProfile
        cancelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //select image if image profile is clicked
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }

        });
    }

    @Override
    protected void onStart() {
        Log.i("state", "onStart");


        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i("state", "onResume");

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i("state", "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString("profileName", name.getText().toString());
        outState.putString("profileEmail", mail.getText().toString());
        outState.putString("profileBio", bio.getText().toString());
        if(uri != null)
            outState.putString("profileImageURI", uri.toString());

        outState.putBoolean("isPhoto", isPhoto);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("state", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        name.setText(savedInstanceState.getString("profileName"));
        mail.setText(savedInstanceState.getString("profileEmail"));
        bio.setText(savedInstanceState.getString("profileBio"));
        isPhoto = savedInstanceState.getBoolean("isPhoto");

        uri = Uri.parse(savedInstanceState.getString("profileImageURI"));

        try {
            if(isPhoto) {
                Bitmap mBitmap = BitmapFactory.decodeFile(uri.toString());
                newProfileImage = mBitmap;
                imageProfile.setImageBitmap(mBitmap);
            } else {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                newProfileImage = bitmap;
                imageProfile.setImageBitmap(bitmap);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("state", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        //if image profile is taken by gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            try {
                uri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                newProfileImage = bitmap;
                imageProfile.setImageBitmap(bitmap);

                isPhoto = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //if image profile is shot by the camera
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            File out = new File(getFilesDir(), "newImage.jpg");

            if(!out.exists()) {
                Toast.makeText(getBaseContext(),
                        R.string.error_camera, Toast.LENGTH_LONG)
                        .show();
                return;
            }

            Bitmap mBitmap = BitmapFactory.decodeFile(out.getAbsolutePath());

            //set new profile image = shot photo
            newProfileImage = mBitmap;
            imageProfile.setImageBitmap(mBitmap);

            uri = Uri.parse(out.getAbsolutePath());
            isPhoto = true;
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        String imageName = Long.toString(System.currentTimeMillis() );
        //String imageName = "profile.jpg";

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory, imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
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

    private void selectImage() {
        //get string for AlertDialog options
        final String optionCamera = getResources().getString(R.string.dialog_camera);
        final String optionLibrary = getResources().getString(R.string.dialog_library);
        final String optionCancel = getResources().getString(R.string.dialog_cancel);

        final CharSequence[] items = {optionCamera, optionLibrary, optionCancel};

        //create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
        //builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals(optionCamera)) {
                    boolean permissionCamera = PermissionManager.checkPermissionCamera(EditProfileActivity.this);

                    userChoosenTask ="Take Photo";
                    if(permissionCamera)
                        cameraIntent();
                } else if (items[item].equals(optionLibrary)) {
                    boolean permissionRead = PermissionManager.checkPermissionRead(EditProfileActivity.this);

                    userChoosenTask ="Choose from Library";
                    if(permissionRead)
                        galleryIntent();
                } else if (items[item].equals(optionCancel)) {
                    userChoosenTask ="Cancel";
                    dialog.dismiss();
                }

            }
        });

        //show the AlertDialog on the screen
        builder.show();
    }

    //intent to access the camera
    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //save photo on MyFileProvider.CONTENT_URI
        intent.putExtra(MediaStore.EXTRA_OUTPUT, MyFileContentProvider.CONTENT_URI);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    //intent to access the gallery
    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Show only images, no videos or anything else
        intent.setType("image/*");
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("state", "onRequestPermissionResult");

        switch (requestCode) {
            case PermissionManager.PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    Toast.makeText(getBaseContext(),
                        R.string.deny_permission_read, Toast.LENGTH_LONG)
                        .show();
                }
                break;

            case PermissionManager.PERMISSION_REQUEST_CAMERA:
                // Request for camera permission.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraIntent();
                } else {
                    Toast.makeText(getBaseContext(),
                        R.string.deny_permission_camera, Toast.LENGTH_LONG)
                        .show();
                }
                break;

        }

    }
}
