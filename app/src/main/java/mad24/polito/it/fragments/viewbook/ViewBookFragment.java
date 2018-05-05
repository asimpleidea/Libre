package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mad24.polito.it.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ViewBookFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ViewBookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewBookFragment extends Fragment
{

    private BookViewPagerAdapter ViewPageAdapter = null;
    private ViewPager viewPager = null;
    private TabLayout Tabs = null;
    private View view = null;

    private final int BOOK_DETAILS = 0;
    private final int BOOK_OWNERS = 1;
    private final int BOOK_MAP = 2;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ViewBookFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ViewBookFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ViewBookFragment newInstance(String param1, String param2) {
        ViewBookFragment fragment = new ViewBookFragment();
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        /*return inflater.inflate(R.layout.fragment_view_book, container, false);*/
        view = inflater.inflate(R.layout.fragment_view_book, container, false);

        setUpViewPager();

        setUpTabs();





        /*final FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.main_frame);
        frameLayout.setBackgroundColor(0xe6f2a2);*/

        /*RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.dummyfrag_scrollableview);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        DessertAdapter adapter = new DessertAdapter(getContext());
        recyclerView.setAdapter(adapter);*/

        return view;
    }

    private void setUpTabs()
    {
        //------------------------------------
        //  Init
        //------------------------------------

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);

        //------------------------------------
        //  Set tabs behaviour
        //------------------------------------

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                viewPager.setCurrentItem(tab.getPosition(), true);

                switch (tab.getPosition()) {
                    case BOOK_DETAILS:
                        Log.d("VIEWBOOK", "Show book's details");
                        break;
                    case BOOK_OWNERS:
                        Log.d("VIEWBOOK", "Show book's owners");
                        break;
                    case BOOK_MAP:
                        Log.d("VIEWBOOK", "Show book's map");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}

        });
    }

    private void setUpViewPager()
    {
        //------------------------------------
        //  Init
        //------------------------------------

        viewPager = view.findViewById(R.id.viewPager);
        ViewPageAdapter = new BookViewPagerAdapter(getActivity().getSupportFragmentManager());

        //------------------------------------
        //  Add other fragments
        //------------------------------------

        ViewPageAdapter.addFragment(new BookDetailsFragment(), "Cacca");
        /*ViewPageAdapter.addFrag(new DummyFragment(
                ContextCompat.getColor(this, R.color.cyan_50)), "Cyan");

        adapter.addFrag(new DummyFragment(
                ContextCompat.getColor(this, R.color.amber_50)), "Amber");

        adapter.addFrag(new DummyFragment(
                ContextCompat.getColor(this, R.color.purple_50)), "Purple");*/

        viewPager.setAdapter(ViewPageAdapter);
        viewPager.setCurrentItem(BOOK_DETAILS, true);

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
