package com.example.elisl.mylab1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

public class SignUpActivity extends AppCompatActivity {

    FacebookAuthenticator FBAuth = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        FBAuth = new FacebookAuthenticator(getApplicationContext(), this);
        FBAuth.setButton((LoginButton) findViewById(R.id.login_button));
        FBAuth.setActionType(FacebookAuthenticator.ActionTypes.SIGNUP);

        //  Get the callback manager
        /*Manager = CallbackManager.Factory.create();

        //  Get the login button
        LoginButton button = (LoginButton) findViewById(R.id.login_button);
        button.setReadPermissions("email", "public_profile");

        //  What to do with the button?
        button.registerCallback(Manager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult)
                    {
                        Log.d("FBLOGIN", "Successful!");

                        //  Do something with the access token that we got from FB
                        onFacebookSignup(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel()
                    {
                        Log.d("FBLOGIN", "User cancelled operation");
                        Toast.makeText(getApplicationContext(), "Cancel!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException error)
                    {
                        Log.d("FBLOGIN", "An error occurred while getting user data");
                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                    }
                }
        );*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to Facebook
        FBAuth.setActivityResult(requestCode, resultCode, data);
    }

    /**
     * Called when facebook login is finished (successfully, of course)
     * @param token the token returned from facebook
     */
    protected void onFacebookLogin(AccessToken token)
    {

    }

    /**
     * Called when facebook signup is finished
     * @param token the access token return from facebook
     */
    protected void onFacebookSignup(AccessToken token)
    {
        GraphRequest me = GraphRequest.newMeRequest(token, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response)
            {
                Log.d("FBLOGIN", "newMeRequest is finished with status: " + response.getRawResponse());

                //  200 OK?
                if(response.getError() != null)
                {
                    Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject data = response.getJSONObject();
                try
                {
                    Log.d("FBLOGIN", "name: " + data.getString("name"));
                    Log.d("FBLOGIN", "first_name: " + data.getString("first_name"));
                    Log.d("FBLOGIN", "last_name: " + data.getString("last_name"));
                    Log.d("FBLOGIN", "gender: " + data.getString("gender"));
                    Log.d("FBLOGIN", "locale: " + data.getString("locale"));
                    Log.d("FBLOGIN", "timezone: " + data.getString("timezone"));
                    Log.d("FBLOGIN", "verified: " + data.getString("verified"));
                    Log.d("FBLOGIN", "email: " + data.getString("email"));
                }
                catch(JSONException j)
                {
                    Log.e("FBLOGIN", "Excption occurred");
                }
            }
        });

        //  Query parameters for the request
        Bundle params = new Bundle();
        params.putString("fields", "name, first_name, last_name, gender, locale, picture, timezone, verified, email");
        me.setParameters(params);
        me.executeAsync();
    }
}
