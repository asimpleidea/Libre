package mad24.polito.it.fragments.viewbook;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.LinkedList;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;
import mad24.polito.it.models.Comment;
import mad24.polito.it.models.Rating;

public class CommentDialog extends Dialog {
    public static final String FIREBASE_DATABASE_LOCATION_COMMENTS = BooksActivity.FIREBASE_DATABASE_LOCATION_COMMENTS;

    public Activity c;
    public Dialog dialog;

    private Button neutralButton;
    private RecyclerView rv;
    private TextView title;
    private TextView noRatings;

    private String userId;
    private String username;

    RecyclerViewAdapterComment recyclerViewAdapter;


    public CommentDialog(Activity a, String userId, String username) {
        super(a);

        this.c = a;

        this.userId = userId;
        this.username = username;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.comment_dialog);

        neutralButton = (Button) findViewById(R.id.commentDialog_neutralButton);
        rv = (RecyclerView) findViewById(R.id.commentDialog_list);
        title = (TextView) findViewById(R.id.commentDialog_userComments);
        noRatings = (TextView) findViewById(R.id.commentDialog_noRatings);

        title.setText(String.format(c.getResources().getString(R.string.bookDetail_titleComment), username));

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        rv.setLayoutManager(mLayoutManager);

        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        downloadComments();
    }


    private void downloadComments() {
        //get comments
        FirebaseDatabase.getInstance()
                .getReference()
                .child(FIREBASE_DATABASE_LOCATION_COMMENTS)
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        LinkedList<Comment> comments = new LinkedList<Comment>();

                        //get comments
                        for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                            Comment comment = bookSnapshot.getValue(Comment.class);
                            comments.add(comment);
                        }

                        if(comments.isEmpty()) {
                            rv.setVisibility(View.GONE);
                            noRatings.setVisibility(View.VISIBLE);
                        }

                        //most recent on top
                        Collections.reverse(comments);

                        //add comments to recyclerViewAdapter
                        recyclerViewAdapter = new RecyclerViewAdapterComment(getContext(), comments, CommentDialog.this);
                        rv.setAdapter(recyclerViewAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        dismiss();
                    }
                });
    }
}
