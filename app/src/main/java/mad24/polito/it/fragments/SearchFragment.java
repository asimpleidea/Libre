package mad24.polito.it.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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

    SearchView searchView;

    // Android Layout
    private RecyclerView rv;
    private RecyclerViewAdapter recyclerViewAdapter;

    // Array lists
    private List<Book> books;

    // Recycler view management
    private Boolean mIsLoading = false;
    private int mPostsPerPage = 6;      //TODO set to 50
    private String lastItemId = null;
    private boolean continueSearch = true;

    private String currentQuery = new String("");

    LinearLayoutManager mLayoutManager;

    Context context;


    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(mad24.polito.it.R.layout.fragment_search, container, false);

        Toolbar mToolbar = (Toolbar) v.findViewById(R.id.search_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);

        searchView = (SearchView) v.findViewById(R.id.search_searchView);
        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflater.inflate(R.menu.menu_search, menu);
        //MenuItem searchItem = menu.findItem(R.id.search_searchMenu);
        //searchItem.expandActionView();

        rv = (RecyclerView) v.findViewById(R.id.search_bookList);
        mLayoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(mLayoutManager);

        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            String oldQuery = new String("");

            @Override
            public boolean onQueryTextSubmit(final String query) {
                synchronized (currentQuery) {
                    //if you click search button more than one time
                    if(oldQuery.equals(query))
                        return false;

                    //update current and old query
                    currentQuery = query;
                    oldQuery = query;

                    //hide keyboard and suggestions
                    searchView.clearFocus();

                    books = new ArrayList<Book>();
                    recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
                    rv.setAdapter(recyclerViewAdapter);

                    lastItemId = null;
                    continueSearch = true;

                    getBooks(lastItemId, query, mPostsPerPage);

                    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (dy > 0) {

                                if (!mIsLoading && continueSearch) {

                                    Log.d("debug", "GET MORE BOOKS");
                                    getBooks(lastItemId, query, mPostsPerPage);
                                    mIsLoading = true;
                                }

                            }
                        }
                    });


                    return true;
                }

            }

            @Override
            public boolean onQueryTextChange(final String query) {
                if(query.length() < 3)
                    return false;

                synchronized (currentQuery) {
                    currentQuery = query;

                    books = new ArrayList<Book>();
                    recyclerViewAdapter = new RecyclerViewAdapter(getContext(), books);
                    rv.setAdapter(recyclerViewAdapter);

                    lastItemId = null;
                    continueSearch = true;


                    getBooks(lastItemId, query, mPostsPerPage);


                    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (dy > 0) {

                                if (!mIsLoading && continueSearch) {

                                    Log.d("debug", "GET MORE BOOKS");
                                    getBooks(lastItemId, query, mPostsPerPage);
                                    mIsLoading = true;
                                }

                            }
                        }
                    });


                    return true;
                }

            }
        });

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        super.onCreateOptionsMenu(menu, inflater);

        super.onCreateOptionsMenu(menu, inflater);
    }


    private void getBooks(final String nodeId, final String keyword, final int remainedQuantity) {
        final Query query;

        if (remainedQuantity < 1)
            return;

        Log.d("debug", "GETBOOKS");

        v.findViewById(R.id.search_emptyView).setVisibility(View.GONE);

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

                    if (nodeId != null && nodeId.equals(book.getBook_id()))
                        continue;

                    //search for a matching
                    if (book.getTitle().toLowerCase().contains(keyword)) {
                        books.add(book);
                        remained--;
                    } else if (book.getAuthor().toLowerCase().contains(keyword)) {
                        books.add(book);
                        remained--;
                    }

                    //check match on publisher
                    //check match on genre

                    lastItemId = book.getBook_id();
                    justLast = false;
                }

                if (!dataSnapshot.hasChildren()) {
                    remained = 0;
                    continueSearch = false;
                }

                if (justLast) {
                    remained = 0;
                    continueSearch = false;
                }

                //if query is changed --> abort operation
                synchronized (currentQuery) {
                    if(!currentQuery.equals(keyword)) {
                        continueSearch = false;
                        mIsLoading = false;
                        return;
                    } else
                        recyclerViewAdapter.addAll(books);
                }

                //if not much books are found
                getBooks(lastItemId, keyword, remained);
                mIsLoading = false;

                if(mLayoutManager.getItemCount() < 1) {
                    v.findViewById(R.id.search_emptyView).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                continueSearch = false;
                mIsLoading = false;
            }
        });
    }

}

