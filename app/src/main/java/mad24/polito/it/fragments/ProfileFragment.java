package mad24.polito.it.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;
import mad24.polito.it.RecyclerViewAdapter;
import mad24.polito.it.ShowProfileActivity;
import mad24.polito.it.models.Book;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;
    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS;

    private int SHOW_PROFILE = 1;

    View v;

    // Android Layout
    private RecyclerView rv;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextView tv;
    private TextView uv;

    // Array lists
    private List<Book> books;

    FirebaseUser userAuth;

    de.hdodenhof.circleimageview.CircleImageView profile_img;
    android.support.design.widget.FloatingActionButton profile_button;
    private Bitmap profileImageBitmap;

    // Recycler view management
    private int mTotalItemCount = 0;
    private int mLastVisibleItemPosition;
    private boolean mIsLoading = false;
    private int mBooksPerPage = 20;

    Boolean semaphoreImage = false;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(mad24.polito.it.R.layout.fragment_profile, container, false);

        tv = (TextView) v.findViewById(R.id.no_books_msg);
        uv = (TextView) v.findViewById(R.id.user_name);

        rv = (RecyclerView) v.findViewById(R.id.posted_book_list);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(mLayoutManager);

        books = new ArrayList<Book>();
        recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
        rv.setAdapter(recyclerViewAdapter);

        getUserName();
        getBooks(null);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    mTotalItemCount = mLayoutManager.getItemCount();
                    mLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();

                    Log.d("debg", "totalItem: "+mTotalItemCount+"; lastvisiblePosition: "+mLastVisibleItemPosition);
                    if (!mIsLoading && mTotalItemCount <= (mLastVisibleItemPosition+ mBooksPerPage)) {

                        getBooks(recyclerViewAdapter.getLastItemId());
                        mIsLoading = true;
                    }
                }
            }
        });

        return v;
    }

    private void getUserName() {
        Query query;

        query = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_DATABASE_LOCATION_USERS)
                .child(FirebaseAuth.getInstance().getUid())
                .child("name");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                uv.setText(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profile_img = (de.hdodenhof.circleimageview.CircleImageView) v.findViewById(R.id.frag_profile_image);
        profile_button = (android.support.design.widget.FloatingActionButton) v.findViewById(R.id.frag_profile_photoButton);

        //get user
        userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //first time "profileImageBitmap" will be always null --> download to Firebase
        if(profileImageBitmap == null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + userAuth.getUid() + ".jpg");
            try {
                final File localFile = File.createTempFile("Images", ".bmp");
                ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        profileImageBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        profile_img.setImageBitmap(profileImageBitmap);

                        synchronized (semaphoreImage) {
                            semaphoreImage = true;
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Activity activity = getActivity();
                        if(activity != null && isAdded())
                            profile_img.setImageDrawable(getResources().getDrawable(R.drawable.unknown_user));

                        synchronized (semaphoreImage) {
                            semaphoreImage = true;
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();

                synchronized (semaphoreImage) {
                    semaphoreImage = true;
                }
            }
        } else {
            profile_img.setImageBitmap(profileImageBitmap);

            synchronized (semaphoreImage) {
                semaphoreImage = true;
            }
        }

        //set event clicking profile image or pencil button
        profile_img.setOnClickListener(new eventImageClick());
        profile_button.setOnClickListener(new eventImageClick());

    }

    private void getBooks(final String nodeId) {

        //Log.d("booksfragment", "getting books starting from: "+nodeId);

        Query query;

        if(nodeId == null) {
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_USERS)
                    .child(FirebaseAuth.getInstance().getUid())
                    .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                    .orderByKey()
                    .limitToFirst(mBooksPerPage);
        }else{
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_USERS)
                    .child(FirebaseAuth.getInstance().getUid())
                    .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                    .orderByKey()
                    .startAt(nodeId)
                    .limitToFirst(mBooksPerPage);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> books = new ArrayList<>();
                boolean flag = false;
                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    if(nodeId == null)
                        books.add(bookSnapshot.getKey());
                    else
                    if(flag)
                        books.add(bookSnapshot.getKey());
                    flag = true;
                }

                Log.d("books", "adding "+books.size()+" books");
                if(books.size() == 0 && recyclerViewAdapter.getItemCount() == 0){
                    tv.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.INVISIBLE);
                }else{
                    tv.setVisibility(View.INVISIBLE);
                    rv.setVisibility(View.VISIBLE);
                }
                recyclerViewAdapter.retrieveBooks(books);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
            }
        });


    }

    class eventImageClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //check if profile image is loaded
            synchronized (semaphoreImage) {
                if(!semaphoreImage) {
                    showDialog(getString(R.string.showprofile_dataNotLoaded),
                            getString(R.string.showprofile_waitData));
                    return;
                }
            }

            //get preferences
            if(getContext() == null)
                return;

            SharedPreferences prefs = getContext().getSharedPreferences("profile", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            //save profile image if not the standard one
            if(profileImageBitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] b = baos.toByteArray();
                String encoded = Base64.encodeToString(b, Base64.DEFAULT);
                editor.putString("profileImage", encoded);
            } else {
                editor.putString("profileImage", "unknown");
            }

            editor.commit();

            Intent intent = new Intent(getActivity(), ShowProfileActivity.class);
            startActivityForResult(intent, SHOW_PROFILE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("state", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_PROFILE && resultCode == Activity.RESULT_OK) {
            //get profile image
            String encoded = data.getStringExtra("profileImage");
            if(encoded != null) {
                byte[] imageAsBytes = Base64.decode(encoded.getBytes(), Base64.DEFAULT);
                profileImageBitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
                profile_img.setImageBitmap(profileImageBitmap);
            }
        }
    }

    private void showDialog(String title, String message) {
        if(getContext() == null)
            return;

        new AlertDialog.Builder(getContext() )
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

}
