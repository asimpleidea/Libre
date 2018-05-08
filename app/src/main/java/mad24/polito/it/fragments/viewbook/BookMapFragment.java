package mad24.polito.it.fragments.viewbook;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import mad24.polito.it.R;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;
import mad24.polito.it.registrationmail.User;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookMapFragment extends Fragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View RootView = null;
    private MapView mMapView = null;
    private GoogleMap googleMap = null;
    private Book TheBook = null;
    private String UID = null;
    private UserMail Owner = null;
    private LatLng Coordinates = null;
    private DatabaseReference DB = null;
    private FusedLocationProviderClient LocationClient = null;
    private LatLng UserLocation = null;
    private final int PERMISSION_REQUEST_LOCATION = 20;


    private OnFragmentInteractionListener mListener;

    public BookMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BookMapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookMapFragment newInstance(String param1, String param2) {
        BookMapFragment fragment = new BookMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TheBook = new Gson().fromJson(getArguments().getString("book"), Book.class);
            if (TheBook != null) {
                UID = TheBook.getUser_id();
                DB = FirebaseDatabase.getInstance().getReference()
                        .child("users/" + UID);
            }
        }
    }

    private void loadAndInjectData() {
        if (TheBook == null) return;

        DB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Owner = dataSnapshot.getValue(UserMail.class);
                if (dataSnapshot.hasChild("lat") && dataSnapshot.hasChild("lon"))
                {
                    Coordinates = new LatLng(dataSnapshot.child("lat").getValue(Double.class), dataSnapshot.child("lon").getValue(Double.class));
                    askForPermission();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d("VIEWBOOK", "can't load user");
            }
        });
    }

    private void askForPermission()
    {
        if (TheBook == null) return;

        Log.d("VIEWBOOK", "Asking for permissions...");
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED /*&& ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/)
        {
            Log.d("VIEWBOOK", "Not permission, getting...");
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
        else
        {
            Log.d("VIEWBOOK", "Already have granted.");
            getLocation();
        }
    }

    private void getLocation()
    {
        Log.d("VIEWBOOK", "Getting current location");

        try
        {
            //  The criteria. This is probably not the most recommended method by Google, but we're short on time so...
            //  you know... done is better than perfect...
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

            LocationManager locManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

            locManager.requestSingleUpdate(criteria, new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    if(location != null)
                    {
                        Log.d("VIEWBOOK", "location is not null: lat:" + location.getLatitude() + ", long: " + location.getLongitude());
                        UserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                    injectData();
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle)
                {
                    Log.d("VIEWBOOK", "on status changed");
                }

                @Override
                public void onProviderEnabled(String s)
                {
                    Log.d("VIEWBOOK", "on provider enabled");
                }

                @Override
                public void onProviderDisabled(String s)
                {
                    Log.d("VIEWBOOK", "on provider disabled");
                }
            }, null);
        }
        catch (SecurityException s)
        {
            Log.d("VIEWBOOK", "on exception");
            injectData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case PERMISSION_REQUEST_LOCATION:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) getLocation();
                else injectData();
            }
            break;
        }
    }

    private void injectData()
    {
        Log.d("VIEWBOOK", "Injecting data...");
        try
        {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap mMap)
                {
                    Log.d("VIEWBOOK", "Map is ready");
                    googleMap = mMap;

                    // For showing a move to my location button
                    googleMap.setMyLocationEnabled(true);

                    /*if(UserLocation != null)
                    {
                        googleMap.addMarker(new MarkerOptions().position(UserLocation).title(getString(R.string.my_position)));
                    }*/

                    googleMap.addMarker(new MarkerOptions().position(Coordinates).title(TheBook.getTitle()));

                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(Coordinates).zoom(15).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });
        }
        catch(SecurityException s)
        {
            Log.d("VIEWBOOK", "NO permissions");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_book_map, container, false);

        mMapView = (MapView) RootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        if(Owner == null || Coordinates == null) loadAndInjectData();
        else askForPermission();

        /*try
        {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

            LocationManager locManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

            locManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location)
                {
                    Log.d("VIEWBOOK", "on lcation changed");
                    if(location != null)
                    {
                        Log.d("VIEWBOOK", "location is not null");
                    }
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle)
                {
                    Log.d("VIEWBOOK", "on status changed");
                }

                @Override
                public void onProviderEnabled(String s)
                {
                    Log.d("VIEWBOOK", "on provider enabled");
                }

                @Override
                public void onProviderDisabled(String s)
                {
                    Log.d("VIEWBOOK", "on provider disabled");
                }
            }, null);
        }
        catch (SecurityException s)
        {
            Log.d("VIEWBOOK", "on exception");
        }*/

        //Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        return RootView;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
