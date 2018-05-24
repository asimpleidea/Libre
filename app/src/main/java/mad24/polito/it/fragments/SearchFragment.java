package mad24.polito.it.fragments;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.PlaceArrayAdapter;
import mad24.polito.it.R;
import mad24.polito.it.RecyclerViewAdapter;
import mad24.polito.it.locator.LocatorEventsListener;
import mad24.polito.it.locator.LocatorSearch;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;
import mad24.polito.it.radiobutton.PresetRadioGroup;
import mad24.polito.it.radiobutton.PresetValueButton;
import mad24.polito.it.search.RecyclerViewAdapterGenre;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS;
    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;
    public static final String FIREBASE_DATABASE_LOCATION_BOOKS_LOCATION = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS_LOCATION;

    private final double RADIUS = 100;

    View v;

    private SearchView searchView;

    // Android Layout
    private RecyclerView rv;
    private RecyclerViewAdapter recyclerViewAdapter;

    // Recycler view management
    private Boolean mIsLoading = false;
    private int mPostsPerPage = 20;
    private Integer itemCount = 0;

    private String currentQuery = new String("");

    private LinearLayoutManager mLayoutManager;
    private LinearLayout searchOptions;
    private PresetRadioGroup mSetDurationPresetRadioGroup;
    private AutoCompleteTextView city;
    private TextView emptyView;

    String searchBy;
    private String[] genresList;

    Context context;

    Boolean semaphoreKeyPrepared = false;

    private ArrayList<String> keyBooks = new ArrayList<String>();

    private static final int GOOGLE_API_CLIENT_ID = 1;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    private double lat;
    private double lon;
    private Double latGPS = null;
    private Double lonGPS = null;
    private UserMail myselfUser = null;

    private boolean isGps;
    LocatorSearch locator;

    private RecyclerViewAdapterGenre myAdapter;

    private boolean boolSavedInstanceState = false;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(mad24.polito.it.R.layout.fragment_search, container, false);

        Toolbar mToolbar = (Toolbar) v.findViewById(R.id.search_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);

        searchView = (SearchView) v.findViewById(R.id.search_searchView);
        searchOptions = (LinearLayout) v.findViewById(R.id.search_searchOptions);
        mSetDurationPresetRadioGroup = (PresetRadioGroup) v.findViewById(R.id.preset_time_radio_group);
        city = (AutoCompleteTextView) v.findViewById(R.id.search_autoCompleteCity);
        emptyView = (TextView) v.findViewById(R.id.search_emptyView);
        genresList = getResources().getStringArray(R.array.genres);

        //event clicking the "search by" radio buttons
        mSetDurationPresetRadioGroup.setOnCheckedChangeListener(new PresetRadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View radioGroup, View radioButton, boolean isChecked, int checkedId) {
                Log.d("debug", "ID:"+checkedId);
                searchBy = ((PresetValueButton) radioButton).getValue();
            }
        });

        //by default --> search by keyword
        searchBy = getResources().getString(R.string.search_searchBy_all);

        //by default --> "All fields" radio button is selected
        PresetValueButton buttonSearchByAll = (PresetValueButton) v.findViewById(R.id.search_searchBy_all);
        buttonSearchByAll.setChecked(true);

        //hide keyboard if you click away
        city.setOnFocusChangeListener(eventFocusChangeListenerCity);

        //set city suggestion
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected() ) {
            try {
                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(Places.GEO_DATA_API)
                        //.enableAutoManage(getActivity(), GOOGLE_API_CLIENT_ID, this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final float scale = getContext().getResources().getDisplayMetrics().density;

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            city.setDropDownHeight((int)(50 * scale));  //if landscape --> 1 suggestion
        else
            city.setDropDownHeight((int)(150 * scale));   //if portrait --> 3 suggestions

        city.setOnItemClickListener(mAutocompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(getContext(), android.R.layout.simple_list_item_1,
                BOUNDS_MOUNTAIN_VIEW,
                new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build());
        city.setAdapter(mPlaceArrayAdapter);



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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //don't ask for GPS coordinates when rotate the device
        if(savedInstanceState != null) {
            lat = savedInstanceState.getDouble("lat", 0.0);
            lon = savedInstanceState.getDouble("lon", 0.0);

            latGPS = savedInstanceState.getDouble("latGPS", 1000);
            lonGPS = savedInstanceState.getDouble("lonGPS", 1000);


            if (latGPS == 1000)
                latGPS = null;

            if (lonGPS == 1000)
                lonGPS = null;

            Log.d("debug", "LATGPS: "+latGPS+ " LONGPS: "+lonGPS);

            String selectedLocation = savedInstanceState.getString("selectedLocation", null);
            if(selectedLocation != null)
                city.setText(selectedLocation);

            //get User object
            String userJson = savedInstanceState.getString("user", null);
            if(userJson != null) {
                Gson gson = new Gson();
                myselfUser = gson.fromJson(userJson, UserMail.class);
            }

            String searchBySavedInstance = savedInstanceState.getString("searchBy", null);
            if(searchBySavedInstance != null) {
                searchBy = searchBySavedInstance;

                if (searchBy.equals(getResources().getString(R.string.search_searchBy_all)))
                    ((PresetValueButton) v.findViewById(R.id.search_searchBy_all)).setChecked(true);
                else if (searchBy.equals(getResources().getString(R.string.search_searchBy_title)))
                    ((PresetValueButton) v.findViewById(R.id.search_searchBy_title)).setChecked(true);
                else if (searchBy.equals(getResources().getString(R.string.search_searchBy_author)))
                    ((PresetValueButton) v.findViewById(R.id.search_searchBy_author)).setChecked(true);
            }

            boolSavedInstanceState = true;
            getLocationGPS();
        } else {
            //get location GPS
            getLocationGPS();
        }

        //set event GPS location button
        ((ImageButton) v.findViewById(R.id.search_buttonLocation)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                getLocationGPS();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        rv = (RecyclerView) v.findViewById(R.id.search_bookList);
        mLayoutManager = new LinearLayoutManager(getActivity());
        rv.setLayoutManager(mLayoutManager);

        int options = searchView.getImeOptions();
        searchView.setImeOptions(options | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            String oldQuery = new String("");

            @Override
            public boolean onQueryTextSubmit(final String query) {
                synchronized (semaphoreKeyPrepared) {
                    if(!semaphoreKeyPrepared)
                        return false;
                }

                rv.setVisibility(View.VISIBLE);
                searchOptions.setVisibility(View.GONE);

                if(keyBooks.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    return true;
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

                        if(recyclerViewAdapter.getItemCount() < 1) {
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            emptyView.setVisibility(View.GONE);
                        }
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
                if(query.length() < 3) {
                    if(query.length() < 1) {
                        searchOptions.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                    }
                    return false;
                }

                searchOptions.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);

                if(keyBooks.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    return true;
                }

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

                    if(recyclerViewAdapter.getItemCount() < 1) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
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
        });

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        super.onCreateOptionsMenu(menu, inflater);
    }


    private void getBooks(final String keyword, final int remainedQuantity) {
        final Query query;

        if(keyBooks.isEmpty()) {
            return;
        }

        if (remainedQuantity < 1)
            return;

        final int number;
        synchronized (itemCount) {
            if(itemCount.intValue() >= keyBooks.size()) {
                return;
            }

            number = itemCount++;
        }

        emptyView.setVisibility(View.GONE);

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
                    if ( (searchBy.equals(getResources().getString(R.string.search_searchBy_all)) || searchBy.equals(getResources().getString(R.string.search_searchBy_title)) ) &&
                            (book.getTitle().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getTitle().toLowerCase()) ) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if ( (searchBy.equals(getResources().getString(R.string.search_searchBy_all)) || searchBy.equals(getResources().getString(R.string.search_searchBy_author)) ) &&
                            (book.getAuthor().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getAuthor().toLowerCase()) ) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if(searchBy.equals(getResources().getString(R.string.search_searchBy_all)) &&
                            (book.getIsbn().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getIsbn().toLowerCase()) ) ) {
                        recyclerViewAdapter.add(book);
                        remained--;
                    } else if(searchBy.equals(getResources().getString(R.string.search_searchBy_all)) &&
                            book.getPublisher() != null && !book.getPublisher().isEmpty() &&
                            (book.getPublisher().toLowerCase().contains(keywordLowerCase) || keywordLowerCase.contains(book.getPublisher().toLowerCase() ) ) ) {

                            recyclerViewAdapter.add(book);
                            remained--;

                    } else if(searchBy.equals(getResources().getString(R.string.search_searchBy_all)) ) {
                        if(book.getGenres() != null) {
                            int indexGenre = -1;

                            //find index of genre
                            for(int i=0; i < genresList.length; i++) {
                                if (genresList[i].toLowerCase().equals(keywordLowerCase)) {
                                    indexGenre = i;
                                    break;
                                }
                            }

                            if(indexGenre > -1) {
                                //Has the book the selected genre?
                                for (int genre : book.getGenres()) {
                                    if (indexGenre == genre) {
                                        recyclerViewAdapter.add(book);
                                        remained--;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    break;
                }

                //if not much books are found
                getBooks(keyword, remained);
                /*if(mLayoutManager.getItemCount() < 1) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }*/

                mIsLoading = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mIsLoading = false;
            }
        });
    }

    public void getKeys(double lat, double lon, double radius) {
        semaphoreKeyPrepared = false;

        keyBooks = new ArrayList<>();
        if(recyclerViewAdapter != null)
            recyclerViewAdapter.clearAll();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(FIREBASE_DATABASE_LOCATION_BOOKS_LOCATION);
        GeoFire geoFire = new GeoFire(ref);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lat, lon), radius);
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

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            //Log.i("google places api", "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            //Log.i("google places api", "Fetching details for ID: " + item.placeId);

            //selectedCity = item.description.toString();
            //idSelectedCity = item.placeId.toString();

            InputMethodManager in = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                //Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            LatLng latlng = place.getLatLng();
            lat = latlng.latitude;
            lon = latlng.longitude;

            getKeys(lat, lon, RADIUS);
            //Log.v("Latitude is", "" + latlng.latitude);
            //Log.v("Longitude is", "" + latlng.longitude);
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        //Log.i("google places api", "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Log.e("google places api", "Google Places API connection failed with error code: " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        //Log.e("google places api", "Google Places API connection suspended.");
    }

    //hide keyboard when you click away the EditText
    View.OnFocusChangeListener eventFocusChangeListenerCity = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                InputMethodManager inputMethodManager =(InputMethodManager)getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                String insertedCity = city.getText().toString();
                ArrayList<PlaceArrayAdapter.PlaceAutocomplete> list = mPlaceArrayAdapter.getListAutocomplete();
                if(list != null && list.size() > 0) {
                    for (PlaceArrayAdapter.PlaceAutocomplete element : list) {
                        if (element.description.equals(insertedCity)) {
                            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                                    .getPlaceById(mGoogleApiClient, list.get(0).placeId.toString());
                            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                            city.setText(list.get(0).description);
                            return;
                        }
                    }

                    PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                            .getPlaceById(mGoogleApiClient, list.get(0).placeId.toString());
                    placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
                    city.setText(list.get(0).description);
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            //mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
    }

    public final void selectGenre(int genre) {
        mSetDurationPresetRadioGroup.check( ((PresetValueButton) v.findViewById(R.id.search_searchBy_all)).getId() );
        searchView.setQuery(genresList[genre], true);
    }

    public void getLocationGPS() {
        if(boolSavedInstanceState) {
            isGps = false;
            getUserInfo();
            return;
        }

        if(latGPS != null && lonGPS != null) {
            lat = latGPS;
            lon = lonGPS;
            city.setText(getResources().getString(R.string.search_yourPosition));
            isGps = true;

            //get coordinates and nearby books
            getUserInfo();
            return;
        }

        //  Get user's current location
        locator = new LocatorSearch(getActivity(), context, new LocatorEventsListener() {

            @Override
            public void onSuccess(double latitude, double longitude) {
                lat = latitude;
                lon = longitude;
                latGPS = lat;
                lonGPS = lon;
                city.setText(getResources().getString(R.string.search_yourPosition));

                isGps = true;
                //Log.d("debug", "OnSuccess Lat: " +lat+ " Lon: "+lon);
                getUserInfo();
            }

            @Override
            public void onFailure() {
                isGps = false;
                //Log.d("debug", "OnFailure");
                getUserInfo();
            }

            @Override
            public void onPermissionDenied() {
                isGps = false;
                //Log.d("debug", "OnPermissionDenied");
                getUserInfo();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save coordinates to not ask coordinates again in the future
        outState.putDouble("lat", lat);
        outState.putDouble("lon", lat);

        if(latGPS != null)
            outState.putDouble("latGPS", latGPS);

        if(lonGPS != null)
            outState.putDouble("lonGPS", lonGPS);

        //save user data to not ask it again in the future
        if(myselfUser != null) {
            //convert in JSON the User object
            Gson gson = new Gson();
            String userJson = gson.toJson(myselfUser);
            outState.putString("user", userJson);
        }

        if(city != null) {
            outState.putString("selectedLocation",city.getText().toString());
        }

        if(searchBy != null) {
            outState.putString("searchBy", searchBy);
        }

    }

    public void getUserInfo() {
        if(myselfUser != null) {
            setLocationGenres();
            return;
        }

        //get user auth
        FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //get data from Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference(FIREBASE_DATABASE_LOCATION_USERS);

        usersRef.child(userAuth.getUid() ).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //get User object
                myselfUser = snapshot.getValue(UserMail.class);
                setLocationGenres();
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
    }

    public void setLocationGenres() {
        //set genre buttons
        ArrayList<Integer> listGenres = new ArrayList<>();
        ArrayList<Integer> favouriteGenres = myselfUser.getGenres();

        //set genre buttons
        RecyclerView myrv = (RecyclerView) v.findViewById(R.id.search_genres);
        myAdapter = new RecyclerViewAdapterGenre(getContext(), new ArrayList<Integer>(), this);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            myrv.setLayoutManager(new GridLayoutManager(getContext(),4));   //4 genres per row
        else
            myrv.setLayoutManager(new GridLayoutManager(getContext(),3));   //3 genres per row
        myrv.setAdapter(myAdapter);

        if(favouriteGenres != null) {
            for(int genre : favouriteGenres)
                listGenres.add(genre);
        }

        for(int i=0; i < 17; i++) {
            if(favouriteGenres == null || !favouriteGenres.contains(i))
                listGenres.add(i);
        }

        myAdapter.addAll(listGenres);

        if(!isGps) {
            //get coordinates
            lat = myselfUser.getLat();
            lon = myselfUser.getLon();

            city.setText(myselfUser.getCity());
        }

        //get nearby books
        getKeys(lat, lon, RADIUS);
    }

}

