package mad24.polito.it.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import mad24.polito.it.*;
import mad24.polito.it.models.*;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class BooksFragment extends Fragment {

    private int REQUEST_CAMERA = 1;
    private int PICK_IMAGE_REQUEST = 2;

    View v;

    // Firebase var
    private DatabaseReference mDatabase;

    // Android Layout
    private RecyclerView rv;
    private FloatingActionButton new_book_button;

    // Array lists
    private List<Book> books;
    private String userChoosenTask;

    public BooksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        Log.d("booksfragment", "onCreateView");
        // Inflate the layout for this fragment
        v =  inflater.inflate(R.layout.fragment_book, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        rv = (RecyclerView) v.findViewById(R.id.book_list);
        new_book_button = (FloatingActionButton) v.findViewById(R.id.newBookBtn);

        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.setAdapter(recyclerViewAdapter);

        new_book_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("booksfragment", "Button pressed");
                selectBookInsertMethod();
            }
        });

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("booksfragment", "onCreate method");

/*        mDatabase.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot myitem : dataSnapshot.getChildren()){
                    Book b = myitem.getValue(Book.class);

                    books.add(b);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/

        books = new ArrayList<>();
        books.add(new Book("title1", "author1", "location1", R.drawable.book1));
        books.add(new Book("title2", "author2", "location2", R.drawable.book2));
        books.add(new Book("title3", "author3", "location3", R.drawable.book3));
        books.add(new Book("title4", "author4", "location4", R.drawable.book4));

        books.add(new Book("title1", "author1", "location1", R.drawable.book1));
        books.add(new Book("title2", "author2", "location2", R.drawable.book2));
        books.add(new Book("title3", "author3", "location3", R.drawable.book3));
        books.add(new Book("title4", "author4", "location4", R.drawable.book4));

        books.add(new Book("title1", "author1", "location1", R.drawable.book1));
        books.add(new Book("title2", "author2", "location2", R.drawable.book2));
        books.add(new Book("title3", "author3", "location3", R.drawable.book3));
        books.add(new Book("title4", "author4", "location4", R.drawable.book4));
    }

    private void selectBookInsertMethod() {
        //get string for AlertDialog options
        final String optionScan = getResources().getString(R.string.dialog_scan);
        final String optionManual = getResources().getString(R.string.dialog_manualIns);
        final String optionCancel = getResources().getString(mad24.polito.it.R.string.dialog_cancel);

        final CharSequence[] items = {optionScan, optionManual, optionCancel};

        //create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        //builder.setTitle("Add Book!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (items[item].equals(optionScan)) {
                    boolean permissionCamera = PermissionManager.checkPermissionCamera(getContext());

                    userChoosenTask ="Scan ISBN";
                    if(permissionCamera)
                        scanIntent();
                } else if (items[item].equals(optionManual)) {
                    boolean permissionRead = PermissionManager.checkPermissionRead(getContext());

                    userChoosenTask ="Choose manual insert";
                    if(permissionRead)
                        manualIntent();
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
    private void scanIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //save photo on MyFileProvider.CONTENT_URI
        intent.putExtra(MediaStore.EXTRA_OUTPUT, MyFileContentProvider.CONTENT_URI);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            //TODO: actual code for scan book
            //startActivityForResult(intent, REQUEST_CAMERA);
            Log.d("booksfragment", "camera intent should start");
            Toast.makeText(getActivity().getBaseContext(), "Camera intent should start", Toast.LENGTH_LONG).show();
        }
    }

    //intent to access the gallery
    private void manualIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Show only images, no videos or anything else
        intent.setType("image/*");
        // Always show the chooser (if there are multiple options available)

        //TODO: actual code for manual insert book
        //startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

        Log.d("booksfragment", "manual intent should start");
        Toast.makeText(getActivity().getBaseContext(), "Camera intent should start", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.d("booksfragment", "onRequestPermissionsResult");

        switch (requestCode) {
            case PermissionManager.PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        scanIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        manualIntent();
                } else {
                    Toast.makeText(getActivity().getBaseContext(),
                            mad24.polito.it.R.string.deny_permission_read, Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case PermissionManager.PERMISSION_REQUEST_CAMERA:
                // Request for camera permission.
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanIntent();
                } else {
                    Toast.makeText(getActivity().getBaseContext(),
                            mad24.polito.it.R.string.deny_permission_camera, Toast.LENGTH_LONG)
                            .show();
                }
                break;

        }

    }


}
