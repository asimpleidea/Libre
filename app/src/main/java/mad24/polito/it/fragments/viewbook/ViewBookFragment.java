package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
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
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import mad24.polito.it.BooksActivity;
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
    private BookDetailsFragment details = null;
    private BookOwnerFragment owner = null;
    private BookBorrowerFragment borrower = null;
    private BookMapFragment map = null;
    private TabLayout tabs = null;
    private boolean AlreadyVisible = false;

    private final int BOOK_DETAILS = 0;
    private final int BOOK_DETAILS_2 = 1;
    private final int BOOK_OWNER = 1;
    private final int BOOK_MAP = 2;

    private final int BOOK_BORROWER = 0;

    private final String BOOK_DETAILS_TITLE = "details";
    private final String BOOK_OWNER_TITLE = "owner";
    private final String BOOK_MAP_TITLE = "map";
    private final String BOOK_BORROWER_TITLE = "borrower";
    private final String BUNDLE_KEY = "book";
    private final String BUNDLE_FRAGMENT = "fragment";
    StorageReference Storage = null;

    private FragmentLoadingListener LoadingListener = null;

    private Book TheBook = null;
    private String JSONBook = null;
    private int fragment; // 0: bookFragment; 1: profileFragment

    private OnFragmentInteractionListener mListener;

    public ViewBookFragment()
    {
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
    public void onDestroyView()
    {
        super.onDestroyView();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ((BooksActivity) getActivity()).setCurrentFragment(fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //  Get the arguments
        if (getArguments() != null)
        {
            fragment = getArguments().getInt(BUNDLE_FRAGMENT);
            JSONBook = getArguments().getString(BUNDLE_KEY);
            TheBook = new Gson().fromJson(getArguments().getString(BUNDLE_KEY), Book.class);
            //Storage = FirebaseStorage.getInstance().getReference().child("bookCovers");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        switch(fragment) {
            case 1:
                view = inflater.inflate(R.layout.fragment_view_book_owner, container, false);
                setUpDetails();
                setUpBorrower();
                break;
            default:
                view = inflater.inflate(R.layout.fragment_view_book, container, false);
                setUpDetails();

                setUpOwner();

                setUpMap();
        }

        setUpViewPager();

        setUpTabs();

        //  Load the book cover
        /*if(TheBook.getBookImageLink() != null && !TheBook.getBookImageLink().isEmpty())
        {
            Glide.with(getActivity().getApplicationContext()).load(TheBook.getBookImageLink()).into((ImageView) view.findViewById(R.id.bookCover));
        }*/

        //  Done! We finished the loading!
        if(LoadingListener != null)
        {
            LoadingListener.onFragmentLoaded();
            Log.d("VIEWBOOK", "Event raised successfully");
        }

        return view;
    }

    private void setUpTabs()
    {
        //------------------------------------
        //  Init
        //------------------------------------
        switch(fragment){
            case 1:
                tabs = view.findViewById(R.id.tabLayoutOwner);
                break;
            default:
                tabs = view.findViewById(R.id.tabLayout);
        }

        //------------------------------------
        //  Set tabs behaviour
        //------------------------------------

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                viewPager.setCurrentItem(tab.getPosition(), true);

                switch(fragment){
                    case 1:
                        switch (tab.getPosition()) {
                            case BOOK_BORROWER:
                                Log.d("VIEWBOOK", "Show book's borrowers");
                                tabs.getTabAt(BOOK_BORROWER).setIcon(R.drawable.ic_owner_selected_new); // TODO: change drawable
                                break;
                            case BOOK_DETAILS_2:
                                Log.d("VIEWBOOK", "Show book's owner");
                                tabs.getTabAt(BOOK_DETAILS_2).setIcon(R.drawable.ic_book_selected_new);
                                break;
                        }
                        break;
                    default:
                        switch (tab.getPosition()) {
                            case BOOK_DETAILS:
                                Log.d("VIEWBOOK", "Show book's details");
                                tabs.getTabAt(BOOK_DETAILS).setIcon(R.drawable.ic_book_selected_new);
                                break;
                            case BOOK_OWNER:
                                Log.d("VIEWBOOK", "Show book's owners");
                                tabs.getTabAt(BOOK_OWNER).setIcon(R.drawable.ic_owner_selected_new);
                                break;
                            case BOOK_MAP:
                                Log.d("VIEWBOOK", "Show book's map");
                                tabs.getTabAt(BOOK_MAP).setIcon(R.drawable.ic_marker_selected_new);
                                break;
                        }
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
                switch(fragment){
                    case 1:
                        switch (tab.getPosition())
                        {
                            case BOOK_BORROWER:
                                Log.d("VIEWBOOK", "Show book's borrower");
                                tabs.getTabAt(BOOK_BORROWER).setIcon(R.drawable.ic_owner_unselected_new);
                                break;
                            case BOOK_DETAILS_2:
                                Log.d("VIEWBOOK", "Show book's details");
                                tabs.getTabAt(BOOK_DETAILS_2).setIcon(R.drawable.ic_book_unselected_new);
                                break;
                        }
                        break;
                    default:
                        switch (tab.getPosition())
                        {
                            case BOOK_DETAILS:
                                Log.d("VIEWBOOK", "Show book's details");
                                tabs.getTabAt(BOOK_DETAILS).setIcon(R.drawable.ic_book_unselected_new);
                                break;
                            case BOOK_OWNER:
                                Log.d("VIEWBOOK", "Show book's owners");
                                tabs.getTabAt(BOOK_OWNER).setIcon(R.drawable.ic_owner_unselected_new);
                                break;
                            case BOOK_MAP:
                                Log.d("VIEWBOOK", "Show book's map");
                                tabs.getTabAt(BOOK_MAP).setIcon(R.drawable.ic_marker_unselected_new);
                                break;
                        }
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
        switch(fragment){
            case 1:
                viewPager = view.findViewById(R.id.viewPagerOwner);
                break;
            default:
                viewPager = view.findViewById(R.id.viewPager);
        }

        ViewPageAdapter = new BookViewPagerAdapter(getChildFragmentManager());

        //------------------------------------
        //  Add other fragments
        //------------------------------------

        switch(fragment){
            case 1:
                ViewPageAdapter.addFragment(borrower, BOOK_BORROWER_TITLE);
                ViewPageAdapter.addFragment(details, BOOK_DETAILS_TITLE);
                break;
            default:
                ViewPageAdapter.addFragment(details, BOOK_DETAILS_TITLE);
                ViewPageAdapter.addFragment(owner, BOOK_OWNER_TITLE);
                ViewPageAdapter.addFragment(map, BOOK_MAP_TITLE);
        }

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
                tabs.getTabAt(position).select();
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

        details = new BookDetailsFragment();
        details.setArguments(b);
        details.setLoadingListener(new FragmentLoadingListener() {
            @Override
            public void onFragmentLoaded()
            {
                //  Nothing...
            }

            @Override
            public void onFragmentLoaded(String arg)
            {
                if(AlreadyVisible) return;

                //  Has no image?
                ImageView cover = view.findViewById(R.id.bookCover);
                if(TheBook.getBookImageLink() == null || (TheBook.getBookImageLink() != null && TheBook.getBookImageLink().isEmpty()))
                {
                    if(arg != null)
                    {
                        Glide.with(getContext()).load(arg)
                                .listener(new RequestListener<String, GlideDrawable>() {
                                    @Override
                                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource)
                                    {
                                        showFragment();
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource)
                                    {
                                        showFragment();
                                        return false;
                                    }
                                }).into(cover);

                        TheBook.setBookImageLink(arg);
                    }
                    else
                    {

                        cover.setImageDrawable(getResources().getDrawable(R.drawable.default_book_cover));
                        showFragment();
                    }
                }
                else showFragment();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    private void showFragment()
    {
        ((ProgressBar)view.findViewById(R.id.loadingScreen)).setVisibility(View.GONE);
        AppBarLayout appBar = view.findViewById(R.id.appBar);

        //  When on landscape...
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            appBar.setExpanded(false);
        }

        appBar.setVisibility(View.VISIBLE);
        switch(fragment){
            case 1:
                ((ViewPager)view.findViewById(R.id.viewPagerOwner)).setVisibility(View.VISIBLE);
                break;
            default:
                ((ViewPager)view.findViewById(R.id.viewPager)).setVisibility(View.VISIBLE);
        }
        AlreadyVisible = true;
    }

    private void setUpOwner()
    {
        Bundle b = new Bundle();
        b.putString("owner", TheBook.getUser_id());
        b.putString("book", JSONBook);

        owner = new BookOwnerFragment();
        owner.setArguments(b);
    }

    private void setUpBorrower(){
        Bundle b = new Bundle();
        b.putString("borrower", "MarcoSchiuma"); // TODO: set useful data to the borrower fragment constructor
        b.putString("book", JSONBook);

        borrower = new BookBorrowerFragment();
        borrower.setArguments(b);
    }

    private void setUpMap()
    {
        Bundle b = new Bundle();
        b.putString("book", JSONBook);

        map = new BookMapFragment();
        map.setArguments(b);
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
