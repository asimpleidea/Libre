package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.security.acl.Owner;

import mad24.polito.it.R;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.UserMail;
import mad24.polito.it.registrationmail.User;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookOwnerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookOwnerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookOwnerFragment extends Fragment
{
    private String UID = null;
    private DatabaseReference DB = null;
    private View RootView = null;
    private UserMail User = null;
    private StorageReference Storage = null;
    private Book TheBook = null;
    private String OwnerImage = null;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public BookOwnerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BookOwnerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookOwnerFragment newInstance(String param1, String param2) {
        BookOwnerFragment fragment = new BookOwnerFragment();
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
            UID = getArguments().getString("owner");
            DB = FirebaseDatabase.getInstance().getReference()
                    .child("users/" + UID);
            TheBook = new Gson().fromJson(getArguments().getString("book"), Book.class);
            Storage = FirebaseStorage.getInstance().getReference("profile_pictures").child(UID + ".jpg");
        }
    }

    private void loadAndInjectUser()
    {
        DB.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                User = dataSnapshot.getValue(UserMail.class);
                injectUser();
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d("VIEWBOOK", "can't load user");
            }
        });
    }

    private void injectUser()
    {
        if(User == null) return;

        //  Put the owner's pic
        if(OwnerImage == null)
        {
            Storage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
            {
                @Override
                public void onSuccess(Uri uri)
                {
                    //  TODO: check for caching options.
                    Glide.with(getContext()).load(uri).into((ImageView) RootView.findViewById(R.id.ownerPic));
                    OwnerImage = uri.toString();
                }
            }).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    //  Failed...
                }
            });
        }
        else Glide.with(getContext()).load(OwnerImage).into((ImageView) RootView.findViewById(R.id.ownerPic));


        //  The name
        ((TextView) RootView.findViewById(R.id.ownerName)).setText(User.getName());

        //  The Conditions
        ((TextView) RootView.findViewById(R.id.bookConditions)).setText(String.format(getResources().getString(R.string.book_conditions_user), TheBook.getCondition()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_book_owner, container, false);

        if(User == null) loadAndInjectUser();
        else injectUser();

        return RootView;
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
