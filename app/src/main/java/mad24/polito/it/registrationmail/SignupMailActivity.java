package mad24.polito.it.registrationmail;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.EditProfileActivity;
import mad24.polito.it.MyFileContentProvider;
import mad24.polito.it.PermissionManager;
import mad24.polito.it.PlaceArrayAdapter;
import mad24.polito.it.R;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SignupMailActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {
    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    private EditText mail;
    private EditText password;
    private EditText name;
    private AutoCompleteTextView city;
    private EditText phone;
    private EditText bio;

    private Button buttonSignup;
    private ImageButton buttonBack;
    private ProgressBar progressBar;

    private FirebaseAuth auth;

    private static final String LOG_TAG = "SignupMailActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private AutoCompleteTextView mAutocompleteTextView;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    //variables for profile image management
    private String userChoosenTask;
    private boolean isPhoto = false;
    Uri uri = null;
    Bitmap newProfileImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_mail);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) findViewById(mad24.polito.it.R.id.signupMail_imageProfile);

        //get EditText of fields
        mail = (EditText) findViewById(R.id.signupMail_mail);
        password = (EditText) findViewById(R.id.signupMail_password);
        name = (EditText) findViewById(R.id.signupMail_name);
        city = (AutoCompleteTextView) findViewById(R.id.signupMail_city);
        phone = (EditText) findViewById(R.id.signupMail_phone);
        bio = (EditText) findViewById(R.id.signupMail_bio);

        //button to signup
        buttonSignup = (Button) findViewById(R.id.signupMail_buttonSignup);

        progressBar = (ProgressBar) findViewById(R.id.signupMail_progressBar);

        //Get Firebase instance
        auth = FirebaseAuth.getInstance();

        //hide keyboard when you click away the editText
        mail.setOnFocusChangeListener(eventFocusChangeListener);
        password.setOnFocusChangeListener(eventFocusChangeListener);
        name.setOnFocusChangeListener(eventFocusChangeListener);
        city.setOnFocusChangeListener(eventFocusChangeListener);
        phone.setOnFocusChangeListener(eventFocusChangeListener);
        bio.setOnFocusChangeListener(eventFocusChangeListener);

        //set default image
        Drawable d = getResources().getDrawable(mad24.polito.it.R.drawable.unknown_user);
        imageProfile.setImageDrawable(d);
        uri = Uri.parse("android.resource://"+ getApplicationContext().getPackageName() +"/drawable/unknown_user.png");

        //back button
        buttonBack = (ImageButton) findViewById(R.id.signupMail_buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //select image if profile image is clicked
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }

        });
        FloatingActionButton photoButton = (FloatingActionButton) findViewById(mad24.polito.it.R.id.signupMail_photoButton);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }

        });

        buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isValid = true;

                //get texts on EditTexts
                final String mailString = mail.getText().toString();
                final String passwordString = password.getText().toString();
                final String nameString = name.getText().toString();
                final String cityString = city.getText().toString();
                final String phoneString = phone.getText().toString();
                final String bioString = bio.getText().toString();

                //check email
                if(mailString.isEmpty() || !isValidEmailAddress(mailString)) {
                    TextInputLayout mailLayout = (TextInputLayout) findViewById(R.id.signupMail_mailLayout);
                    mailLayout.setError(getString(R.string.signup_insertValidMail));

                    isValid = false;
                }

                //check password
                if(passwordString.length() < 6) {
                    TextInputLayout passwordLayout = (TextInputLayout) findViewById(R.id.signupMail_passwordLayout);
                    passwordLayout.setError(getString(R.string.signup_insertValidPassword));

                    isValid = false;
                }

                //check name
                if(nameString.length() < 2) {
                    TextInputLayout nameLayout = (TextInputLayout) findViewById(R.id.signupMail_nameLayout);
                    nameLayout.setError(getString(R.string.signup_insertValidName));

                    isValid = false;
                }

                //check city
                if(cityString.length() < 2) {
                    TextInputLayout cityLayout = (TextInputLayout) findViewById(R.id.signupMail_cityLayout);
                    cityLayout.setError(getString(R.string.signup_insertValidCity));

                    isValid = false;
                }

                //check phone number (not mandatory)
                if(!phoneString.isEmpty() && !isValidPhoneNumber(phoneString) ) {
                    TextInputLayout phoneLayout = (TextInputLayout) findViewById(R.id.signupMail_phoneLayout);
                    phoneLayout.setError(getString(R.string.signup_insertValidPhone));

                    isValid = false;
                }

                //bio is not mandatory
                //favourite genres are not mandatory

                //if all checks are positive
                if(isValid) {
                    if(!isOnline()) {
                        new AlertDialog.Builder(SignupMailActivity.this)
                                .setTitle(R.string.signup_no_internet_connection)
                                .setMessage(R.string.signup_internet_retry)
                                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                        return;
                    }

                    Log.e("state", "ISONLINE: "+isOnline());


                    progressBar.setVisibility(View.VISIBLE);
                    //sign up
                    auth.createUserWithEmailAndPassword(mailString, passwordString)
                            .addOnCompleteListener(SignupMailActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);

                                    if (!task.isSuccessful()) {
                                        //if duplicated email
                                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                            showDialog(getResources().getString(R.string.signup_mail_duplicated),
                                                    getResources().getString(R.string.signup_insert_another_mail) );
                                        } else {
                                            //if generic error
                                            showDialog(getResources().getString(R.string.signup_error),
                                                    getResources().getString(R.string.signup_retry) );
                                        }
                                    } else {
                                        FirebaseUser user = auth.getCurrentUser();
                                        Uri downloadUrl = Uri.parse("");

                                        //load image profile in Firebase Storage
                                        /*try {
                                            FirebaseStorage storage = FirebaseStorage.getInstance();

                                            StorageReference storageRef = storage.getReference().child("profile_pictures").child(user.getUid()+".jpg");

                                            Uri file = Uri.fromFile(new File(uri.toString()));
                                            UploadTask uploadTask = storageRef.putFile(file);

                                            // Register observers to listen for when the download is done or if it fails
                                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception exception) {
                                                    showDialog(getResources().getString(R.string.signup_error),
                                                            getResources().getString(R.string.signup_retry) );
                                                }
                                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                    FirebaseUser user = auth.getCurrentUser();
                                                    Uri downloadUrl = Uri.parse("");

                                                    //taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                                    downloadUrl = taskSnapshot.getDownloadUrl();

                                                    //add new user in Firebase Database
                                                    try {
                                                        DatabaseReference myDatabase = FirebaseDatabase.getInstance().getReference();
                                                        myDatabase.child("users").child(user.getUid() )
                                                                .setValue(new UserMail(mailString, nameString, cityString, phoneString, bioString));
                                                    } catch (Exception e) {
                                                        showDialog(getResources().getString(R.string.signup_error),
                                                                getResources().getString(R.string.signup_retry) );

                                                        if(auth.getCurrentUser() != null) {
                                                            user = auth.getCurrentUser();
                                                            auth.signOut();
                                                            user.delete();
                                                        }

                                                        return;
                                                    }

                                                    startActivity(new Intent(SignupMailActivity.this, BooksActivity.class));
                                                    finish();
                                                }
                                            });

                                        } catch (Exception e) {
                                            //if image profile saving fails
                                            showDialog(getResources().getString(R.string.signup_error),
                                                    getResources().getString(R.string.signup_retry) );

                                            if(auth.getCurrentUser() != null) {
                                                user = auth.getCurrentUser();
                                                auth.signOut();
                                                user.delete();
                                            }

                                            return;
                                        }*/

                                        //add new user in Firebase Database
                                        try {
                                            DatabaseReference myDatabase = FirebaseDatabase.getInstance().getReference();
                                            myDatabase.child("users").child(user.getUid() )
                                                    .setValue(new UserMail(mailString, nameString, cityString, phoneString, bioString));
                                        } catch (Exception e) {
                                            showDialog(getResources().getString(R.string.signup_error),
                                                    getResources().getString(R.string.signup_retry) );

                                            if(auth.getCurrentUser() != null) {
                                                user = auth.getCurrentUser();
                                                auth.signOut();
                                                user.delete();
                                            }

                                            return;
                                        }

                                        startActivity(new Intent(SignupMailActivity.this, BooksActivity.class));
                                        finish();
                                    }
                                }
                            });
                }
            }
        });

        //enable city suggestions
        mGoogleApiClient = new GoogleApiClient.Builder(SignupMailActivity.this)
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

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private boolean isValidEmailAddress(String emailAddress) {
        return Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return Patterns.PHONE.matcher(phoneNumber).matches();
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(SignupMailActivity.this)
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

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if(netInfo != null) {
                if(netInfo.getState() == NetworkInfo.State.CONNECTED)
                    return true;
            }
        }
        return false;
    }

    private void selectImage() {
        //get string for AlertDialog options
        final String optionCamera = getResources().getString(R.string.dialog_camera);
        final String optionLibrary = getResources().getString(R.string.dialog_library);
        final String optionCancel = getResources().getString(R.string.dialog_cancel);

        final CharSequence[] items = {optionCamera, optionLibrary, optionCancel};

        //create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupMailActivity.this);
        //builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals(optionCamera)) {
                    boolean permissionCamera = PermissionManager.checkPermissionCamera(SignupMailActivity.this);

                    userChoosenTask ="Take Photo";
                    if(permissionCamera)
                        cameraIntent();
                } else if (items[item].equals(optionLibrary)) {
                    boolean permissionRead = PermissionManager.checkPermissionRead(SignupMailActivity.this);

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i("state", "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if(uri != null)
            outState.putString("profileImageURI", uri.toString());

        outState.putBoolean("isPhoto", isPhoto);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i("state", "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);

        //get resources
        uri = Uri.parse(savedInstanceState.getString("profileImageURI"));
        isPhoto = savedInstanceState.getBoolean("isPhoto");

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

}

