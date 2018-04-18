package mad24.polito.it.registrationmail;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import mad24.polito.it.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordMailActivity extends AppCompatActivity {

    private EditText mail;
    private Button buttonReset;
    private ImageButton buttonBack;
    private FirebaseAuth auth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        //field mail
        mail = (EditText) findViewById(R.id.resetpassword_mail);

        //button reset
        buttonReset = (Button) findViewById(R.id.resetpassword_buttonReset);

        //progress bar during wait (shown when you press the buttonReset)
        progressBar = (ProgressBar) findViewById(R.id.resetpassword_progressBar);

        //get the instance of Firebase Auth
        auth = FirebaseAuth.getInstance();

        //back button
        buttonBack = (ImageButton) findViewById(R.id.resetpassword_buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //event clicking reset button
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String mailString = mail.getText().toString();

                //check email
                if(mailString.isEmpty() || !isValidEmailAddress(mailString)) {
                    TextInputLayout mailLayout = (TextInputLayout) findViewById(R.id.resetpassword_mailLayout);
                    mailLayout.setError(getString(R.string.signup_insertValidMail));
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                auth.sendPasswordResetEmail(mailString)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    progressBar.setVisibility(View.GONE);
                                    setResult(RESULT_OK);
                                    finish();

                                } else {
                                    new AlertDialog.Builder(ResetPasswordMailActivity.this)
                                            .setTitle(R.string.reset_password_failed)
                                            .setMessage(R.string.reset_password_failed_mail)
                                            .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();
                                }

                                progressBar.setVisibility(View.GONE);
                            }
                        });
            }
        });
    }

    private boolean isValidEmailAddress(String emailAddress) {
        return Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches();
    }

}
