package mad24.polito.it.chats

import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import mad24.polito.it.R
import mad24.polito.it.models.UserMail

class ChatActivity : AppCompatActivity()
{
    lateinit var MainReference : DatabaseReference
    lateinit var MessagesReference : DatabaseReference
    lateinit var MyFriendIsTyping : DatabaseReference
    lateinit var IAmTyping : DatabaseReference

    var ChatID : String? = null
    val Take : Int = 20
    val Me : String?  = FirebaseAuth.getInstance().currentUser?.uid
    var Them : String? = null
    var LastMessagePulled : String? = null
    var Initialized : Boolean = false
    var User : UserMail? = null

    //  TODO: Change this type: it won't be a textview on production, of course
    lateinit var TypingNotifier : TextView
    lateinit var Typer : EditText
    lateinit var StopTyping : CountDownTimer
    var CountDownRunning : Boolean = false
    val Seconds : Long = 3 *1000

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //  Initialize the main DB reference
        MainReference = FirebaseDatabase.getInstance().getReference("chat_messages")

        //  Get the other user
        if(intent.hasExtra("user") && intent.getStringExtra("user") != null)
        {
            User = Gson().fromJson(intent.getStringExtra("user"), UserMail::class.java)
            Them = intent.getStringExtra("with")
        }

        //  Set typing text
        //  TODO: use String.format(id, User?.name) instead of this hardcoded string
        TypingNotifier = findViewById(R.id.userIsTyping)
        TypingNotifier.setText("${User?.name} is typing... (THREE DOTS ANIMATION HERE?")

        //  The typer
        Typer = findViewById(R.id.typeText)

        //  Do we already have a chat stored?
        if(intent.hasExtra("chat") && intent.getStringExtra("chat") != null)
        {
            ChatID = intent.getStringExtra("chat")
            LastMessagePulled = intent.getStringExtra("start")

            //  Set up messages listener
            setUpMessagesListener()

            //  Set up typing listener
            setUpTypingListener()

            //  Set up the typing observer
            setUpTypingObserver()
        }

    }


    private fun setUpMessagesListener()
    {
        //  Set it up
        MessagesReference = MainReference.child(ChatID).child("messages")

        //  Set the event
        var query = MessagesReference.orderByChild("sent")

        //  TODO: check the limitToLast
        if(!Initialized) query = query.startAt(LastMessagePulled).limitToLast(Take)

        query.addValueEventListener(object : ValueEventListener
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

    private fun setUpTypingListener()
    {
        //  Get the reference
        MyFriendIsTyping = MainReference.child(ChatID).child("partecipants").child(Them)

        //  Set up listener
        MyFriendIsTyping.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?)
            {
                Log.d("CHAT", "typing listener error")
            }

            override fun onDataChange(p0: DataSnapshot?)
            {
                Log.d("CHAT", "on dataChange typing listener")
                if(!::TypingNotifier.isInitialized) return

                if(p0 != null)
                {
                    if(p0.hasChild("is_typing"))
                    {
                        //  TODO: add fade in & fade out animations here
                        if(p0.child("is_typing")?.value == true) TypingNotifier?.visibility = View.VISIBLE
                        else TypingNotifier?.visibility = View.GONE
                    }
                }
            }

        })
    }

    private fun setUpTypingObserver()
    {
        //  Set the typing reference
        IAmTyping = MainReference.child(ChatID).child("partecipants").child(Me)

        //  Set the countdown timer
        StopTyping = object : CountDownTimer(Seconds, 100)
        {
            override fun onFinish()
            {
                synchronized(this)
                {
                    IAmTyping.child("is_typing").setValue(false) { error, success ->
                        if(error != null)
                        {
                            Log.d("CHAT", "error: could not reset iamtyping")
                            return@setValue
                        }

                        Log.d("CHAT", "I am not typing anymore")

                    }
                    CountDownRunning = false
                }
            }

            override fun onTick(p0: Long)
            {
                Log.d("CHAT", "ticking")
            }

        }

        Typer.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN)
            {
                Log.d("CHAT", "DOWN")

                synchronized(this)
                {
                    if(!CountDownRunning)
                    {
                        // TODO: check if events are needed here
                        IAmTyping.child("is_typing").setValue(true) { p0, p1 ->
                            if(p0 != null)
                            {
                                Log.d("CHAT", "error")
                                return@setValue
                            }

                            Log.d("CHAT", "no error")
                            StopTyping.start()
                        }

                        CountDownRunning = true
                    }
                    else
                    {
                        StopTyping.cancel()
                        StopTyping.start()
                        Log.d("CHAT", "no typing because already did it and restarted")
                    }
                }

                return@OnKeyListener false
            }

            if(keyEvent.action == KeyEvent.ACTION_UP)
            {
                Log.d("CHAT", "UP")
            }

            return@OnKeyListener false

        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
