package mad24.polito.it.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import mad24.polito.it.R;
import mad24.polito.it.RecyclerViewAdapter;
import mad24.polito.it.models.Book;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment {

    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = "books";

    View v;

    EditText searchBar;
    Button searchButton;

    // Android Layout
    private RecyclerView rv;
    private RecyclerViewAdapter recyclerViewAdapter;

    // Array lists
    private List<Book> books;
    private String userChoosenTask;

    // Recycler view management
    private int mTotalItemCount = 0;
    private int mLastVisibleItemPosition;
    private boolean mIsLoading = false;
    private int mPostsPerPage = 6;
    private String lastItemId = null;
    private boolean continueSearch = true;


    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(mad24.polito.it.R.layout.fragment_search, container, false);

        rv = (RecyclerView) v.findViewById(R.id.search_bookList);
        searchBar = (EditText) v.findViewById(R.id.search_searchBar);
        searchButton = (Button) v.findViewById(R.id.search_searchButton);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(mLayoutManager);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                books = new ArrayList<Book>();
                recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
                rv.setAdapter(recyclerViewAdapter);

                final String searchString = searchBar.getText().toString();
                lastItemId = null;
                continueSearch = true;
                //mTotalItemCount = 0;

                getBooks(lastItemId, searchString, mPostsPerPage);

                rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        if (dy > 0) {
                            mTotalItemCount = mLayoutManager.getItemCount();
                            mLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();

                            Log.d("debug", "totalItem: "+mTotalItemCount+"; lastvisiblePosition: "+mLastVisibleItemPosition);
                            if (!mIsLoading && continueSearch) { //mTotalItemCount <= (mLastVisibleItemPosition+mPostsPerPage)) {

                                Log.d("debug", "GET MORE BOOKS");
                                getBooks(lastItemId, searchString, mPostsPerPage);
                                mIsLoading = true;
                            }
                        }
                    }
                });

            }
        });

        return v;
    }

    private void getBooks(final String nodeId, final String keyword, final int remainedQuantity) {
        Query query;

        if(remainedQuantity < 1)
            return;

        Log.d("debug", "GETBOOKS");

        if (nodeId == null)
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                    .orderByKey()
                    .limitToFirst(mPostsPerPage);
        else
            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                    .orderByKey()
                    .startAt(nodeId)
                    .limitToFirst(mPostsPerPage);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Book> books = new ArrayList<>();
                int remained = remainedQuantity;
                boolean justLast = true;

                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    Book book = bookSnapshot.getValue(Book.class);

                    if(nodeId != null && nodeId.equals(book.getBook_id()) )
                        continue;

                    //mTotalItemCount++;

                    if(book.getTitle().toLowerCase().contains(keyword)) {
                        books.add(book);
                        remained--;
                    } else if(book.getAuthor().toLowerCase().contains(keyword)) {
                        books.add(book);
                        remained--;
                    }

                    //check match on publisher
                    //check match on genre

                    lastItemId = book.getBook_id();
                    justLast = false;
                }

                if(!dataSnapshot.hasChildren() )
                    remained = 0;

                if(justLast) {
                    remained = 0;
                    continueSearch = false;
                }

                recyclerViewAdapter.addAll(books);

                //if not much books are found
                getBooks(lastItemId, keyword, remained);
                mIsLoading = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
            }
        });
    }

}

/*
usersRef.orderByChild("title").startAt("lord").endAt("lord\uf8ff").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object obj = snapshot.getValue(Object.class);
                Log.i("state", "ad");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
 */
