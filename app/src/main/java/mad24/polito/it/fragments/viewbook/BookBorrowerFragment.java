package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.util.Locale;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;
import mad24.polito.it.chats.ChatActivity;
import mad24.polito.it.fragments.profile.RatingDialog;
import mad24.polito.it.models.Book;
import mad24.polito.it.models.Borrowing;
import mad24.polito.it.models.UserMail;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookBorrowerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookBorrowerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookBorrowerFragment extends Fragment
{
    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;
    private static final String FIREBASE_DATABASE_LOCATION_BORROWINGS = BooksActivity.FIREBASE_DATABASE_LOCATION_BORROWINGS;
    public static final String FIREBASE_DATABASE_LOCATION_BOOKS = BooksActivity.FIREBASE_DATABASE_LOCATION_BOOKS;

    private String UID = null;
    private DatabaseReference DBBorrowing = null;
    private DatabaseReference DB = null;
    private View RootView = null;
    private TextView textInYourPossession = null;
    private TextView textLoan = null;
    private Button buttonChat = null;
    private Button buttonTerminateLoan = null;
    private LinearLayout borrowerLayout = null;
    private UserMail User = null;
    private StorageReference Storage = null;
    private Book TheBook = null;
    private String BorrowerImage = null;
    private Borrowing borrowing = null;

    private TextView stars = null;
    private me.zhanghai.android.materialratingbar.MaterialRatingBar ratingBar;
    private TextView ratingNumber = null;
    private Button commentsButton = null;

    private OnFragmentInteractionListener mListener;

    public BookBorrowerFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BookOwnerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookBorrowerFragment newInstance() {
        BookBorrowerFragment fragment = new BookBorrowerFragment();
        //Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (getArguments() != null)
        {
            TheBook = new Gson().fromJson(getArguments().getString("book"), Book.class);

            if(TheBook.getBorrowing_id() != null && !TheBook.getBorrowing_id().isEmpty()) {
                DBBorrowing = FirebaseDatabase.getInstance().getReference()
                        .child(FIREBASE_DATABASE_LOCATION_BORROWINGS + "/" + TheBook.getBorrowing_id());
            }
        }

    }

    private void loadAndInjectBorrowing()
    {
        if(TheBook.getBorrowing_id() != null && !TheBook.getBorrowing_id().isEmpty()) {
            borrowerLayout.setVisibility(View.VISIBLE);
            textInYourPossession.setVisibility(View.GONE);
        } else {
            borrowerLayout.setVisibility(View.GONE);
            textInYourPossession.setVisibility(View.VISIBLE);
            return;
        }

        DBBorrowing.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                borrowing = dataSnapshot.getValue(Borrowing.class);

                //if you are the borrower --> set the owner id
                //if you are the owner --> set the borrower id
                if(borrowing.getTo().equals(FirebaseAuth.getInstance().getUid())) {
                    textLoan.setText(R.string.bookDetail_bookInYourPossessionRemember);
                    UID = borrowing.getFrom();
                } else {
                    textLoan.setText(R.string.book_on_loan);
                    UID = borrowing.getTo();
                }

                DB = FirebaseDatabase.getInstance().getReference()
                        .child(FIREBASE_DATABASE_LOCATION_USERS + "/" + UID);
                TheBook = new Gson().fromJson(getArguments().getString("book"), Book.class);
                Storage = FirebaseStorage.getInstance().getReference("profile_pictures").child(UID + ".jpg");

                buttonChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        /*DatabaseReference chats =*/
                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("chats")
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .child(UID)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //  Init the intent
                                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                                        intent.putExtra("partner_id", UID);
                                        intent.putExtra("book_id", TheBook.getBook_id());

                                        //  Start the activity
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //  TODO: display an error message?
                                        Log.d("BOOKVIEW", "on error");
                                    }
                                });
                    }
                });

                buttonTerminateLoan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String borrowingId = TheBook.getBorrowing_id();
                        String temp;
                        if(TheBook.getUser_id().equals(FirebaseAuth.getInstance().getUid()))
                            temp = "owner";
                        else
                            temp = "borrower";

                        final String borrowerOrOwner = temp;

                        //create the AlertDialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        //builder.setTitle("Add Photo!");
                        //  Set title and message
                        builder.setMessage(R.string.bookDetail_terminateLoanDialogMessage)
                                .setTitle(R.string.bookDetail_terminateLoanDialogTitle);

                        //  Set negative button
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                dialogInterface.dismiss();
                            }
                        });

                        //  Set positive button
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Task task = FirebaseDatabase.getInstance().getReference()
                                        .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                                        .child(TheBook.getBook_id())
                                        .child("borrowing_id")
                                        .setValue(new String(""));

                                task.addOnSuccessListener(new OnSuccessListener() {
                                    @Override
                                    public void onSuccess(Object o) {
                                        //create dialog to rate the other user
                                        RatingDialog ratingDialog = new RatingDialog(getActivity(), borrowingId, borrowerOrOwner);

                                        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                                        lp.copyFrom(ratingDialog.getWindow().getAttributes());
                                        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                                        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                                        ratingDialog.show();
                                        ratingDialog.getWindow().setAttributes(lp);


                                        borrowerLayout.setVisibility(View.GONE);

                                        if(borrowerOrOwner.equals("borrower"))
                                            textInYourPossession.setText(getResources().getString(R.string.bookDetail_bookReturned));

                                        textInYourPossession.setVisibility(View.VISIBLE);
                                        TheBook.setBorrowing_id("");
                                    }
                                });

                                task.addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //show error dialog
                                        showErrorDialog(getResources().getString(R.string.bookDetail_errorTerminatingLoan));
                                    }
                                });
                            }
                        });

                        //show the AlertDialog on the screen
                        builder.show();
                    }
                });



                loadAndInjectUser();
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d("VIEWBOOK", "can't load user");
            }
        });
    }

    private void loadAndInjectUser()
    {
        if(TheBook.getBorrowing_id() != null && !TheBook.getBorrowing_id().isEmpty()) {
            borrowerLayout.setVisibility(View.VISIBLE);
            textInYourPossession.setVisibility(View.GONE);
        } else {
            borrowerLayout.setVisibility(View.GONE);
            textInYourPossession.setVisibility(View.VISIBLE);
            return;
        }

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

    private void injectBorrowing() {
        if(borrowing.getTo().equals(FirebaseAuth.getInstance().getUid()))
            textLoan.setText(R.string.bookDetail_bookInYourPossessionRemember);
        else
            textLoan.setText(R.string.book_on_loan);

        injectUser();
    }

    private void injectUser()
    {
        if(User == null) return;

        //  Put the owner's pic
        if(BorrowerImage == null)
        {
            Storage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
            {
                @Override
                public void onSuccess(Uri uri)
                {
                    //  TODO: check for caching options.
                    Glide.with(getContext()).load(uri).into((ImageView) RootView.findViewById(R.id.borrowerPic));
                    BorrowerImage = uri.toString();
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
        else Glide.with(getContext()).load(BorrowerImage).into((ImageView) RootView.findViewById(R.id.borrowerPic));

        //  The name
        ((TextView) RootView.findViewById(R.id.borrowerName)).setText(User.getName());

        //User rating
        String ratingStarsString = "0.0";
        float ratingStars = 0;

        //to avoid division by 0
        if(User.getRaters() > 0) {
            ratingStars = (float) User.getRating() / (float) User.getRaters();
            ratingStarsString = String.format("%.1f", ratingStars);
        }

        stars.setText(ratingStarsString);
        ratingBar.setRating(ratingStars);
        ratingNumber.setText(String.format(getString(R.string.user_ratings_count), User.getRaters()));

        commentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UID == null)
                    return;

                //create dialog to rate the other user
                CommentDialog commentDialog = new CommentDialog(getActivity(), UID, User.getName());

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(commentDialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                commentDialog.show();
                commentDialog.getWindow().setAttributes(lp);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_book_borrower, container, false);

        textInYourPossession = (TextView) RootView.findViewById(R.id.borrower_inyourpossession);
        textLoan = (TextView) RootView.findViewById(R.id.borrowerTextLoan);
        buttonChat = (Button) RootView.findViewById(R.id.borrower_startChat);
        buttonTerminateLoan = (Button) RootView.findViewById(R.id.borrower_terminateLoan);
        borrowerLayout = (LinearLayout) RootView.findViewById(R.id.borrowerLayout);

        stars = (TextView) RootView.findViewById(R.id.borrowerStars);
        ratingBar = (me.zhanghai.android.materialratingbar.MaterialRatingBar) RootView.findViewById(R.id.borrowerMaterialRatingBar);
        ratingNumber = (TextView) RootView.findViewById(R.id.borrowerRatingNumber);
        commentsButton = (Button) RootView.findViewById(R.id.borrowerComments);

        if(User == null) {
            loadAndInjectBorrowing();
        } else {
            injectBorrowing();
        }

        return RootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
       /* if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        //mListener = null;
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
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void showErrorDialog(String title) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
