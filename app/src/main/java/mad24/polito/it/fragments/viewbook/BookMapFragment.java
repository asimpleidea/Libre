package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import mad24.polito.it.R;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;

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
        if (getArguments() != null)
        {
            TheBook = new Gson().fromJson(getArguments().getString("book"), Book.class);
            if(TheBook != null)
            {
                UID = TheBook.getUser_id();
                DB = FirebaseDatabase.getInstance().getReference()
                        .child("users/" + UID);
            }
        }
    }

    private void loadAndInjectData()
    {
        DB.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Owner = dataSnapshot.getValue(UserMail.class);
                if(dataSnapshot.hasChild("lat") && dataSnapshot.hasChild("lon"))
                {
                    Coordinates = new LatLng(dataSnapshot.child("lat").getValue(Double.class), dataSnapshot.child("lon").getValue(Double.class));
                    injectData();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d("VIEWBOOK", "can't load user");
            }
        });
    }

    private void injectData()
    {
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
                public void onMapReady(GoogleMap mMap) {
                    googleMap = mMap;

                    // For showing a move to my location button
                    //googleMap.setMyLocationEnabled(true);

                    Log.d("VIEWBOOK", "Latitude: " + Coordinates.latitude);
                    Log.d("VIEWBOOK", "Longitude: " + Coordinates.longitude);
                    // For dropping a marker at a point on the Map
                    LatLng sydney = new LatLng(Coordinates.latitude, Coordinates.longitude);
                    googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));

                    // For zooming automatically to the location of the marker
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
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
        else injectData();

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
