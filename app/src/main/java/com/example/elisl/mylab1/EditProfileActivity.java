package com.example.elisl.mylab1;

import android.app.Activity;
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
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditProfileActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {
    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;

    Toolbar toolbar;

    TextView saveText;
    TextView cancelImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    private EditText name;
    private EditText phone;
    private EditText mail;
    private EditText bio;
    private AutoCompleteTextView city;
    private LinearLayout genres;

    private Button btnGenre;
    private String[] genresList;                                    //all genres list
    boolean[] checkedItems;                                         //checked genres
    ArrayList<Integer> selectedGenres = new ArrayList<Integer>();   //favourite genres



    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Bitmap newProfileImage = null;

    Uri uri = null;

    private String userChoosenTask;
    private boolean isPhoto = false;

    private static final String LOG_TAG = "EditProfileActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private AutoCompleteTextView mAutocompleteTextView;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("state", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        saveText = (TextView) findViewById(R.id.saveEdit);
        cancelImage = (TextView) findViewById(R.id.cancelEdit);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) findViewById(R.id.editImageProfile);

        //get edit fields
        name = (EditText) findViewById(R.id.editName);
        phone = (EditText) findViewById(R.id.editPhone);
        mail = (EditText) findViewById(R.id.editMail);
        bio = (EditText) findViewById(R.id.editBio);
        city = (AutoCompleteTextView) findViewById(R.id.autoCompleteCity);

        //manage genres
        genres = (LinearLayout) findViewById(R.id.edit_favourite_genres_list);
        btnGenre = (Button) findViewById(R.id.buttonGenre);
        genresList = getResources().getStringArray(R.array.genres);
        checkedItems = new boolean[genresList.length];

        //get preferences
        prefs = getSharedPreferences("profile", MODE_PRIVATE);

        //get name if already inserted
        String str = prefs.getString("profileName", null);
        if (str != null)
            name.setText(str);

        //get phone if already inserted
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

        //get saved selectedItems
        str = prefs.getString("profileGenres", null);
        genres.removeAllViews();
        selectedGenres = new ArrayList<Integer>();

        if(str != null && !str.isEmpty()) {
            Log.i("state", "onCreate GENRES: " +str);
            String[] strArray = str.split(",");

            for(int i = 0; i < strArray.length; i++) {
                selectedGenres.add(Integer.parseInt(strArray[i]) );
                checkedItems[Integer.parseInt(strArray[i])] = true;
                genres.addView(BuildGenreLayout(genresList[Integer.parseInt(strArray[i])] ) );
            }
        }

        //save changes if "Save" is pressed and load showProfile
        saveText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!mail.getText().toString().isEmpty() && !isValidEmailAddress(mail.getText().toString())) {
                    new AlertDialog.Builder(EditProfileActivity.this)
                            .setTitle(R.string.mail_not_valid)
                            .setMessage(R.string.insert_valid_mail)
                            .setNeutralButton(R.string.ok,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else if(!phone.getText().toString().isEmpty() && !isValidPhoneNumber(phone.getText().toString())) {
                    new AlertDialog.Builder(EditProfileActivity.this)
                            .setTitle(R.string.phone_not_valid)
                            .setMessage(R.string.insert_valid_phone)
                            .setNeutralButton(R.string.ok,new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else {

                    editor = prefs.edit();

                    String newName = name.getText().toString();
                    String newPhone = phone.getText().toString();
                    String newMail = mail.getText().toString();
                    String newBio = bio.getText().toString();
                    String newCity = city.getText().toString();

                    //store new image profile
                    String pathProfileImage = new String();
                    if (newProfileImage != null) {

                        try {
                            pathProfileImage = new SaveToInternalStorage(getApplicationContext()).execute(newProfileImage).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        /*pathProfileImage = saveToInternalStorage(newProfileImage);*/
                    }

                    // Save strings in SharedPref
                    editor.putString("profileName", newName);
                    editor.putString("profilePhone", newPhone);
                    editor.putString("profileMail", newMail);
                    editor.putString("profileBio", newBio);
                    editor.putString("profileCity", newCity);
                    if (newProfileImage != null) {
                        String oldImage = prefs.getString("profileImage", null);

                        editor.putString("profileImage", pathProfileImage);

                        //delete the previous profile image
                        if (oldImage != null) {
                            File file = new File(oldImage);
                            boolean deleted = file.delete();
                            Log.i("state", "Old profile image deleted: " + deleted);
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < selectedGenres.size()-1; i++) {
                        sb.append(selectedGenres.get(i).toString() ).append(",");
                    }

                    if(selectedGenres.size() > 0) {
                        sb.append(selectedGenres.get(selectedGenres.size() - 1).toString());
                        editor.putString("profileGenres", sb.toString());
                    } else
                        editor.remove("profileGenres");

                    Log.i("state", "save button GENRES: "+sb.toString());
                    Log.i("state", "content saved");

                    editor.commit();

                    Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();

                    finish();
                }
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

        //hide keyboard if you click away
        name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        phone.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    hideKeyboard(v);
                }
            }
        });

        //hide keyboard if you click away
        mail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        //hide keyboard if you click away
        bio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        //hide keyboard if you click away
        city.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        //set listener for genre button
        manageButtonGenre();

        //enable city suggestions
        mGoogleApiClient = new GoogleApiClient.Builder(EditProfileActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        city.setThreshold(3);

        city.setOnItemClickListener(mAutocompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_MOUNTAIN_VIEW,
                new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build());
        city.setAdapter(mPlaceArrayAdapter);
    }

    private boolean isValidEmailAddress(String emailAddress) {
        String  expression="^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = emailAddress;
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String expression="^(\\+([0-9]{2,3})\\s)?[0-9\\s]{4,13}$";
        CharSequence inputStr = phoneNumber;
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        return matcher.matches();
    }

    @Override
    protected void onStart() {
        Log.i("state", "onStart");


        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i("state", "onStop");

        mGoogleApiClient.disconnect();
        super.onStop();
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
        outState.putString("profilePhone", phone.getText().toString());
        outState.putString("profileEmail", mail.getText().toString());
        outState.putString("profileBio", bio.getText().toString());
        outState.putString("profileCity", city.getText().toString());
        if(uri != null)
            outState.putString("profileImageURI", uri.toString());

        //save selected genres
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedGenres.size()-1; i++) {
            sb.append(selectedGenres.get(i).toString() ).append(",");
        }

        if(selectedGenres.size() > 0) {
            sb.append(selectedGenres.get(selectedGenres.size() - 1).toString());
            outState.putString("profileGenres", sb.toString());
        } else
            outState.remove("profileGenres");

        Log.i("state", "onSaveInstanceState GENRES: "+sb.toString());

        outState.putBoolean("isPhoto", isPhoto);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("state", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        name.setText(savedInstanceState.getString("profileName"));
        phone.setText(savedInstanceState.getString("profilePhone"));
        mail.setText(savedInstanceState.getString("profileEmail"));
        bio.setText(savedInstanceState.getString("profileBio"));
        city.setText(savedInstanceState.getString("profileCity"));
        isPhoto = savedInstanceState.getBoolean("isPhoto");

        uri = Uri.parse(savedInstanceState.getString("profileImageURI"));

        try {
            if(isPhoto) {
                Bitmap mBitmap = BitmapFactory.decodeFile(uri.toString());
                newProfileImage = mBitmap;
                imageProfile.setImageBitmap(mBitmap);
            } else {
                File f = new File(uri.toString());
                if(!f.exists() ) {
                    //if image is saved on gallery (new image)
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    newProfileImage = bitmap;
                    imageProfile.setImageBitmap(bitmap);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        //get saved selectedItems
        String str = savedInstanceState.getString("profileGenres", null);
        genres.removeAllViews();
        selectedGenres = new ArrayList<Integer>();
        checkedItems = new boolean[genresList.length];

        if(str != null) {
            if(str.isEmpty()) {
                genres.addView(BuildGenreLayout("") );
            } else {
                Log.i("state", "onRestoreInstanceState GENRES: "+str);
                String[] strArray = str.split(",");
                selectedGenres = new ArrayList<Integer>();


                for (int i = 0; i < strArray.length; i++) {
                    selectedGenres.add(Integer.parseInt(strArray[i]));
                    checkedItems[Integer.parseInt(strArray[i])] = true;
                    genres.addView(BuildGenreLayout(genresList[Integer.parseInt(strArray[i])]));
                }
            }
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

    /*private String saveToInternalStorage(Bitmap bitmapImage) {
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
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 85, fos);
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
    }*/

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

    //method to hide keyboard when you click away on the screen
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(LOG_TAG, "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(LOG_TAG, "Fetching details for ID: " + item.placeId);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
                @Override
                public void onResult(PlaceBuffer places) {
                    if (!places.getStatus().isSuccess()) {
                        Log.e(LOG_TAG, "Place query did not complete. Error: " +
                                places.getStatus().toString());
                        return;
                    }
                    // Selecting the first object buffer.
                    final Place place = places.get(0);
                    CharSequence attributions = places.getAttributions();

                    if (attributions != null) {

                    }
                }
            };

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(LOG_TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this, R.string.google_connection_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(LOG_TAG, "Google Places API connection suspended.");
    }

    private TextView BuildGenreLayout(final String name) {
        TextView genre = new TextView(getApplicationContext());
        genre.setText(name);
        genre.setTextSize(this.getResources().getDimension(R.dimen.genre_item));
        genre.setTextColor(this.getResources().getColor(R.color.black));

        return genre;
    }

    private void manageButtonGenre() {
        btnGenre.setOnClickListener(new View.OnClickListener() {
            ArrayList<Integer> oldSelectedGenres = new ArrayList<Integer>();

            @Override
            public void onClick(View view) {
                oldSelectedGenres = new ArrayList<Integer>();

                for(int i=0; i < genresList.length; i++) {
                    if(checkedItems[i] == true)
                        oldSelectedGenres.add(i);
                }

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(EditProfileActivity.this);
                mBuilder.setTitle(R.string.title_genre_alertdialog);
                mBuilder.setMultiChoiceItems(genresList, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    //called every time you click a checkbox
                    public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                        if (isChecked) {
                            if(!selectedGenres.contains(position))
                                selectedGenres.add(position);
                        } else {
                            selectedGenres.remove((Integer.valueOf(position)));
                        }
                    }
                });

                mBuilder.setCancelable(false);

                //called when you click "ok" button
                mBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        genres.removeAllViews();

                        for (int i = 0; i < selectedGenres.size(); i++) {
                            String item = genresList[selectedGenres.get(i)];

                            genres.addView(BuildGenreLayout(item));
                        }
                    }
                });

                mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        for (int i=0; i < genresList.length; i++) {
                            checkedItems[i] = false;
                        }

                        for(int i=0; i < oldSelectedGenres.size(); i++) {
                            checkedItems[oldSelectedGenres.get(i)] = true;
                        }

                        selectedGenres = new ArrayList<Integer>(oldSelectedGenres);

                        dialogInterface.dismiss();
                    }
                });

                final AlertDialog mDialog = mBuilder.create();

                mDialog.show();
            }
        });
    }
}

