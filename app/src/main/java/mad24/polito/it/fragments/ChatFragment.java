package mad24.polito.it.fragments;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import mad24.polito.it.R;
import mad24.polito.it.models.UserMail;
import mad24.polito.it.models.UserStatus;
import mad24.polito.it.registrationmail.LoginActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment
{
    private UserMail Me = null;
    private DatabaseReference MainReference = null;
    private DatabaseReference ChatsReference = null;
    private String MyID = null;

    private FirebaseFirestore Firestore = null;
    private CollectionReference ChatsCollection = null;

    private View RootView = null;

    public ChatFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_chat, container, false);

        return RootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //---------------------------------------------
        //  Is user logged?
        //---------------------------------------------
Log.d("CHAT", "TESTING");
        if(FirebaseAuth.getInstance().getCurrentUser() == null)
        {
            Intent i =  new Intent(getActivity().getApplicationContext(), LoginActivity.class);
            startActivity(i);
            getActivity().finish();
        }

        //  My id
        MyID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //---------------------------------------------
        //  Inits
        //---------------------------------------------

        //  Init firestore
        Firestore = FirebaseFirestore.getInstance();

        //  The chats collection
        ChatsCollection = Firestore.collection("chats/" + MyID + "/conversations");


        load();
        //  Set up MainReference
       /* MainReference = FirebaseDatabase.getInstance().getReference();

        //  Get Data
        MainReference.child("users").child(MyID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                //  TODO: check for errors here
                if(dataSnapshot != null)
                {
                    Me = dataSnapshot.getValue(UserMail.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d("CHAT", "Error while trying to get user");
            }
        });*/
    }

    private void load()
    {
        //  Load
        ChatsCollection.orderBy("last_message_time").get()

                //  Everything ok?
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        if(!queryDocumentSnapshots.isEmpty())
                        {
                            Log.d("CHAT", "loaded: " + queryDocumentSnapshots.size());
                        }
                    }
                })

                //  Not ok?
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Log.d("CHAT", "Could not load chats");
                }
            });
    }
}

