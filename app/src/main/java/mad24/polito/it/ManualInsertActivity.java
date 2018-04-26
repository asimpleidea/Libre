package mad24.polito.it;

import android.app.ProgressDialog;
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

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import mad24.polito.it.models.Book;

public class ManualInsertActivity extends AppCompatActivity {

    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;
    private int ISBN_SCAN = 3;

    // [START declare_database_ref]
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    // [END declare_database_ref]

    ProgressDialog progressDialog;
    Bitmap mBitmap;

    private EditText mTitleField;
    private EditText mAuthorField;
    private EditText mISBNField;
    private Button submit_btn;
    private TextView mCancel;
    private ImageView mImageField;

    private String userChoosenTask;
    private Uri uri;
    private String bookCoverUri;

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

        Bundle b = getIntent().getExtras();
        int value = -1; // or other values
        if(b != null)
            value = b.getInt("scan");

        if(value == 1){
            IntentIntegrator scan_integrator = new IntentIntegrator(this);
            scan_integrator.setBeepEnabled(false)
                    .setDesiredBarcodeFormats(IntentIntegrator.EAN_13)
                    .initiateScan();
        }

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
        Log.i("isbn", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);


        //if image profile is taken by gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();

            mBitmap = null;
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mImageField.setImageBitmap(mBitmap);
        }else {
            //if image profile is shot by the camera
            if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
                File out = new File(getFilesDir(), "newImage.jpg");

                if (!out.exists()) {
                    Toast.makeText(getBaseContext(),
                            mad24.polito.it.R.string.error_camera, Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                mBitmap = BitmapFactory.decodeFile(out.getAbsolutePath());

                //set new profile image = shot photo
                mImageField.setImageBitmap(mBitmap);

                uri = Uri.parse(out.getAbsolutePath());
                Log.d("absolutepath", uri.toString());
            } else {

                //        if(requestCode == ISBN_SCAN){
                IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

                if (scanningResult != null) {
                    String scanContent = scanningResult.getContents();
                    String scanFormat = scanningResult.getFormatName();

                    Log.d("isbn", scanContent);
                    Log.d("isbn", scanFormat);

                    Toast.makeText(getApplicationContext(),
                            "format: " + scanFormat + " code: " + scanContent, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("isbn", "error");
                    Toast.makeText(getApplicationContext(),
                            "No scan data received!", Toast.LENGTH_SHORT).show();
                }
//          }
            }
        }
    }

    private void submitBook() {
        final String title = mTitleField.getText().toString();
        final String author = mAuthorField.getText().toString();
        final String isbn = mISBNField.getText().toString();

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

        DatabaseReference mRef = mDatabase.child("books");
        String bookKey = mRef.push().getKey();

        uploadImage(bookKey);

        mRef.child(bookKey).setValue(new Book(
                mTitleField.getText().toString(),
                mAuthorField.getText().toString(),
                mISBNField.getText().toString(),
                bookCoverUri,
                bookKey,
                FirebaseAuth.getInstance().getUid()));

        //Log.d("user_id",  FirebaseAuth.getInstance().getUid());
    }

    private void uploadImage(String bookKey) {

        //create reference to images folder and assing a name to the file that will be uploaded
        StorageReference bookCoverRef = mStorageRef.child("bookCovers").child(bookKey+".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        //creating and showing progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMax(100);
        progressDialog.setMessage("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
        progressDialog.setCancelable(false);

        //starting upload
//        UploadTask uploadTask = bookCoverRef.putFile(uri);

        UploadTask uploadTask = bookCoverRef.putBytes(data);

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                //sets and increments value of progressbar
                progressDialog.incrementProgressBy((int) progress);
            }
        });
        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(ManualInsertActivity.this,"Error in uploading!",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Toast.makeText(ManualInsertActivity.this,"Upload successful",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                //showing the uploaded image in ImageView using the download url
                Glide.with(ManualInsertActivity.this).load(downloadUrl).into(mImageField);

                finish();

            }
        });

    }
}
