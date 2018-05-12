package mad24.polito.it.chats

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import mad24.polito.it.R
import mad24.polito.it.models.ChatMessage
import mad24.polito.it.models.ChatMessageContainer
import mad24.polito.it.models.UserMail
import mad24.polito.it.registrationmail.LoginActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity()
{
    lateinit var MainReference : DatabaseReference
    lateinit var MessagesReference : DatabaseReference
    lateinit var TheyAreTyping : DatabaseReference
    lateinit var IAmTyping : DatabaseReference

    var ChatID : String? = null
    var Take : Int = 20
    val Me : String? = FirebaseAuth.getInstance().currentUser?.uid
    var Them : String? = null
    var MostRecentMessaged : String? = null
    var OldestMessage : String? = null
    var Initialized : Boolean = false
    var User : UserMail? = null

    //  TODO: Change this type: it won't be a textview on production, of course
    lateinit var TypingNotifier : TextView
    lateinit var Typer : EditText
    lateinit var SubmitButton : Button
    lateinit var StopTyping : CountDownTimer
    var CountDownRunning : Boolean = false
    val Seconds : Long = 3 *1000

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        //  Login, man!
        if(FirebaseAuth.getInstance().currentUser == null)
        {
            val i : Intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(i)
            finish()
        }

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

        //  The submitter
        SubmitButton = findViewById(R.id.submitButton)

        //  Set up the typing observer
        setUpTypingObserver()

        ChatID = "-LCJ2HXE0ECtlt_5oFF0"
        MostRecentMessaged = "-LCJ2VQqpxPE7P4cbd_8"
        //  Set up other stuff
        setUps()
        //  Do we already have a chat stored?
        if(intent.hasExtra("chat") && intent.getStringExtra("chat") != null)
        {
            //ChatID = intent.getStringExtra("chat")
            //MostRecentMessaged = intent.getStringExtra("start")

            //  Set up other stuff
            //setUps()
        }
    }

    private fun setUps()
    {
        //  Typing observer
        setUpTypingObserver()

        //  Set up messages listener
        setUpMessagesListener()

        //  Set up typing listener
        setUpTypingListener()
    }

    private fun setUpMessagesListener()
    {
        Log.d("CHAT", "on setUpMessagesListener")

        //  Set it up
        MessagesReference = MainReference.child(ChatID).child("messages")

        synchronized(this.Initialized)
        {
            if(!Initialized) getFirstMessages()
            else listenForNewMessages()
        }
    }

    private fun getFirstMessages()
    {
        val t = this

        //  Set the event
        var query = MessagesReference.orderByChild("sent")

        //  TODO: check the limitToLast
        .startAt(MostRecentMessaged).limitToLast(Take)

        query.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?)
            {
                Log.d("CHAT", "on Cancelled")
            }

            override fun onDataChange(p0: DataSnapshot?)
            {
                synchronized(t.Initialized)
                {
                    Initialized = true
                    Take = 1
                }

                if(p0 == null) return
                for(p : DataSnapshot in p0.children)
                {
                    Log.d("CHAT", "message: ${p.child("content").getValue(String::class.java)}")
                }

                listenForNewMessages()
            }
        })
    }

    private fun listenForNewMessages()
    {
        //  Set the event
        var query = MessagesReference.orderByChild("sent")

        .startAt(MostRecentMessaged).limitToLast(1)

        query.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?)
            {
                Log.d("CHAT", "on Cancelled")
            }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 == null) return

                //  TODO: probably a check to see if we loaded them all
                if(p0.children.count() < Take) return


                //  TODO: Discard the first message because it is already there

                if(p0.key == MostRecentMessaged) return

                //  TODO: check if this is correct
                MostRecentMessaged = p0.children.first().key

                Log.d("CHAT", "new message: ${p0.child("content").getValue(String::class.java)}")

            }
        })
    }

    private fun setUpTypingListener()
    {
        //  Get the reference
        TheyAreTyping = MainReference.child(ChatID).child("partecipants").child(Them)

        //  Set up listener
        TheyAreTyping.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?)
            {
                Log.d("CHAT", "typing listener error")
            }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(!::TypingNotifier.isInitialized) return
                Log.d("CHAT", "onDataChange of typing")
                if(p0 != null)
                {
                    Log.d("CHAT", "onDataChange of typing: not null")
                    if(p0.hasChild("is_typing"))
                    {Log.d("CHAT", "onDataChange hastyping")
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
        val t = this

        //  Set the button
        setUpButtonEvent()

        if(ChatID == null) return

        //  Set the typing reference
        IAmTyping = MainReference.child(ChatID).child("partecipants").child(Me)

        //  Set the countdown timer
        StopTyping = object : CountDownTimer(Seconds, Seconds)
        {
            override fun onFinish()
            {
                synchronized(t.CountDownRunning)
                {
                    IAmTyping.child("is_typing").setValue(false) { error, success ->
                        if(error != null)
                        {
                            Log.d("CHAT", "error: could not reset iamtyping")
                            return@setValue
                        }
                    }

                    CountDownRunning = false
                }
            }

            override fun onTick(p0: Long) {}
        }

        Typer.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN)
            {
                synchronized(t.CountDownRunning)
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

                            StopTyping.start()
                        }

                        CountDownRunning = true
                    }
                    else
                    {
                        StopTyping.cancel()
                        StopTyping.start()
                    }
                }
            }

            return@OnKeyListener false

        })
    }

    private fun setUpButtonEvent()
    {
        //  Already has the event?
        if(SubmitButton.hasOnClickListeners()) return

        //  Set the event
        SubmitButton.setOnClickListener(View.OnClickListener
        {
            //  Update: isBlank checks for null and whitespace-only strings.
            if(Typer.text.isBlank()) return@OnClickListener

            //  Prevent user from double-tapping, thus sending twice
            SubmitButton.setOnClickListener(null)

            //  Is there a conversation already?
            if(ChatID == null) createConversation()
            else postMessage()
        })
    }

    private fun getCurrentISODate() : String
    {
        //  Locale with a lambda initialization, like Javascript
        val locale = ( fun() : Locale
        {
            //  It doesn't matter if it says that it is deprecated. We are supporting from API 15, so we have to do it like this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return applicationContext.resources.configuration.locales[0]
            else return applicationContext.resources.configuration.locale
        }())

        //  The timezone
        val tz = TimeZone.getDefault()

        //  The dateformat
        val dateFormat : DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale)

        //  UPDATE: all dates will be formatted according to UTC. Later, at display, it will be adjusted with user's timezone
        //dateFormat.timeZone = TimeZone.getDefault()

        return dateFormat.format(Calendar.getInstance(tz, locale).time)
    }

    private fun createConversation()
    {
        val me : ChatMessageContainer.Partecipants.User = ChatMessageContainer.Partecipants.User(getCurrentISODate())
        val p : ChatMessageContainer.Partecipants = ChatMessageContainer.Partecipants()

        //  At this point, we are pretty sure that we are logged, that's why I use a !!
        p.addPartecipant(Me!!, me)
        p.addPartecipant(Them!!, ChatMessageContainer.Partecipants.User("0"))

        ChatID = MainReference.push().key
        MainReference.child(ChatID).setValue(p){ error, ref ->
            if(error != null)
            {
                Log.d("CHAT", "Delete conversation")
                return@setValue
            }

            //  Set up stuff
            //  NOTE: no need to synchronize this, because at this point I am the only one who modifies it
            Initialized = true
            setUps()

            postMessage()
        }
    }

    private fun postMessage()
    {
        //  Again, the !! is pretty useless because we're sure that the user is authenticated at this point
        val c = ChatMessage(Typer.text.toString(), Me!!, getCurrentISODate())

        //  First, push the id
        val p = MessagesReference.push().key

        //  Now push the actual message
        MessagesReference.child(p).setValue(c) { error, ref ->
            if(error != null)
            {
                Log.d("CHAT", "error on post message")
            }
            else
            {
                MostRecentMessaged = p
            }

            //  Error or not, user must be able to write again
            setUpButtonEvent()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if(ChatID == null) return

        val t = this

        //  Reset: Update my last time here and set that I am not writing

        TheyAreTyping.parent.child(Me!!).setValue(ChatMessageContainer.Partecipants.User(getCurrentISODate()))
        { err, ref ->
            if(err != null)
            {
                Log.d("CHAT", "could not reset")
                //return@setValue
            }

            //  Stop the counting
            synchronized(t.CountDownRunning)
            {
                if(CountDownRunning) StopTyping.cancel()
            }

            //  Detach everything
            //  NOTE: I don't actually know if this is necessary or android handles this on its own but... whatever
            Typer.setOnClickListener(null)
            SubmitButton.setOnClickListener(null)

            //  TODO: Remove events as well? I think that is not necessary
            Log.d("CHAT", "reset done")
        }
    }
}
