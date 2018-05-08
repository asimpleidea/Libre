package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import mad24.polito.it.R;
import mad24.polito.it.fragments.FragmentLoadingListener;
import mad24.polito.it.fragments.FragmentWithLoadingListener;
import mad24.polito.it.models.Book;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ViewBookFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ViewBookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewBookFragment extends Fragment implements FragmentWithLoadingListener
{
    private BookViewPagerAdapter ViewPageAdapter = null;
    private ViewPager viewPager = null;
    private View view = null;
    private BookDetailsFragment Details = null;
    private BookOwnerFragment Owner = null;
    private BookMapFragment Map = null;
    private TabLayout Tabs = null;
    private StorageReference Storage = null;

    private final int BOOK_DETAILS = 0;
    private final int BOOK_OWNER = 1;
    private final int BOOK_MAP = 2;

    private final String BOOK_DETAILS_TITLE = "Details";
    private final String BOOK_OWNER_TITLE = "Owner";
    private final String BOOK_MAP_TITLE = "Map";
    private final String BUNDLE_KEY = "book";

    private FragmentLoadingListener LoadingListener = null;

    private Book TheBook = null;
    private String JSONBook = null;

    private OnFragmentInteractionListener mListener;

    public ViewBookFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment ViewBookFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ViewBookFragment newInstance(String param1) {
        ViewBookFragment fragment = new ViewBookFragment();

        /*Bundle args = new Bundle();
        fragment.setArguments(args);*/

        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("VIEWBOOK", "destroyed view");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("VIEWBOOK", "destroyed fragment");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  Get the arguments
        if (getArguments() != null)
        {
            JSONBook = getArguments().getString(BUNDLE_KEY);
            TheBook = new Gson().fromJson(getArguments().getString(BUNDLE_KEY), Book.class);
            Storage = FirebaseStorage.getInstance().getReference().child("bookCovers").child(TheBook.getBook_id() + ".jpg");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_view_book, container, false);

        //  Load the Image
        Storage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
        {
            @Override
            public void onSuccess(Uri uri)
            {
                Glide.with(getActivity().getApplicationContext()).load(uri).into((ImageView) view.findViewById(R.id.bookCover));
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {

            }
        });

        Log.d("VIEWBOOK", "onCreateView");

        setUpDetails();

        setUpOwner();

        setUpMap();

        setUpViewPager();

        setUpTabs();

        //  Done! We finished the loading!
        if(LoadingListener != null)
        {
            LoadingListener.onFragmentLoaded();
            Log.d("VIEWBOOK", "Event raised successfully");
        }

        new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
                //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                Log.d("VIEWBOOK", "count down finished");
                ((ProgressBar)view.findViewById(R.id.loadingScreen)).setVisibility(View.GONE);
                ((AppBarLayout)view.findViewById(R.id.main_appbar)).setVisibility(View.VISIBLE);
                ((ViewPager)view.findViewById(R.id.viewPager)).setVisibility(View.VISIBLE);
            }
        }.start();

        return view;
    }

    private void setUpTabs()
    {
        //------------------------------------
        //  Init
        //------------------------------------

        Tabs = view.findViewById(R.id.tabLayout);

        //------------------------------------
        //  Set tabs behaviour
        //------------------------------------

        Tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                viewPager.setCurrentItem(tab.getPosition(), true);

                switch (tab.getPosition()) {
                    case BOOK_DETAILS:
                        Log.d("VIEWBOOK", "Show book's details");
                        Tabs.getTabAt(BOOK_DETAILS).setIcon(R.drawable.ic_book_selected);
                        break;
                    case BOOK_OWNER:
                        Log.d("VIEWBOOK", "Show book's owners");
                        Tabs.getTabAt(BOOK_OWNER).setIcon(R.drawable.ic_owner_selected);
                        break;
                    case BOOK_MAP:
                        Log.d("VIEWBOOK", "Show book's map");
                        Tabs.getTabAt(BOOK_MAP).setIcon(R.drawable.ic_marker_selected);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
                switch (tab.getPosition())
                {
                    case BOOK_DETAILS:
                        Log.d("VIEWBOOK", "Show book's details");
                        Tabs.getTabAt(BOOK_DETAILS).setIcon(R.drawable.ic_book_unselected);
                        break;
                    case BOOK_OWNER:
                        Log.d("VIEWBOOK", "Show book's owners");
                        Tabs.getTabAt(BOOK_OWNER).setIcon(R.drawable.ic_owner_unselected);
                        break;
                    case BOOK_MAP:
                        Log.d("VIEWBOOK", "Show book's map");
                        Tabs.getTabAt(BOOK_MAP).setIcon(R.drawable.ic_marker_unselected);
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}

        });
        Log.d("VIEWBOOK", "SetUpTabs called");
    }

    private void setUpViewPager()
    {
        //------------------------------------
        //  Init
        //------------------------------------

        viewPager = view.findViewById(R.id.viewPager);
        ViewPageAdapter = new BookViewPagerAdapter(getChildFragmentManager());

        //------------------------------------
        //  Add other fragments
        //------------------------------------

        ViewPageAdapter.addFragment(Details, BOOK_DETAILS_TITLE);
        ViewPageAdapter.addFragment(Owner, BOOK_OWNER_TITLE);
        ViewPageAdapter.addFragment(Map, BOOK_MAP_TITLE);

        //------------------------------------
        //  Set adapter and current item
        //------------------------------------

        viewPager.setAdapter(ViewPageAdapter);
        viewPager.setCurrentItem(BOOK_DETAILS, true);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                Tabs.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        Log.d("VIEWBOOK", "SetUpViewPager called");
    }

    private void setUpDetails()
    {
        Bundle b = new Bundle();
        b.putString(BUNDLE_KEY, JSONBook);

        Details = new BookDetailsFragment();
        Details.setArguments(b);
    }

    private void setUpOwner()
    {
        Bundle b = new Bundle();
        b.putString("owner", TheBook.getUser_id());
        b.putString("book", JSONBook);

        Owner = new BookOwnerFragment();
        Owner.setArguments(b);
    }

    private void setUpMap()
    {
        Map = new BookMapFragment();
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
        //  TODO: CHECK THIS! WHY?
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

    @Override
    public void setLoadingListener(FragmentLoadingListener loadingListener)
    {
        LoadingListener = loadingListener;
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
