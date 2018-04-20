package mad24.polito.it;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mad24.polito.it.models.Book;

public class ManualInsertActivity extends AppCompatActivity {

    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;

    // [START declare_database_ref]
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    // [END declare_database_ref]

    private EditText mTitleField;
    private EditText mAuthorField;
    private EditText mISBNField;
    private Button submit_btn;
    private TextView mCancel;
    private ImageView mImageField;

    private String userChoosenTask;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_insert);

        // [START initialize_database_ref]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        // [END initialize_database_ref]

        mImageField = (ImageView) findViewById(R.id.manual_ins_newImage);
        mTitleField = (EditText) findViewById(R.id.manual_ins_book_title);
        mAuthorField = (EditText) findViewById(R.id.manual_ins_book_author);
        mISBNField = (EditText) findViewById(R.id.manual_ins_isbn);

        mCancel = (TextView) findViewById(R.id.cancelEdit);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        submit_btn = (Button) findViewById(R.id.manual_ins_submitbtn);
        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitBook();
            }
        });

        mImageField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBookPhoto();
            }
        });
    }

    private void addBookPhoto() {

            //get string for AlertDialog options
            final String optionScan = getResources().getString(R.string.dialog_camera);
            final String optionManual = getResources().getString(R.string.dialog_library);
            final String optionCancel = getResources().getString(mad24.polito.it.R.string.dialog_cancel);

            final CharSequence[] items = {optionScan, optionManual, optionCancel};

            //create the AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(ManualInsertActivity.this);
            //builder.setTitle("Add Book!");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {

                    if (items[item].equals(optionScan)) {
                        boolean permissionCamera = PermissionManager.checkPermissionCamera(ManualInsertActivity.this);

                        userChoosenTask ="Scan ISBN";
                        if(permissionCamera)
                            cameraIntent();
                    } else if (items[item].equals(optionManual)) {
                        boolean permissionRead = PermissionManager.checkPermissionRead(ManualInsertActivity.this);

                        userChoosenTask ="Choose manual insert";
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
                            mad24.polito.it.R.string.deny_permission_read, Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case PermissionManager.PERMISSION_REQUEST_CAMERA:
                // Request for camera permission.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraIntent();
                } else {
                    Toast.makeText(getBaseContext(),
                            mad24.polito.it.R.string.deny_permission_camera, Toast.LENGTH_LONG)
                            .show();
                }
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("state", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        //if image profile is taken by gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mImageField.setImageBitmap(bitmap);
        }

        //if image profile is shot by the camera
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            File out = new File(getFilesDir(), "newImage.jpg");

            if(!out.exists()) {
                Toast.makeText(getBaseContext(),
                        mad24.polito.it.R.string.error_camera, Toast.LENGTH_LONG)
                        .show();
                return;
            }

            Bitmap mBitmap = BitmapFactory.decodeFile(out.getAbsolutePath());

            //set new profile image = shot photo
            mImageField.setImageBitmap(mBitmap);

            uri = Uri.parse(out.getAbsolutePath());
        }
    }

    private void submitBook() {
        final String title = mTitleField.getText().toString();
        final String author = mAuthorField.getText().toString();
        final String isbn = mISBNField.getText().toString();

        // Title is required
        if (TextUtils.isEmpty(title)) {
            mTitleField.setError(getResources().getString(R.string.required));
            return;
        }

        // Author is required
        if (TextUtils.isEmpty(author)) {
            mAuthorField.setError(getResources().getString(R.string.required));
            return;
        }

        // ISBN is required
        if (TextUtils.isEmpty(isbn)) {
            mISBNField.setError(getResources().getString(R.string.required));
            return;
        }

        // ISBN 13 digits
        if (isbn.length() != 13) {
            mISBNField.setError(getResources().getString(R.string.ISBN_explain));
            return;
        }

        DatabaseReference mRef = mDatabase.child("books");
        String bookKey = mRef.push().getKey();

        StorageReference bookCoverRef = mStorageRef.child("bookCovers").child(bookKey);
        bookCoverRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUri = taskSnapshot.getDownloadUrl();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Error loading image", Toast.LENGTH_LONG);
                        return;
                    }
                });

        mRef.child(bookKey).setValue(new Book(mTitleField.getText().toString(),
                mAuthorField.getText().toString(),
                mISBNField.getText().toString(),
                bookKey));


        finish();
/*
        // Disable button so there are no multi-posts
        setEditingEnabled(false);
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show();

        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        Book user = dataSnapshot.getValue(Book.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Log.e(TAG, "User " + userId + " is unexpectedly null");
                            Toast.makeText(NewPostActivity.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            writeNewPost(userId, user.username, title, body);
                        }

                        // Finish this Activity, back to the stream
                        setEditingEnabled(true);
                        finish();
                        // [END_EXCLUDE]
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        // [START_EXCLUDE]
                        setEditingEnabled(true);
                        // [END_EXCLUDE]
                    }
                });
        // [END single_value_read]
    }

    private void setEditingEnabled(boolean enabled) {
        mTitleField.setEnabled(enabled);
        mBodyField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }

    // [START write_fan_out]
    private void writeNewPost(String userId, String username, String title, String body) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = mDatabase.child("posts").push().getKey();
        Post post = new Post(userId, username, title, body);
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
    }
    // [END write_fan_out]
*/

    }
}
