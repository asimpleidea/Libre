package mad24.polito.it;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
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
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;
import mad24.polito.it.registrationmail.LoginActivity;
import mad24.polito.it.registrationmail.SignupMailActivity;

public class ManualInsertActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS;
    private static final String FIREBASE_DATABASE_LOCATION_LOCATION = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS_LOCATION;

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
    private EditText mPublisherField;
    private EditText mEditionYearField;
    private EditText mBookConditionField;

    private Button submit_btn;
    private TextView mCancel;
    private ImageView mImageField;

    private String userChoosenTask;
    private Uri uri;
    private String bookCoverUri;

    private Double lat = null;
    private Double lon = null;

    private Button btnGenre;
    private String[] genresList;                                    //all genres list
    boolean[] checkedItems;                                         //checked genres
    private boolean isPhoto;

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
        mPublisherField = (EditText) findViewById(R.id.manual_ins_publisher);
        mEditionYearField = (EditText) findViewById(R.id.manual_ins_book_editionYear);
        mBookConditionField = (EditText) findViewById(R.id.manual_ins_book_conditions);

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

        //get elements to manage favourite genres
        btnGenre = (Button) findViewById(R.id.manual_ins_buttonGenre);
        genresList = getResources().getStringArray(R.array.genres);
        checkedItems = new boolean[genresList.length];

        //set event to manage favourite genres
        manageButtonGenre();

        Bundle b = getIntent().getExtras();
        int value = -1; // or other values
        if(b != null)
            value = b.getInt("scan");

        if(value == 1){
            getIntent().removeExtra("scan");
            IntentIntegrator scan_integrator = new IntentIntegrator(this);
            scan_integrator.setBeepEnabled(false)
//                    .setRequestCode(ISBN_SCAN)
                    .setCameraId(0)
                    .setOrientationLocked(false)
                    .setDesiredBarcodeFormats(IntentIntegrator.EAN_13)
                    .initiateScan();
        }

        FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //get data from Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        //get coordinates
        usersRef.child(userAuth.getUid() ).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //get User object
                UserMail user = snapshot.getValue(UserMail.class);

                //get coordinates
                lat = user.getLat();
                lon = user.getLon();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                return;
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

            isPhoto = false;
            mImageField.setImageBitmap(mBitmap);
        }
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

            isPhoto = true;
            uri = Uri.parse(out.getAbsolutePath());
            Log.d("absolutepath", uri.toString());
        }

        if(requestCode == IntentIntegrator.REQUEST_CODE && resultCode != RESULT_OK) {
            finish();
            return;
        }

        if( resultCode != RESULT_OK) {
            return;
        }

        if(requestCode == IntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("isbn", "requestCode: "+requestCode);
            Log.d("isbn", "resultCode: "+resultCode);

            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

            if (scanningResult != null) {
                String scanContent = scanningResult.getContents();
                String scanFormat = scanningResult.getFormatName();

                if (scanContent == null || scanFormat == null) {
                    showDialog(getResources().getString(R.string.manual_insert_error),
                            getResources().getString(R.string.manual_insert_error_retry));
                    return;
                }

                Log.d("isbn", scanContent);
                Log.d("isbn", scanFormat);

//                Toast.makeText(getApplicationContext(),
//                        "format: " + scanFormat + " code: " + scanContent, Toast.LENGTH_LONG).show();

                //get info by isbn
                try {
                    JSONObject responseJson = new RetrieveBookGoogle().execute(scanContent).get();

                    if ((responseJson == null) || (responseJson.getInt("totalItems") == 0)){
                        Log.i("state", "NULL");
                        Toast.makeText(getApplicationContext(), getString(R.string.isbn_not_valid), Toast.LENGTH_LONG).show();
                        //alert dialog
                    } else {
                        Log.i("state", "OK");
                        mISBNField.setText(scanContent);
                        Book book = new Book();

                        try {
                            JSONObject bookInfoJSON = (JSONObject) responseJson.getJSONArray("items").get(0);
                            JSONObject bookJSON = bookInfoJSON.getJSONObject("volumeInfo");

                            //set title
                            mTitleField.setText(bookJSON.getString("title"));

                            //get and set authors
                            String authors = "";
                            JSONArray arrayAuthors = bookJSON.getJSONArray("authors");
                            for (int i = 0; i < arrayAuthors.length(); i++) {
                                if (i > 0)
                                    authors += ", ";

                                authors += arrayAuthors.getString(i);
                            }

                            mAuthorField.setText(authors);

                            if(bookJSON.has("publishedDate"))
                                //set edition year
                                mEditionYearField.setText(bookJSON.getString("publishedDate") );

                        } catch (Exception e) {
                            e.printStackTrace();
                            showDialog(getResources().getString(R.string.manual_insert_error),
                                    getResources().getString(R.string.manual_insert_error_retry));

                            return;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                    showDialog("Error getting info about book", "Please retry or insert data handly");
                }
            } else {
                Log.d("isbn", "error");
                Toast.makeText(getApplicationContext(),
                        "No scan data received!", Toast.LENGTH_SHORT).show();
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

        //if coordinates are not set
        if(lat == null || lon == null) {
            new AlertDialog.Builder(ManualInsertActivity.this)
                    .setTitle(getResources().getString(R.string.coordinates_error))
                    .setMessage(getResources().getString(R.string.coordinates_errorGettingInfo))
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return;
        }

        //get genre strings
        ArrayList<Integer> selectedGenres = new ArrayList<Integer>();

        for (int i = 0; i < genresList.length; i++) {
            if (checkedItems[i] == true)
                selectedGenres.add(i);
        }

        DatabaseReference mRef = mDatabase.child(FIREBASE_DATABASE_LOCATION_BOOKS);
        String bookKey = mRef.push().getKey();
        Date date = new Date();
//        Log.d("anotherbug", (mBitmap==null)?"no image":"there is something!");
        if(mBitmap != null)
            uploadImage(bookKey);
//        Log.d("anotherbug", "uploading book");
        mRef.child(bookKey).setValue(new Book(
                mTitleField.getText().toString(),
                mAuthorField.getText().toString(),
                mISBNField.getText().toString(),
                mPublisherField.getText().toString(),
                mEditionYearField.getText().toString(),
                mBookConditionField.getText().toString(),
                bookCoverUri,
                bookKey,
                FirebaseAuth.getInstance().getUid(),
                date,
                selectedGenres));

        GeoFire geoFire = new GeoFire(mDatabase.child(FIREBASE_DATABASE_LOCATION_LOCATION));

        SharedPreferences prefs = getSharedPreferences("location", MODE_PRIVATE);
        geoFire.setLocation(bookKey, new GeoLocation(lat, lon), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });

//        Log.d("anotherbug", "book uploaded");
        mRef = mDatabase.child("users");

        mRef.child(FirebaseAuth.getInstance().getUid()).child(FIREBASE_DATABASE_LOCATION_BOOKS).child(bookKey).setValue(true);

        Toast.makeText(this, getString(R.string.book_submitted),Toast.LENGTH_LONG).show();
        finish();
        //Log.d("anotherbug", "user updated");
    }

    private void uploadImage(String bookKey) {

        //create reference to images folder and assing a name to the file that will be uploaded
        StorageReference bookCoverRef = mStorageRef.child("bookCovers").child(bookKey+".jpg");

        //the shortest side must be 180px
        Bitmap b = mBitmap; //BitmapFactory.decodeFile(uri.toString());
        float scale;
        if(b.getWidth() > b.getHeight()) {
            scale = (float)b.getHeight() / 180;
        } else {
            scale = (float)b.getWidth() / 180;
        }

        if(scale < 1)
            scale = 1;

        mBitmap = Bitmap.createScaledBitmap(b, (int)((float)b.getWidth()/scale), (int)((float)b.getHeight()/scale), true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        //creating and showing progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMax(100);
        progressDialog.setMessage(getResources().getText(R.string.manualInsert_uploading));
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
                Toast.makeText(ManualInsertActivity.this, getResources().getText(R.string.manualInsert_uploadError),Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Toast.makeText(ManualInsertActivity.this, getResources().getText(R.string.manualInsert_uploadSuccessful),Toast.LENGTH_SHORT).show();
                //progressDialog.dismiss();
                //showing the uploaded image in ImageView using the download url
                //Glide.with(ManualInsertActivity.this).load(downloadUrl).into(mImageField);

                finish();

            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save favourite genres
        outState.putSerializable("book_genres", checkedItems);

        outState.putBoolean("isPhoto", isPhoto);

//        Log.d("checking", (uri==null)?"null obj":uri.toString());
        if(uri != null)
            outState.putString("uri", uri.toString());

        return;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //get favourite genres
        checkedItems = (boolean[]) savedInstanceState.getSerializable("book_genres");

        if(savedInstanceState.getString("uri") != null)
            uri = Uri.parse(savedInstanceState.getString("uri"));
        else
            uri = null;

        if(uri != null) {
            isPhoto = savedInstanceState.getBoolean("isPhoto");

            try {
                if (isPhoto) {
                    Bitmap bmp = BitmapFactory.decodeFile(uri.toString());
                    mBitmap = bmp;
                    mImageField.setImageBitmap(bmp);
                } else {
                    File f = new File(uri.toString());
                    if (!f.exists()) {
                        //if image is saved on gallery (new image)
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mBitmap = bmp;
                        mImageField.setImageBitmap(bmp);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//            mImageField.setImageURI(uri);

        return;
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(getApplicationContext())
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //event clicking the button to choose favourite genres
    private void manageButtonGenre() {
        btnGenre.setOnClickListener(new View.OnClickListener() {
            boolean[] oldSelectedGenres = new boolean[genresList.length];

            @Override
            public void onClick(View view) {
                oldSelectedGenres = new boolean[genresList.length];

                for(int i=0; i < genresList.length; i++) {
                    if(checkedItems[i] == true)
                        oldSelectedGenres[i] = true;
                }

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(ManualInsertActivity.this);
                mBuilder.setTitle(R.string.title_genre_alertdialog);
                mBuilder.setMultiChoiceItems(genresList, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    //called every time you click a checkbox
                    public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                    }
                });

                mBuilder.setCancelable(false);

                //called when you click "ok" button
                mBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                    }
                });

                mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        for(int i=0; i < genresList.length; i++) {
                            checkedItems[i] = oldSelectedGenres[i];
                        }

                        dialogInterface.dismiss();
                    }
                });

                final AlertDialog mDialog = mBuilder.create();

                mDialog.show();
            }
        });
    }
}
