package mad24.polito.it.fragments.profile;

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
import java.util.HashMap;
import java.util.LinkedList;
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
public class BorrowedBooksFragment extends Fragment {

    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;
    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS;

    View v;

    // Android Layout
    private RecyclerView rv;
    private RecyclerViewAdapter recyclerViewAdapter;
    private TextView tv;

    // Array lists
    private List<Book> books;
    private HashMap<String, String> borrowings;

    // Recycler view management
    private int mTotalItemCount = 0;
    private int mLastVisibleItemPosition;
    private boolean mIsLoading = false;
    private int mBooksPerPage = 20;

    public BorrowedBooksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_profile_borrowedbooks, container, false);

        tv = (TextView) v.findViewById(R.id.no_borrowed_books_msg);

        rv = (RecyclerView) v.findViewById(R.id.borrowed_book_list);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(mLayoutManager);

        books = new ArrayList<Book>();
        recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
        rv.setAdapter(recyclerViewAdapter);

        getBorrowedBooks(null);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) {
                    mTotalItemCount = mLayoutManager.getItemCount();
                    mLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();

                    Log.d("debg", "totalItem: "+mTotalItemCount+"; lastvisiblePosition: "+mLastVisibleItemPosition);
                    if (!mIsLoading && mTotalItemCount <= (mLastVisibleItemPosition+ mBooksPerPage)) {

                        getBorrowedBooks(recyclerViewAdapter.getLastItemId());
                        mIsLoading = true;
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("state", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getBorrowedBooks(final String nodeId) {

        //Log.d("booksfragment", "getting borrowed books starting from: "+nodeId);

        Query query;

        //if(nodeId == null) {
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_USERS)
                    .child(FirebaseAuth.getInstance().getUid())
                    .child("borrowed_books");
                    /*.orderByKey()
                    .limitToFirst(mBooksPerPage);
        }else{
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_USERS)
                    .child(FirebaseAuth.getInstance().getUid())
                    .child("borrowed_books")
                    .orderByKey()
                    .startAt(nodeId)
                    .limitToFirst(mBooksPerPage);
        }*/

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final HashMap<String, String> borrowedBooks = new HashMap<>();
                final List<Boolean> booksToRate = new LinkedList<>();
                final List<String> borrowingIds = new LinkedList<>();

                boolean flag = false;
                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    if(nodeId == null) {
                        borrowedBooks.put(bookSnapshot.getKey(), (String) bookSnapshot.getValue());
                        booksToRate.add(false);
                        borrowingIds.add("");
                    }else if(flag) {
                        borrowedBooks.put(bookSnapshot.getKey(), (String) bookSnapshot.getValue());
                        booksToRate.add(false);
                        borrowingIds.add("");
                    }
                    flag = true;
                }

                Log.d("borrowed books", "adding "+borrowedBooks.size()+" borrowed books");
                if(borrowedBooks.size() == 0 && recyclerViewAdapter.getItemCount() == 0){
                    tv.setVisibility(View.VISIBLE);
                    rv.setVisibility(View.INVISIBLE);
                }else{
                    tv.setVisibility(View.INVISIBLE);
                    rv.setVisibility(View.VISIBLE);
                }

                Query queryToRate = FirebaseDatabase.getInstance().getReference()
                        .child(FIREBASE_DATABASE_LOCATION_USERS)
                        .child(FirebaseAuth.getInstance().getUid())
                        .child("books_to_rate");

                queryToRate.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                            if (nodeId == null) {
                                borrowedBooks.put(bookSnapshot.getKey(), (String) bookSnapshot.getValue());
                                booksToRate.add(true);
                                borrowingIds.add(bookSnapshot.getKey()+"/"+bookSnapshot.getValue()); //"-LC0OVG4MuFY-qtOAU_b/-LEGYI5vwOTB9YN1TjvK");
                            }
                        }

                        Log.d("borrowed books", "adding " + borrowedBooks.size() + " books to rate");
                        if (borrowedBooks.size() == 0 && recyclerViewAdapter.getItemCount() == 0) {
                            tv.setVisibility(View.VISIBLE);
                            rv.setVisibility(View.INVISIBLE);
                        } else {
                            tv.setVisibility(View.INVISIBLE);
                            rv.setVisibility(View.VISIBLE);
                        }

                        recyclerViewAdapter.retrieveBooksAndToRate(new LinkedList<String>(borrowedBooks.keySet() ), booksToRate, borrowingIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mIsLoading = false;
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
            }
        });


    }

}
