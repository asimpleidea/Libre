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
    var MostRecentMessage : String = ""
    var OldestMessage : String = ""
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

        //  The typer (edit text)
        Typer = findViewById(R.id.typeText)

        //  The submitter
        SubmitButton = findViewById(R.id.submitButton)

        //  Do we already have a chat stored?
        if(intent.hasExtra("chat") && intent.getStringExtra("chat") != null)
        {
            ChatID = intent.getStringExtra("chat")
            MostRecentMessage = intent.getStringExtra("start")

            //  At first they are the same of course
            OldestMessage = MostRecentMessage
        }

        //  Set up stuff
        setUps()
    }

    private fun setUps()
    {
        //  Typing observer
        setUpTypingObserver()

        if(ChatID != null)
        {
            //  Set up messages listener
            setUpMessagesListener()

            //  Set up typing listener
            setUpTypingListener()
        }
    }

    private fun setUpMessagesListener()
    {
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
        var query = MessagesReference.orderByKey()

        //  TODO: check the limitToLast
        .limitToLast(Take).endAt(OldestMessage)

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
                }

                if(p0 == null) return

                for(p : DataSnapshot in p0.children)
                {
                    Log.d("CHAT", "message: ${p.child("content").getValue(String::class.java)}")
                }

                //  Little trick to know if this is the first time we are doing this
                if(OldestMessage.compareTo(MostRecentMessage) == 0) listenForNewMessages()

                //  No need to synchronize it now
                OldestMessage = p0.children.first().key

                //  TODO: scroll on top to get previous messages
                //  General idea behind this:
                //  NOTE: this idea must be implemented because I don't know if it works or not
                if(p0.children.count() == Take)
                {
                    /*- if we got exactly Take message (example 20)
                        - then here we attach the scroll event to the view:
                            when user scrolls on top, this function (getFirstMessages) gets called again,
                            but this time, OldestMessage is a different value (we changed it two lines above)
                            Since this is a addListerForSINGLEevent this want be triggered more if user keeps scrolling.
                            So we will be called here again: if 20 messages redo attach scroll event.
                            So this is going to be a recursive scroll attacher until less than 20 messages are loaded.
                            In that case, this block won't be called again, thus making scroll just a pure scroll, not an infinite one
                            NOTE: firebase will get every Take messages until the message provided as endAt,
                            BUT: the one that you pass to endAt will be included!
                            So, if you want to get 20 messages, you have to load 21: you show only the last 20;
                            the first one, you have to *NOT* use but keep it as parameter for the next query when user scrolls.
                            I know that this general algorithm seems a bit complicated...
                            If you can find a better algorithm go for it, otherwise contact me on slack for better explanation.

                            I wrote a simulator down below, simulating a user scrolling up every 5 seconds.
                            Uncomment it to have an example of what I have written above.
                            You might want to set Take to a smaller value (like 3) to get an idea of what I was talking about.
                    */
                    /*
                    //  OldestMessage is now that element which Firebase will stop at (endAt())
                    //  It will take 20 messages in which this will be the 20th.
                    //  So, as I said above, better take 21 and keep that first one just as a reference for the next query,
                    //  and instead show the other 20 (so from 1 to 20, discard message on position 0)
                    Log.d("CHAT", "new oldest: $OldestMessage")
                    object : CountDownTimer(5*1000, 5*1000)
                        {
                            override fun onFinish()
                            {
                                Log.d("CHAT", "Going to reload again")
                                getFirstMessages()
                            }

                            override fun onTick(p0: Long) {}

                        }.start()*/
                }
            }
        })
    }

    private fun listenForNewMessages()
    {
        //  Reference to this class
        val t = this

        //  Set the event
        var query = MessagesReference.orderByChild("sent")

        .startAt(MostRecentMessage).limitToLast(1)

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
                if(p0.children.count() < 1) return

                //  Get the actual message (we are taking just one, so we're getting the first)
                val newMessage = p0.children.first()

                //  If you're here, it means that the new message was caught
                //  NOTE: a new message can arrive when we are still parsing the previous new one.
                //  In that case, we have to wait for the previous onDataChange to finish this part
                synchronized(t.MostRecentMessage)
                {
                    //  Discard the message if it is already the same as the last one (like: the one that we just posted)
                    //  NOTE: kotlin suggests doing compareTo() instead of equals(), so I did it like that
                    if(newMessage.key.compareTo(MostRecentMessage) == 0)
                    {
                        return
                    }

                    MostRecentMessage = newMessage.key
                }
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
                //  TODO: Dialog: "Sorry we could not send your message, please try again later"
                return@setValue
            }

            //  Set up stuff
            //  NOTE: no need to synchronize this block, because at this point I am the only one who modifies it
            Initialized = true
            setUps()

            postMessage()
        }
    }

    private fun postMessage()
    {
        val t = this

        //  Again, the !! is pretty useless because we're sure that the user is authenticated at this point
        val c = ChatMessage(Typer.text.toString(), Me!!, getCurrentISODate())

        //  First, push the id
        val p = MessagesReference.push().key
        var previousRecentMessage = ""

        //  This must happen in a synchronized way
        synchronized(this.MostRecentMessage)
        {
            previousRecentMessage = MostRecentMessage
            MostRecentMessage = p
        }

        //  Now push the actual message
        MessagesReference.child(p).setValue(c) { error, ref ->
            synchronized(t.MostRecentMessage)
            {
                if(error != null)
                {
                    // restore it
                    MostRecentMessage = previousRecentMessage
                    //  TODO: Dialog: "Sorry we could not send your message, please try again later"
                }
                else
                {

                }
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
        }
    }
}
