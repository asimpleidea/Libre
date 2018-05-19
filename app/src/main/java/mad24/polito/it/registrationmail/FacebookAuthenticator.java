package mad24.polito.it.registrationmail;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AlertDialogLayout;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;
import mad24.polito.it.locator.Locator;
import mad24.polito.it.locator.LocatorEventsListener;
import mad24.polito.it.models.UserMail;


/**
 * Facebook Authenticator
 *
 * This class handles login and sign up through Facebook
 * Author: Elis Lulja
 * Last Revision: April 16 2018, 16:39
 * To-DOs written in the source.
 */
public class FacebookAuthenticator
{
    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;

    /**
     * The action types
     */
    public enum ActionTypes {UNKNOWN, SIGNUP, LOGIN, LOGOUT};

    /**
     * The button
     */
    private LoginButton Button = null;

    /**
     * The callback manager.
     */
    private CallbackManager Manager = null;

    /**
     * The current context
     * @param context
     */
    private Context context = null;

    /**
     * What are we doing?
     */
    private ActionTypes Type = ActionTypes.UNKNOWN;

    /**
     * The activity I have been called from
     */
    private Activity CurrentActivity = null;

    /**
     * The FireBase Authenticator
     */
    private FirebaseAuth FireAuth = null;

    /**
     * The Firebase Storage
     */
    private StorageReference Storage = null;

    /**
     * The RealTime Database
     */
    private DatabaseReference DB = null;

    private AlertDialog.Builder DialogBuilder = null;

    private AlertDialog WaitingDialog = null;

    private AlertDialog ErrorDialog = null;

    /**
     * The constructor
     * @param context
     */
    FacebookAuthenticator(Context context, Activity currentActivity)
    {
        this.context = context;
        Manager = CallbackManager.Factory.create();
        CurrentActivity = currentActivity;
        FireAuth = FirebaseAuth.getInstance();
        DB = FirebaseDatabase.getInstance().getReference();
        Storage = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Set the button
     * @param button the button
     */
    public void setButton(LoginButton button)
    {
        Button = button;
        Button.setReadPermissions("email", "public_profile");

        //  Set the callback
        Button.registerCallback(Manager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult)
                    {
                        onFinish(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel()
                    {
                        Log.d("FBLOGIN", "Operation cancelled by user");
                    }

                    @Override
                    public void onError(FacebookException error)
                    {
                        Log.d("FBLOGIN", "An error occurred while getting user data");
                        Toast.makeText(context, context.getResources().getString(R.string.fb_error_get_me), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * Set the action type (what to do when button is clicked
     * @param type the type. Permitted: login, signup
     */
    public void setActionType(ActionTypes type)
    {
        Type = type;

        String text = null;

        switch (type)
        {
            case LOGIN:
                text = context.getResources().getString(R.string.login_with_fb);
                break;

            case SIGNUP:
                text = context.getResources().getString(R.string.signup_with_fb);
                break;

            case LOGOUT:
                text = context.getResources().getString(R.string.logout_from_fb);
                break;

        }

        //Button.setText(text);
    }

    /**
     * This gets called when the login/signup procedure is concluded
     * @param token the access token from facebook
     */
    protected void onFinish(AccessToken token)
    {
        if(Type == ActionTypes.UNKNOWN) return;

        //---------------------------------
        //  Sign up
        //---------------------------------

        if(Type == ActionTypes.SIGNUP || Type == ActionTypes.LOGIN)
        {
            WaitingDialog.show();
            signMeIn(token);
        }
    }

    public void setDialogBuilder(AlertDialog.Builder p)
    {
        DialogBuilder = p;

        //-------------------------------------
        //  Waiting Dialog
        //-------------------------------------

        //  Seems like that creating everything through DialogBuilder gets an error when showing another.
        //  So I have to build it like this.
        WaitingDialog = DialogBuilder.create();

        //  Create the progress bar
        ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        progress.setPadding(0,0,0,25);

        WaitingDialog.setTitle(R.string.fb_wait);
        WaitingDialog.setMessage(context.getResources().getString(R.string.fb_wait_message));
        WaitingDialog.setView(progress);

        //-------------------------------------
        //  Error Dialog
        //-------------------------------------

        ErrorDialog = DialogBuilder.create();

        ErrorDialog.setTitle(R.string.fb_error);
        ErrorDialog.setMessage(context.getResources().getString(R.string.fb_error_authenticate));
        ErrorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener()
        {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ErrorDialog.dismiss();
                    }
                });
    }

    /**
     * This gets the activity result from the HTTP request to this class.
     * @param requestCode the code of the request (from android os)
     * @param resultCode the code of the response (HTTP)
     * @param data the intent with the data
     */
    public void setActivityResult(int requestCode, int resultCode, Intent data)
    {
        Manager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Get connected user data after a successful signup
     * @param token the token from facebook
     */
    private void signMeIn(final AccessToken token)
    {
        GraphRequest me = GraphRequest.newMeRequest(token, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response)
            {

                //  200 OK?
                if(response.getError() != null)
                {
                    WaitingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface)
                        {
                            ErrorDialog.show();
                        }
                    });
                    WaitingDialog.dismiss();

                    //Toast.makeText(context, context.getResources().getText(R.string.fb_error_get_me), Toast.LENGTH_SHORT).show();
                    return;
                }

                //  Sign in the user
                signIn(token, response.getJSONObject());
            }
        });

        //  Query parameters for the request
        Bundle params = new Bundle();
        params.putString("fields", context.getResources().getString(R.string.fields));
        me.setParameters(params);
        me.executeAsync();
    }

    /**
     * Signs the user in
     * @param token the token from facebook
     * @param o the data got from facebook
     */
    private void signIn(final AccessToken token, final JSONObject o)
    {
        //  Get the credential
        final AuthCredential  credential = FacebookAuthProvider.getCredential(token.getToken());

        //  ... and log the user in
        FireAuth.signInWithCredential(credential).addOnCompleteListener(CurrentActivity,
                new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {

                        if (task.isSuccessful())
                        {
                            //  Get the user logged
                            final FirebaseUser logged = FirebaseAuth.getInstance().getCurrentUser();

                            //----------------------------------
                            //  User already exists?
                            //----------------------------------

                            DB.child(FIREBASE_DATABASE_LOCATION_USERS).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot)
                                {
                                    //  No need to do anything else then
                                    if (dataSnapshot.hasChild(logged.getUid()))
                                    {
                                        //Toast.makeText(context, "User already exists, just logging", Toast.LENGTH_SHORT).show();
                                        context.startActivity(new Intent(context, BooksActivity.class));
                                        CurrentActivity.finish();
                                        return;
                                    }

                                    //----------------------------------
                                    //  Create new user
                                    //----------------------------------

                                    //  Get the user's picture
                                    InputStream stream = null;

                                    try
                                    {
                                        //----------------------------------
                                        //  Upload the user's profile pic
                                        //----------------------------------

                                        final String pic = o.getJSONObject("picture").getJSONObject("data").getString("url");

                                        stream = new PullFBPictureStream(logged.getUid()).execute(pic).get();
                                    }
                                    catch(JSONException j)
                                    {
                                        //  Nothing, just don't save the picture
                                        Log.d("FBLOGIN", "JSONException");
                                    }
                                    catch(InterruptedException i){Log.d("FBLOGIN", "InterruptionException");}
                                    catch(ExecutionException e){Log.d("FBLOGIN", "ExecutionException");}


                                    UploadTask task = Storage.child("profile_pictures").child(logged.getUid()+".jpg").putStream(stream);
                                    task.addOnFailureListener(new OnFailureListener()
                                    {
                                        @Override
                                        public void onFailure(@NonNull Exception e)
                                        {
                                            //  Create User
                                            //Log.d("FBLOGIN", "failure");
                                            createUser(logged, o, null);
                                        }
                                    });

                                    task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                                    {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                        {
                                            //Log.d("FBLOGIN", "Success");

                                            String picUrl = taskSnapshot.getDownloadUrl() != null ? taskSnapshot.getDownloadUrl().toString() : null;
                                            createUser(logged, o, picUrl);
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError)
                                {
                                    FirebaseAuth.getInstance().signOut();
                                }
                            });
                        }
                        else FirebaseAuth.getInstance().signOut();
                    }
                }
        );
    }

    private void createUser(final FirebaseUser logged, final JSONObject o, String picture)
    {
        final UserMail u = new UserMail();

        try
        {
            //  Todo: check for email value, as Facebook not always returns it (check fb developer page)
            u.setEmail(o.getString("email"));
            u.setBio("");
            u.setCity("");
            u.setGenres(new ArrayList<Integer>());
            u.setIdCity("");
            u.setName(o.getString("name"));
            u.setPhone("");
            u.setFb(true);

            //  Get user's current location
            Locator l = new Locator(CurrentActivity, context);
            l.addListener(new LocatorEventsListener() {

                @Override
                public void onSuccess(double latitude, double longitude)
                {
                    u.setLat(latitude);
                    u.setLon(longitude);
                    pushUser(u, logged);
                }

                @Override
                public void onFailure()
                {
                    //  Set latitude and longitude of politecnico di torino
                    u.setLat(45.06249);
                    u.setLon(7.66220);
                    pushUser(u, logged);
                }

                @Override
                public void onPermissionDenied()
                {
                    //  Set latitude and longitude of politecnico di torino
                    u.setLat(45.06249);
                    u.setLon(7.66220);
                    pushUser(u, logged);
                }
            }).once();
        }
        catch(JSONException j)
        {
            onFailure(logged);
        }

    }

    private void pushUser(final UserMail u, final FirebaseUser logged)
    {
        //  Finally, store user to DB!
        DB.child(FIREBASE_DATABASE_LOCATION_USERS)
                .child(logged.getUid())
                .setValue(u, new DatabaseReference.CompletionListener()
                {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference)
                    {
                        if(databaseError != null) onFailure(logged);
                        else
                        {
                            WaitingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface)
                                {
                                        context.startActivity(new Intent(context, BooksActivity.class));
                                        CurrentActivity.finish();
                                }
                            });
                            WaitingDialog.dismiss();
                            //Toast.makeText(context, "Thanks for joining in! Happy to have you on board!", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    private void onFailure(final FirebaseUser logged)
    {
        //  Something happened? Then first sign the user out, then delete them!
        String uid = logged.getUid();
        FirebaseAuth.getInstance().signOut();

        DB.child("member").child(uid).removeValue(new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference)
            {
                WaitingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface)
                    {
                        ErrorDialog.setTitle(R.string.fb_error);
                        ErrorDialog.show();
                    }
                });
                WaitingDialog.dismiss();
                //Toast.makeText(context, context.getResources().getText(R.string.fb_error_get_me), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
