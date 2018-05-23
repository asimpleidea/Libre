package mad24.polito.it.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import mad24.polito.it.R;
import mad24.polito.it.chats.ConversationsAdapter;
import mad24.polito.it.models.Chat;
import mad24.polito.it.models.UserMail;
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
    private ListenerRegistration ChatsListener = null;

    private ConversationsAdapter Adapter = null;
    private LinearLayoutManager ViewManager = null;
    private RecyclerView RV = null;
    private final Boolean AdapterLock = true;

    private View RootView = null;
    private TextView NoChatsText = null;
    private ProgressBar Loading = null;

    public ChatFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_chat, container, false);

        RV = RootView.findViewById(R.id.chatsContainer);
        RV.setHasFixedSize(true);
        RV.setAdapter(Adapter);
        RV.setLayoutManager(ViewManager);

        NoChatsText = RootView.findViewById(R.id.noChatsText);
        Loading = RootView.findViewById(R.id.loadingScreen);

        return RootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //---------------------------------------------
        //  Is user logged?
        //---------------------------------------------

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

        //---------------------------------------------
        //  Recycler view related stuff
        //---------------------------------------------

        Adapter = new ConversationsAdapter(getContext(), MyID);
        ViewManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        load();
    }

    private void load()
    {
        ChatsCollection.orderBy("last_message_time", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>()
                {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e)
                    {
                        if(queryDocumentSnapshots == null) return;

                        boolean firstTime = false;

                        if(queryDocumentSnapshots.isEmpty()) return;

                        for(DocumentChange d : queryDocumentSnapshots.getDocumentChanges())
                        {
                            synchronized (AdapterLock)
                            {
                                switch(d.getType())
                                {
                                    case ADDED:
                                        Adapter.push(d.getDocument().toObject(Chat.class));
                                        break;

                                    case MODIFIED:
                                        Adapter.swap(d.getOldIndex(), d.getNewIndex(), d.getDocument().toObject(Chat.class));
                                        break;

                                    /*default:
                                        Log.d("CHAT", "default case");
                                        break;*/
                                }

                                if(Adapter.getItemCount() == 0)
                                {
                                    NoChatsText.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    if(RV.getVisibility() != View.VISIBLE)
                                    {
                                        NoChatsText.setVisibility(View.GONE);
                                        RV.setVisibility(View.VISIBLE);
                                    }
                                }

                                //  Anyway, hide the progress bar
                                if(Loading.getVisibility() == View.VISIBLE) Loading.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    /*@Override
    public void onPause()
    {
        super.onPause();

        ChatsListener.remove();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }*/
}

