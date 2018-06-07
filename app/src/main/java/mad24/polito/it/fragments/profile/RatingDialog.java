package mad24.polito.it.fragments.profile;

import android.app.Activity;
import android.app.Dialog;

import mad24.polito.it.EditProfileActivity;
import mad24.polito.it.models.Rating;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;

public class RatingDialog extends Dialog {
    private static final String FIREBASE_DATABASE_LOCATION_BORROWINGS = BooksActivity.FIREBASE_DATABASE_LOCATION_BORROWINGS;
    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;

    public Activity c;
    public Dialog dialog;

    private RatingBar ratingBar;
    private TextView description;
    private EditText comment;
    private Button negativeButton;
    private Button positiveButton;

    private String bookBorrowingId;
    private String borrowerOrOwner;
    private String bookId;


    public RatingDialog(Activity a, String bookBorrowingId, String borrowerOrOwner, String bookId) {
        super(a);

        this.c = a;

        this.bookBorrowingId = bookBorrowingId;

        if(borrowerOrOwner.equals("borrower"))
            this.borrowerOrOwner = "borrower_rating";
        else
            this.borrowerOrOwner = "owner_rating";

        this.bookId = bookId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rating_dialog);

        ratingBar = (RatingBar) findViewById(R.id.ratingDialog_rating_bar);
        description = (TextView) findViewById(R.id.ratingDialog_description);
        comment = (EditText) findViewById(R.id.ratingDialog_comment);
        positiveButton = (Button) findViewById(R.id.ratingDialog_positiveButton);
        negativeButton = (Button) findViewById(R.id.ratingDialog_negativeButton);

        //3 stars by default
        ratingBar.setRating(3);

        //max 5 stars
        ratingBar.setMax(5);

        //if click stars --> change description
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
           @Override
           public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
               if(fromUser) {
                   switch ((int)rating) {
                       case 1:
                           description.setText(R.string.ratingDialog_1star);
                           break;
                       case 2:
                           description.setText(R.string.ratingDialog_2stars);
                           break;
                       case 3:
                           description.setText(R.string.ratingDialog_3stars);
                           break;
                       case 4:
                           description.setText(R.string.ratingDialog_4stars);
                           break;
                       case 5:
                           description.setText(R.string.ratingDialog_5stars);
                           break;
                       default:
                           description.setText(R.string.ratingDialog_3stars);
                           break;
                   }
               }
           }
        });

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commentText = new String("");

                if(comment.getText().toString().isEmpty())
                    commentText = null;
                else
                    commentText = comment.getText().toString();


                Task task = FirebaseDatabase.getInstance().getReference()
                        .child(FIREBASE_DATABASE_LOCATION_BORROWINGS)
                        .child(bookBorrowingId)
                        .child(borrowerOrOwner)
                        .setValue(new Rating((int)ratingBar.getRating(), commentText) );

                task.addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        showThanksDialog(c.getResources().getString(R.string.ratingDialog_thanks) );
                        dismiss();
                    }
                });

                task.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showThanksDialog(c.getResources().getString(R.string.ratingDialog_thanks) );
                        dismiss();
                    }
                });
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(borrowerOrOwner.equals("borrower_rating")) {
                    Task task = FirebaseDatabase.getInstance().getReference()
                            .child(FIREBASE_DATABASE_LOCATION_USERS)
                            .child(FirebaseAuth.getInstance().getUid())
                            .child("books_to_rate")
                            .child(bookId)
                            .removeValue();

                    task.addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                            dismiss();
                        }
                    });

                    task.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dismiss();
                        }
                    });
                } else {
                    dismiss();
                }
            }
        });
    }

    private void showThanksDialog(String title) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
