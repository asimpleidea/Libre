package mad24.polito.it.chats

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_chat.*
import mad24.polito.it.R

class ChatActivity : AppCompatActivity()
{
    var MainReference : DatabaseReference? = null
    var MessagesReference : DatabaseReference? = null
    var ChatID : String? = null;
    val Take : Int = 20
    val Me : String?  = FirebaseAuth.getInstance().currentUser?.uid
    var LastMessagePulled : String? = null
    var Initialized : Boolean = false

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                message.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                message.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //  Initialize the main DB reference
        MainReference = FirebaseDatabase.getInstance().getReference("chat_messages")

        //  Do we already have a chat stored?
        if(intent.hasExtra("chat") && intent.getStringExtra("chat") != null)
        {
            ChatID = intent.getStringExtra("chat")
            LastMessagePulled = intent.getStringExtra("start")

            //  Set up messages listener
            setUpMessagesListener()
        }

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun setUpMessagesListener()
    {
        //  Set it up
        MessagesReference = MainReference?.child(ChatID)?.child("messages")

        //  Set the event
        var query = MessagesReference?.orderByChild("sent")

        //  TODO: check the limitToLast
        if(!Initialized) query = query?.startAt(LastMessagePulled)?.limitToLast(Take)

        query?.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?)
            {
                Log.d("CHAT", "on Cancelled")
            }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 == null) return
                for(p : DataSnapshot in p0.children)
                {
                    Log.d("CHAT", "onDataChange: ${p.child("").key}")
                }

            }

        })
    }
}
