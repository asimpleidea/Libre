package mad24.polito.it;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mad24.polito.it.registrationmail.LoginActivity;
import mad24.polito.it.models.UserMail;

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
    private EditText bio;
    private AutoCompleteTextView city;
    private LinearLayout genres;

    private Button btnGenre;
    private String[] genresList;                                    //all genres list
    boolean[] checkedItems;                                         //checked genres
    private ArrayList<String> books;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Bitmap newProfileImage = null;

    Uri uri = null;

    FirebaseUser userAuth;
    UserMail user;
    Integer semaphore = 0;
    Bitmap profileImageBitmap = null;

    private String userChoosenTask;
    private boolean isPhoto = false;

    private String selectedCity;
    private String idSelectedCity;

    private ProgressBar progressBar;

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
        setContentView(mad24.polito.it.R.layout.activity_edit_profile);

        //get user
        userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //check if logged, if not go to login activity
        if (userAuth == null) {
            Intent i = new Intent(getApplicationContext(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

            FragmentManager fm = getSupportFragmentManager();
            for(int j = 0; j < fm.getBackStackEntryCount(); ++j)
                fm.popBackStack();

        }

        saveText = (TextView) findViewById(mad24.polito.it.R.id.saveEdit);
        cancelImage = (TextView) findViewById(mad24.polito.it.R.id.cancelEdit);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) findViewById(mad24.polito.it.R.id.editImageProfile);

        //get edit fields
        name = (EditText) findViewById(mad24.polito.it.R.id.editName);
        phone = (EditText) findViewById(mad24.polito.it.R.id.editPhone);
        bio = (EditText) findViewById(mad24.polito.it.R.id.editBio);
        city = (AutoCompleteTextView) findViewById(mad24.polito.it.R.id.autoCompleteCity);

        //manage genres
        genres = (LinearLayout) findViewById(mad24.polito.it.R.id.edit_favourite_genres_list);
        btnGenre = (Button) findViewById(mad24.polito.it.R.id.buttonGenre);
        genresList = getResources().getStringArray(R.array.genres);
        checkedItems = new boolean[genresList.length];

        //get progressBar
        progressBar = (ProgressBar) findViewById(R.id.editprofile_progressBar);

        //get preferences
        prefs = getSharedPreferences("profile", MODE_PRIVATE);
        editor = prefs.edit();

        //get User object
        String userJson = prefs.getString("user", null);
        Gson gson = new Gson();
        user = gson.fromJson(userJson, UserMail.class);

        if(user != null) {
            name.setText(user.getName());
            phone.setText(user.getPhone());
            bio.setText(user.getBio());
            city.setText(user.getCity());
            selectedCity = user.getCity();
            idSelectedCity = user.getIdCity();

            //get favourite genres
            if(user.getGenres() == null)
                genres.addView(BuildGenreLayout(getResources().getString(R.string.noFavouriteGenreProfile) ) );
            else {
                for (Integer genreIndex : user.getGenres()) {
                    genres.addView(BuildGenreLayout(genresList[genreIndex]));
                    checkedItems[genreIndex] = true;
                }
            }
        }

        //get profile image
        String encoded = prefs.getString("profileImage", null);
        if(encoded != null && !encoded.equals("unknown")) {
            byte[] imageAsBytes = Base64.decode(encoded.getBytes(), Base64.DEFAULT);
            imageProfile.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
        } else {
            imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.unknown_user) );
        }

        //no new image is set
        uri = null;

        ArrayList<String> posted_books = null;
        books = new ArrayList<>();
        if(savedInstanceState != null)
            posted_books = (ArrayList<String>) savedInstanceState.get("books");

        if(posted_books == null) {
            // get booksID posted by the user
            Query query = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(FirebaseAuth.getInstance().getUid())
                    .child("books")
                    .orderByKey();
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot book : dataSnapshot.getChildren())
                        books.add(book.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }else{
            books.addAll(posted_books);
        }

        //save changes if "Save" is pressed and load showProfile
        saveText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                boolean isModified = false;

                //read fields
                final String newName = name.getText().toString();
                final String newCity = city.getText().toString();
                final String newPhone = phone.getText().toString();
                final String newBio = bio.getText().toString();

                //get favourite genres
                final ArrayList<Integer> newSelectedGenres = new ArrayList<Integer>();

                for (int i = 0; i < genresList.length; i++) {
                    if (checkedItems[i] == true)
                        newSelectedGenres.add(i);
                }

                //check name
                if(newName.length() < 2) {
                    showDialog(getResources().getString(R.string.invalidName),
                            getResources().getString(R.string.editprofile_insertValidName));

                    return;
                }

                //check city
                if(newCity.length() < 2) {
                    showDialog(getResources().getString(R.string.invalidCity),
                            getResources().getString(R.string.editprofile_insertValidCity));

                    return;
                }

                //check if city is selected by google suggestion
                if(!newCity.equals(selectedCity)) {
                    showDialog(getResources().getString(R.string.invalidCity),
                            getResources().getString(R.string.editprofile_selectSuggestion));

                    return;
                }

                //check phone number (not mandatory)
                if(!newPhone.isEmpty() && !isValidPhoneNumber(newPhone)) {
                    showDialog(getResources().getString(R.string.invalidPhone),
                            getResources().getString(R.string.editprofile_insertValidPhone));

                    return;
                }

                //bio is not mandatory
                //favourite genres are not mandatory

                //if some fields have been modified
                if(!newName.equals(user.getName()) || !newCity.equals(user.getCity()) || !newPhone.equals(user.getPhone()) ||
                        !newBio.equals(user.getBio()) ) {
                    isModified = true;
                } else {
                    //if past favourite genres list was empty
                    if(user.getGenres() == null || user.getGenres().size() == 0) {
                        for (int i = 0; i < genresList.length; i++) {
                            if(checkedItems[i] == true) {
                                isModified = true;
                                break;
                            }
                        }
                    } else {
                        //if past favourite genres list was not empty
                        for (int i = 0; i < genresList.length; i++) {
                            if (user.getGenres().contains(i) && checkedItems[i] == false) {
                                isModified = true;
                                break;
                            } else if (!user.getGenres().contains(i) && checkedItems[i] == true) {
                                isModified = true;
                                break;
                            }
                        }
                    }
                }

                //set semaphore
                semaphore = 0;

                if(isModified)
                    semaphore++;

                if(uri != null)
                    semaphore++;

                //if data and image are not modified
                if(uri == null && !isModified) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("modified", false);
                    resultIntent.putExtra("imageModified", false);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }

                //show progress bar
                progressBar.setVisibility(View.VISIBLE);

                //if profile image has been modified
                if(uri != null) {
                    //load image profile in Firebase Storage
                    try {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference().child("profile_pictures").child(userAuth.getUid() + ".jpg");

                        UploadTask uploadTask;
                        if (isPhoto) {
                            File f = new File(getBaseContext().getCacheDir(), "profileimage.jpg");
                            f.createNewFile();

                            //the shortest side must be 180px
                            Bitmap b = BitmapFactory.decodeFile(uri.toString());
                            float scale;
                            if(b.getWidth() > b.getHeight()) {
                                scale = (float)b.getHeight() / 180;
                            } else {
                                scale = (float)b.getWidth() / 180;
                            }

                            if(scale < 1)
                                scale = 1;

                            profileImageBitmap = Bitmap.createScaledBitmap(b, (int)((float)b.getWidth()/scale), (int)((float)b.getHeight()/scale), true);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            byte[] bitmapdata = bos.toByteArray();

                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(bitmapdata);
                            fos.flush();
                            fos.close();

                            uploadTask = storageRef.putFile(Uri.fromFile(f));
                        } else {
                            File f = new File(getBaseContext().getCacheDir(), "profileimage.jpg");
                            f.createNewFile();

                            //the shortest side must be 180px
                            Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            float scale;
                            if(b.getWidth() > b.getHeight()) {
                                scale = (float)b.getHeight() / 180;
                            } else {
                                scale = (float)b.getWidth() / 180;
                            }

                            if(scale < 1)
                                scale = 1;

                            profileImageBitmap = Bitmap.createScaledBitmap(b, (int)((float)b.getWidth()/scale), (int)((float)b.getHeight()/scale), true);
                            //profileImageBitmap = Bitmap.createScaledBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri), 200, 200, true);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            byte[] bitmapdata = bos.toByteArray();

                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(bitmapdata);
                            fos.flush();
                            fos.close();

                            uploadTask = storageRef.putFile(Uri.fromFile(f));
                        }

                        // Register observers to listen for when the download is done or if it fails
                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                progressBar.setVisibility(View.GONE);

                                showDialog(getResources().getString(R.string.signup_error),
                                        getResources().getString(R.string.signup_retry));

                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                //check if upload on database and/or storage have finished
                                synchronized (semaphore) {
                                    semaphore--;

                                    if(semaphore <= 0) {
                                        progressBar.setVisibility(View.GONE);

                                        //store bitmap in sharedPreferences
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                        byte[] b = baos.toByteArray();
                                        String encoded = Base64.encodeToString(b, Base64.DEFAULT);

                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("modified", true);
                                        resultIntent.putExtra("imageModified", true);
                                        resultIntent.putExtra("profileImage", encoded);
                                        setResult(Activity.RESULT_OK, resultIntent);
                                        finish();
                                    }
                                }
                            }
                        });

                    } catch (Exception e) {
                        progressBar.setVisibility(View.GONE);

                        //if image profile saving fails
                        showDialog(getResources().getString(R.string.signup_error),
                                getResources().getString(R.string.signup_retry));

                        return;
                    }
                }

                //if all checks are positive
                if(isModified) {
                    try {
                        //get coordinates
                        Places.GeoDataApi.getPlaceById(mGoogleApiClient, idSelectedCity)
                                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                                    @Override
                                    public void onResult(PlaceBuffer places) {
                                        if (places.getStatus().isSuccess()) {
                                            final Place myPlace = places.get(0);
                                            LatLng queriedLocation = myPlace.getLatLng();
                                            Double lat = queriedLocation.latitude;
                                            Double lon = queriedLocation.longitude;

                                            DatabaseReference myDatabase = FirebaseDatabase.getInstance().getReference();

                                            Task initTask = myDatabase.child("users").child(userAuth.getUid())
                                                    .setValue(new UserMail(userAuth.getEmail(), newName, newCity, idSelectedCity, newPhone,
                                                            newBio, newSelectedGenres, lat, lon) );

                                            initTask.addOnSuccessListener(new OnSuccessListener() {
                                                @Override
                                                public void onSuccess(Object o) {
                                                    //check if upload on database and/or storage have finished
                                                    synchronized (semaphore) {
                                                        semaphore--;

                                                        if (semaphore <= 0) {
                                                            progressBar.setVisibility(View.GONE);

                                                            String encoded = "";
                                                            if(profileImageBitmap != null) {
                                                                //store bitmap in sharedPreferences
                                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                                profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                                                byte[] b = baos.toByteArray();
                                                                encoded = Base64.encodeToString(b, Base64.DEFAULT);
                                                            }

                                                            Intent resultIntent = new Intent();
                                                            resultIntent.putExtra("modified", true);
                                                            resultIntent.putExtra("imageModified", (uri != null));
                                                            if(!encoded.equals(""))
                                                                resultIntent.putExtra("profileImage", encoded);

                                                            setResult(Activity.RESULT_OK, resultIntent);
                                                            finish();
                                                        }
                                                    }

                                                }
                                            });

                                            initTask.addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    progressBar.setVisibility(View.GONE);

                                                    //if image profile saving fails
                                                    showDialog(getResources().getString(R.string.signup_error),
                                                            getResources().getString(R.string.signup_retry));
                                                }
                                            });
                                } else {
                                    progressBar.setVisibility(View.GONE);

                                    //if image profile saving fails
                                    showDialog(getResources().getString(R.string.signup_error),
                                            getResources().getString(R.string.signup_retry));
                                }

                                places.release();
                            }
                        });
                    } catch (Exception e) {
                        progressBar.setVisibility(View.GONE);

                        //if image profile saving fails
                        showDialog(getResources().getString(R.string.signup_error),
                                getResources().getString(R.string.signup_retry));
                    }
                }
            }
        });

        //don't save changes and load showProfile
        cancelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("modified", false);
                resultIntent.putExtra("imageModified", false);
                setResult(Activity.RESULT_OK, resultIntent);
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
        FloatingActionButton photoButton = (FloatingActionButton) findViewById(mad24.polito.it.R.id.photoButton);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }

        });

        //hide keyboard if you click away
        name.setOnFocusChangeListener(eventFocusChangeListener);
        phone.setOnFocusChangeListener(eventFocusChangeListener);
        bio.setOnFocusChangeListener(eventFocusChangeListener);
        city.setOnFocusChangeListener(eventFocusChangeListenerCity);

        //set listener for genre button
        manageButtonGenre();

        //enable city suggestions
        mGoogleApiClient = new GoogleApiClient.Builder(EditProfileActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        city.setThreshold(2);

        city.setOnItemClickListener(mAutocompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_MOUNTAIN_VIEW,
                new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build());
        city.setAdapter(mPlaceArrayAdapter);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return Patterns.PHONE.matcher(phoneNumber).matches();
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

        //save uri photo/image
        if(uri != null)
            outState.putString("profileImageURI", uri.toString());

        outState.putBoolean("isPhoto", isPhoto);

        //save favourite genres
        outState.putSerializable("genres", checkedItems);

        outState.putSerializable("books", books);

        //save selected city
        outState.putString("selectedCity", selectedCity);
        outState.putString("idSelectedCity", idSelectedCity);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("state", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        //get profile image
        String imageProfileUri = savedInstanceState.getString("profileImageURI");
        if(imageProfileUri != null)
            uri = Uri.parse(imageProfileUri);

        //if photo has been changed
        if(uri != null) {
            isPhoto = savedInstanceState.getBoolean("isPhoto");

            try {
                if (isPhoto) {
                    Bitmap mBitmap = BitmapFactory.decodeFile(uri.toString());
                    newProfileImage = mBitmap;
                    imageProfile.setImageBitmap(mBitmap);
                } else {
                    File f = new File(uri.toString());
                    if (!f.exists()) {
                        //if image is saved on gallery (new image)
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        newProfileImage = bitmap;
                        imageProfile.setImageBitmap(bitmap);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //get favourite genres
        checkedItems = (boolean[]) savedInstanceState.getSerializable("genres");

        //show favourite genres
        genres.removeAllViews();
        boolean noGenres = true;
        for (int i = 0; i < genresList.length; i++) {
            if(checkedItems[i] == true) {
                genres.addView(BuildGenreLayout(genresList[i]));
                noGenres = false;
            }
        }

        if(noGenres)
            genres.addView(BuildGenreLayout(getResources().getString(R.string.noFavouriteGenreProfile) ) );

        //get selected city
        selectedCity = savedInstanceState.getString("selectedCity");
        idSelectedCity = savedInstanceState.getString("idSelectedCity");
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
                        mad24.polito.it.R.string.error_camera, Toast.LENGTH_LONG)
                        .show();
                return;
            }

            Bitmap mBitmap = BitmapFactory.decodeFile(out.getAbsolutePath());

            //set new profile image = shot photo
            newProfileImage = mBitmap;
            imageProfile.setImageBitmap(mBitmap);

            uri = Uri.parse(out.getAbsolutePath());
            Log.d("absolutepath", uri.toString());
            isPhoto = true;
        }
    }

    private void selectImage() {
        //get string for AlertDialog options
        final String optionCamera = getResources().getString(mad24.polito.it.R.string.dialog_camera);
        final String optionLibrary = getResources().getString(mad24.polito.it.R.string.dialog_library);
        final String optionCancel = getResources().getString(mad24.polito.it.R.string.dialog_cancel);

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
                            mad24.polito.it.R.string.deny_permission_read, Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case PermissionManager.PERMISSION_REQUEST_CAMERA:
                // Request for camera permission.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraIntent();
                } else {
                    Toast.makeText(getBaseContext(),
                            mad24.polito.it.R.string.deny_permission_camera, Toast.LENGTH_LONG)
                            .show();
                }
                break;

        }

    }


    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            /*Log.i(LOG_TAG, "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(LOG_TAG, "Fetching details for ID: " + item.placeId);*/

            selectedCity = item.description.toString();
            idSelectedCity = item.placeId.toString();

            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        }
    };

    /*private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
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
            LatLng latlng = place.getLatLng();
            Log.v("Latitude is", "" + latlng.latitude);
            Log.v("Longitude is", "" + latlng.longitude);
        }
    };*/

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(LOG_TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this, mad24.polito.it.R.string.google_connection_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(LOG_TAG, "Google Places API connection suspended.");
    }

    private TextView BuildGenreLayout(final String name) {
        TextView genre = new TextView(getApplicationContext());
        genre.setText(name);
        genre.setTextSize(this.getResources().getDimension(mad24.polito.it.R.dimen.genre_item));
        genre.setTextColor(this.getResources().getColor(mad24.polito.it.R.color.black));

        return genre;
    }

    private void manageButtonGenre() {
        btnGenre.setOnClickListener(new View.OnClickListener() {
            boolean[] oldSelectedGenres = new boolean[genresList.length];

            @Override
            public void onClick(View view) {
                oldSelectedGenres = new boolean[genresList.length];

                for(int i=0; i < genresList.length; i++) {
                    if(checkedItems[i] == true)
                        oldSelectedGenres[i] = true;
                }

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(EditProfileActivity.this);
                mBuilder.setTitle(R.string.title_genre_alertdialog);
                mBuilder.setMultiChoiceItems(genresList, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    //called every time you click a checkbox
                    public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                    }
                });

                mBuilder.setCancelable(false);

                //called when you click "ok" button
                mBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        genres.removeAllViews();

                        for (int i = 0; i < genresList.length; i++) {
                            if(checkedItems[i] == true)
                                genres.addView(BuildGenreLayout(genresList[i]));
                        }

                    }
                });

                mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        for(int i=0; i < genresList.length; i++) {
                            checkedItems[i] = oldSelectedGenres[i];
                        }

                        dialogInterface.dismiss();
                    }
                });

                final AlertDialog mDialog = mBuilder.create();

                mDialog.show();
            }
        });
    }

    //hide keyboard when you click away the EditText
    View.OnFocusChangeListener eventFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };

    //hide keyboard when you click away the EditText
    View.OnFocusChangeListener eventFocusChangeListenerCity = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                String insertedCity = city.getText().toString();
                ArrayList<PlaceArrayAdapter.PlaceAutocomplete> list = mPlaceArrayAdapter.getListAutocomplete();
                if(list.size() > 0) {
                    for (PlaceArrayAdapter.PlaceAutocomplete element : list) {
                        if (element.description.equals(insertedCity)) {
                            selectedCity = element.description.toString();
                            idSelectedCity = element.placeId.toString();
                            return;
                        }
                    }

                    selectedCity = list.get(0).description.toString();
                    idSelectedCity = list.get(0).placeId.toString();
                    city.setText(list.get(0).description);
                }
            }
        }
    };



    private void showDialog(String title, String message) {
        new AlertDialog.Builder(EditProfileActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}


