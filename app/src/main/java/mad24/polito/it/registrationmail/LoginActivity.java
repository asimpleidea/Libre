package mad24.polito.it.registrationmail;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.registrationmail.FacebookAuthenticator;
import mad24.polito.it.R;

import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private static final int RESET_PASSWORD_ACTIVITY = 1;

    private EditText inputEmail, inputPassword;
    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private Button buttonSignup;
    private Button buttonLogin;
    private TextView textReset;

    /**
     * The facebook authenticator class
     */
    FacebookAuthenticator FBAuth = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, BooksActivity.class));
            finish();
        }

        //-----------------------------------
        //  Set up facebook log in
        //-----------------------------------

        FBAuth = new FacebookAuthenticator(getApplicationContext(), this);
        FBAuth.setButton((LoginButton) findViewById(R.id.login_button));
        FBAuth.setActionType(FacebookAuthenticator.ActionTypes.LOGIN);

        //-----------------------------------
        //  Set up Email log in
        //-----------------------------------

        setContentView(R.layout.activity_login);
        inputEmail = (EditText) findViewById(R.id.login_mail);
        inputPassword = (EditText) findViewById(R.id.login_password);
        progressBar = (ProgressBar) findViewById(R.id.login_progressBar);
        buttonSignup = (Button) findViewById(R.id.login_button_signup);
        buttonLogin = (Button) findViewById(R.id.login_button_login);
        textReset = (TextView) findViewById(R.id.login_resetpassword);

        //hide keyboard when you click away the editText
        inputEmail.setOnFocusChangeListener(eventFocusChangeListener);
        inputPassword.setOnFocusChangeListener(eventFocusChangeListener);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupMailActivity.class));
            }
        });

        textReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(LoginActivity.this, ResetPasswordMailActivity.class), RESET_PASSWORD_ACTIVITY);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailString = inputEmail.getText().toString();
                final String passwordString = inputPassword.getText().toString();
                boolean isValid = true;

                //check if email or password are empty
                if(emailString.isEmpty() ) {
                    TextInputLayout mailLayout = (TextInputLayout) findViewById(R.id.login_mailLayout);
                    mailLayout.setError(getString(R.string.signup_insertValidMail));

                    isValid = false;
                }

                if(passwordString.isEmpty() ) {
                    TextInputLayout mailLayout = (TextInputLayout) findViewById(R.id.login_passwordLayout);
                    mailLayout.setError(getString(R.string.signup_insertValidPassword));

                    isValid = false;
                }

                if(!isValid)
                    return;

                progressBar.setVisibility(View.VISIBLE);

                //authenticate user
                auth.signInWithEmailAndPassword(emailString, passwordString)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);

                                if (!task.isSuccessful()) {
                                    new AlertDialog.Builder(getApplicationContext() )
                                            .setTitle(R.string.login_login_failed)
                                            .setMessage(R.string.login_wrong_mail_pass)
                                            .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();

                                } else {
                                    Intent intent = new Intent(LoginActivity.this, BooksActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if image profile is taken by gallery
        if (requestCode == RESET_PASSWORD_ACTIVITY)
        {
            if(resultCode == RESULT_OK) {
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle(R.string.reset_password_success)
                        .setMessage(R.string.reset_password_success_mail)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }
        else
        {
            // Pass the activity result back to Facebook
            Log.d("FBB", "on result activyt");
            FBAuth.setActivityResult(requestCode, resultCode, data);
        }


    }

    View.OnFocusChangeListener eventFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };
}

