package com.example.elisl.mylab1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.facebook.login.widget.LoginButton;

public class SignUpActivity extends AppCompatActivity {

    FacebookAuthenticator FBAuth = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //-----------------------------------
        //  Set up facebook sign up
        //-----------------------------------

        FBAuth = new FacebookAuthenticator(getApplicationContext(), this);
        FBAuth.setButton((LoginButton) findViewById(R.id.login_button));
        FBAuth.setActionType(FacebookAuthenticator.ActionTypes.SIGNUP);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to Facebook
        FBAuth.setActivityResult(requestCode, resultCode, data);
    }

}
