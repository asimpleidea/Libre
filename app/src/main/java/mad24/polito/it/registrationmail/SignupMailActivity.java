package mad24.polito.it.registrationmail;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import mad24.polito.it.R;
import mad24.polito.it.ShowProfileActivity;
import mad24.polito.it.registrationmail.User;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupMailActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

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
    private SignupMail_PlaceArrayAdapter mPlaceArrayAdapter;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_mail);

        mail = (EditText) findViewById(R.id.signupMail_mail);
        password = (EditText) findViewById(R.id.signupMail_password);
        name = (EditText) findViewById(R.id.signupMail_name);
        city = (AutoCompleteTextView) findViewById(R.id.signupMail_city);
        phone = (EditText) findViewById(R.id.signupMail_phone);
        bio = (EditText) findViewById(R.id.signupMail_bio);

        buttonSignup = (Button) findViewById(R.id.signupMail_buttonSignup);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //Get Firebase instance
        auth = FirebaseAuth.getInstance();

        //hide keyboard when you click away the editText
        mail.setOnFocusChangeListener(eventFocusChangeListener);
        password.setOnFocusChangeListener(eventFocusChangeListener);
        name.setOnFocusChangeListener(eventFocusChangeListener);
        city.setOnFocusChangeListener(eventFocusChangeListener);
        phone.setOnFocusChangeListener(eventFocusChangeListener);
        bio.setOnFocusChangeListener(eventFocusChangeListener);

        //back button
        buttonBack = (ImageButton) findViewById(R.id.signupMail_buttonBack2);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isValid = true;

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
                                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                            new AlertDialog.Builder(SignupMailActivity.this)
                                                    .setTitle(R.string.signup_mail_duplicated)
                                                    .setMessage(R.string.signup_insert_another_mail)
                                                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .show();
                                        } else {
                                            new AlertDialog.Builder(SignupMailActivity.this)
                                                    .setTitle(R.string.signup_error)
                                                    .setMessage(R.string.signup_retry)
                                                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .show();
                                        }
                                    } else {
                                        DatabaseReference myDatabase = FirebaseDatabase.getInstance().getReference();

                                        try {
                                            String mailSatinized = mailString;//.replaceAll("\\.", "_");

                                            myDatabase.child("users").child(mailSatinized)
                                                    .setValue(new User(mailString, nameString, cityString, phoneString, bioString));
                                        } catch (Exception e) {
                                            new AlertDialog.Builder(SignupMailActivity.this)
                                                    .setTitle(R.string.signup_error)
                                                    .setMessage(R.string.signup_retry)
                                                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .show();

                                            auth.getCurrentUser().delete();
                                            auth.signOut();
                                            return;
                                        }

                                        startActivity(new Intent(SignupMailActivity.this, ShowProfileActivity.class));
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
        mPlaceArrayAdapter = new SignupMail_PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
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


    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final SignupMail_PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
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

}

