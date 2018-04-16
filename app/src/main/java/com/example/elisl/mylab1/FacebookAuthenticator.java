package com.example.elisl.mylab1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
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

import org.json.JSONException;
import org.json.JSONObject;


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
    /**
     * The action types
     */
    public enum ActionTypes {UNKNOWN, SIGNUP, LOGIN};

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
     * The RealTime Database
     */
    private DatabaseReference DB = null;

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
    }

    /**
     * Set the button
     * @param button the button
     */
    public void setButton(LoginButton button)
    {
        Button = button;

        //  Set the callback
        Button.registerCallback(Manager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult)
                    {
                        Log.d("FBLOGIN", "Successful!");
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
                        Toast.makeText(context, context.getResources().getText(R.string.fb_error_get_me), Toast.LENGTH_LONG).show();
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
    }

    /**
     * This gets called when the login/signup procedure is concluded
     * @param token the access token from facebook
     */
    protected void onFinish(AccessToken token)
    {
        Log.d("FBLOGIN", "onFinish called!");

        if(Type == ActionTypes.UNKNOWN) return;

        //---------------------------------
        //  Sign up
        //---------------------------------

        if(Type == ActionTypes.SIGNUP)
        {
            signMeIn(token);
        }
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
                Log.d("FBLOGIN", "newMeRequest is finished with status: " + response.getRawResponse());

                //  200 OK?
                if(response.getError() != null)
                {
                    Toast.makeText(context, context.getResources().getText(R.string.fb_error_get_me), Toast.LENGTH_SHORT).show();
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

        //  ... and the user in
        FireAuth.signInWithCredential(credential).addOnCompleteListener(CurrentActivity,
                new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //  Get the user logged
                            final FirebaseUser logged = FirebaseAuth.getInstance().getCurrentUser();

                            //----------------------------------
                            //  User already exists?
                            //----------------------------------

                            DB.child("member").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    //  No need to do anything else then
                                    if (dataSnapshot.hasChild(logged.getUid()))
                                    {
                                        Toast.makeText(context, "User already exists, just logging", Toast.LENGTH_SHORT).show();
                                       // CurrentActivity.finish();
                                        return;
                                    }

                                    //----------------------------------
                                    //  Create new user
                                    //----------------------------------

                                    User u = new User();
                                    try {
                                        u.setEmail(o.getString("email"));
                                        u.setGender(o.getString("gender"));
                                        u.setLocale(o.getString("locale"));
                                        u.setName(o.getString("name"));
                                        u.setTimezone(o.getInt("timezone"));

                                        //  Finally, store user to DB!
                                        DB.child("member")
                                                .child(logged.getUid())
                                                .setValue(u, new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference)
                                                    {
                                                        //CurrentActivity.finish();
                                                        Toast.makeText(context, "Thanks for joining in! Happy to have you on board!", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } catch (JSONException j) {
                                        //  Something happened? Then delete the user!
                                        FirebaseAuth.getInstance().getCurrentUser().delete()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Log.d("FBLOGIN", "User account deleted.");
                                                            Toast.makeText(context, context.getResources().getText(R.string.fb_error_get_me), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError)
                                {

                                }
                            });
                        }
                    }
                }
        );
    }
}