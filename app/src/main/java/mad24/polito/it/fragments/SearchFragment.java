package mad24.polito.it.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

import mad24.polito.it.R;
import mad24.polito.it.RecyclerViewAdapter;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;


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

    // Recycler view management
    private Boolean mIsLoading = false;
    private int mPostsPerPage = 6;      //TODO set to 50
    private Integer itemCount = 0;

    private String currentQuery = new String("");

    LinearLayoutManager mLayoutManager;

    Context context;

    Boolean semaphoreKeyPrepared = false;

    private ArrayList<String> keyBooks = new ArrayList<String>();


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

        //get user auth
        FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //get data from Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        usersRef.child(userAuth.getUid() ).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //get User object
                UserMail user = snapshot.getValue(UserMail.class);

                //get coordinates
                Double lat = user.getLat();
                Double lon = user.getLon();

                //get coordinates and nearby books
                getKeys(lat, lon);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if(getContext() == null)
                    return;

                new AlertDialog.Builder(getContext())
                        .setTitle(getResources().getString(R.string.coordinates_error))
                        .setMessage(getResources().getString(R.string.coordinates_errorGettingInfo))
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        //zero books for now
        itemCount = 0;

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
                synchronized (semaphoreKeyPrepared) {
                    if(!semaphoreKeyPrepared)
                        return false;
                }

                synchronized (currentQuery) {
                    itemCount = 0;

                    //if you click search button more than one time
                    if(oldQuery.equals(query))
                        return false;

                    //hide keyboard and suggestions
                    searchView.clearFocus();

                    recyclerViewAdapter = new RecyclerViewAdapter(getContext(), new ArrayList<Book>());
                    rv.setAdapter(recyclerViewAdapter);

                    synchronized(itemCount) {
                        getBooks(query, mPostsPerPage);
                    }

                    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (dy > 0) {
                                synchronized (itemCount) {
                                    if(itemCount.intValue() >= keyBooks.size())
                                        return;

                                    getBooks(query, mPostsPerPage);
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

                synchronized (semaphoreKeyPrepared) {
                    if(!semaphoreKeyPrepared)
                        return false;
                }

                synchronized (currentQuery) {
                    itemCount = 0;

                    currentQuery = query;

                    recyclerViewAdapter = new RecyclerViewAdapter(getContext(), new ArrayList<Book>());
                    rv.setAdapter(recyclerViewAdapter);

                    getBooks(query, mPostsPerPage);

                    rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (dy > 0) {
                                synchronized (itemCount) {
                                    if(itemCount.intValue() >= keyBooks.size())
                                        return;

                                    getBooks(query, mPostsPerPage);
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
    }


    private void getBooks(final String keyword, final int remainedQuantity) {
        final Query query;

        if (remainedQuantity < 1)
            return;

        final int number;
        synchronized (itemCount) {
            if(itemCount.intValue() >= keyBooks.size()) {
                return;
            }

            number = itemCount++;
        }

        v.findViewById(R.id.search_emptyView).setVisibility(View.GONE);

        query = FirebaseDatabase.getInstance().getReference()
                .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                .orderByKey()
                .equalTo(keyBooks.get(number));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!keyword.equals(currentQuery))
                    return;

                int remained = remainedQuantity;

                for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                    Book book = bookSnapshot.getValue(Book.class);
                    String keywordLowerCase = keyword.toLowerCase();

                    if(recyclerViewAdapter.contains(book))
                        return;

                    //search for a matching
                    if (book.getTitle().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getTitle().toLowerCase()) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if (book.getAuthor().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getAuthor().toLowerCase()) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if(book.getIsbn().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getIsbn().toLowerCase()) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if(book.getPublisher() != null && !book.getPublisher().isEmpty()) {
                        if(book.getPublisher().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getPublisher().toLowerCase()) ) {
                            recyclerViewAdapter.add(book);
                            remained--;
                        }
                    }

                    //check match on genre

                    break;
                }

                //if not much books are found
                getBooks(keyword, remained);
                if(mLayoutManager.getItemCount() < 1) {
                    v.findViewById(R.id.search_emptyView).setVisibility(View.VISIBLE);
                } else {
                    v.findViewById(R.id.search_emptyView).setVisibility(View.GONE);
                }

                mIsLoading = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
            }
        });
    }

    public void getKeys(double lat, double lon) {
        semaphoreKeyPrepared = false;

        keyBooks = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("locationBooks");
        GeoFire geoFire = new GeoFire(ref);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lat, lon), 100);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Log.d("debug", "onKeyEntered - New key added");
                if(!keyBooks.contains(key))
                    keyBooks.add(key);
            }

            @Override
            public void onKeyExited(String key) {
                //System.out.println(String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                //System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                //Log.d("debug", "OnGeoQueryReady - all keys are loaded");

                //reverse array to have on top the nearest and most recent
                Collections.reverse(keyBooks);
                semaphoreKeyPrepared = true;
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                //System.err.println("There was an error with this query: " + error);
            }
        });
    }

}

