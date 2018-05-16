package mad24.polito.it.chats

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import mad24.polito.it.R
import mad24.polito.it.models.ChatMessage
import mad24.polito.it.models.ChatMessageContainer
import mad24.polito.it.models.UserMail
import mad24.polito.it.models.UserStatus
import mad24.polito.it.registrationmail.LoginActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity()
{
    lateinit var MainReference : DatabaseReference
    lateinit var MessagesReference : DatabaseReference
    lateinit var PartnerIsTyping : DatabaseReference
    lateinit var IAmTyping : DatabaseReference
    lateinit var PartnerReference : DatabaseReference
    lateinit var ChatCreationListener : ValueEventListener

    private val KeysSeparator = '&'

    private lateinit var RV: RecyclerView
    private lateinit var Adapter: MessagesRecyclerAdapter
    private lateinit var ViewManager: RecyclerView.LayoutManager
    private var ScrollListener : Boolean = true

    //var ChatID : String? = null
    lateinit var ChatID : String
    val Take : Int = 5
    val Me : String? = FirebaseAuth.getInstance().currentUser?.uid
    var PartnerID : String? = null
    var NewestMessage : String = ""
    var MostRecentTime : String = ""
    var OldestMessage : String = ""
    var Initialized : Boolean = false
    var User : UserMail? = null

    //  TODO: Change this type: it won't be a textview on production, of course
    lateinit var TypingNotifier : TextView
    lateinit var Typer : EditText
    lateinit var SubmitButton : Button
    lateinit var ChatToolbar : Toolbar
    lateinit var PartnerStatus : TextView

    lateinit var StopTyping : CountDownTimer
    var CountDownRunning : Boolean = false
    val Seconds : Long = 3 *1000

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        //------------------------------------
        //  Is user logged?
        //------------------------------------

        if(FirebaseAuth.getInstance().currentUser == null)
        {
            val i : Intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(i)
            finish()
        }

        //------------------------------------
        //  Init
        //------------------------------------

        setContentView(R.layout.activity_chat)

        //------------------------------------
        //  Get data about the other user
        //------------------------------------

        //  Do I have any information about my partner?
        if(!intent.hasExtra("partner_id") || (intent.hasExtra("partner_id") && intent.getStringExtra("partner_id").isBlank()))
        {
            //  We finish, because what's the point of having chat if I have no data about my partner?
            //  TODO: show a dialog: "sorry there was a problem loading this chat...". For now, we just finish
            finish()
        }

        PartnerID = intent.getStringExtra("partner_id")

        //------------------------------------
        //  Views
        //------------------------------------

        //  Set my partner's name
        findViewById<TextView>(R.id.theirName).text = intent.getStringExtra("partner_name")

        //  The online status
        PartnerStatus = findViewById(R.id.theirStatus)

        //  Set typing text
        //  TODO: use String.format(id, User?.name) instead of this hardcoded string
        TypingNotifier = findViewById(R.id.userIsTyping)
        TypingNotifier.text = "${intent.getStringExtra("partner_name")} is typing... (THREE DOTS ANIMATION HERE?"

        //  The typer (edit text)
        Typer = findViewById(R.id.typeText)

        //  The submitter
        SubmitButton = findViewById(R.id.submitButton)

        //  Put a back button
        ChatToolbar = findViewById(R.id.chatToolbar)
        ChatToolbar.title = getString(R.string.app_name)
        ChatToolbar.setNavigationIcon(R.drawable.ic_white_back_arrow)
        ChatToolbar.setNavigationOnClickListener(
        {
            finish()
        })

        //  Put user's image
        //  TODO: CHANGE THIS WITH NEW METHOD
        FirebaseStorage.getInstance().getReference("profile_pictures")
                .child("$PartnerID.jpg")
                .downloadUrl
                .addOnSuccessListener {url ->
                    Glide.with(applicationContext).load(url).into(findViewById(R.id.theirImage))
                }.addOnFailureListener {
                    Glide.with(applicationContext).load(R.drawable.unknown_user).into(findViewById(R.id.theirImage))
                }

        //------------------------------------
        //  The recycler adapter
        //------------------------------------

        ViewManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, true)

        //  This is just temporary, it will be updated later, in statusListener
        Adapter = MessagesRecyclerAdapter("0")

        RV = findViewById<RecyclerView>(R.id.messagesContainer).apply {
            layoutManager = ViewManager
            adapter = Adapter
            setHasFixedSize(true)
        }

        //------------------------------------
        //  Set up references
        //------------------------------------

        //  Initialize the main DB reference
        MainReference = FirebaseDatabase.getInstance().getReference("chat_messages")

        //  Put the receiver Reference
        PartnerReference = MainReference.parent.child("users").child(PartnerID)

        //  The chat ID
        //  We create a chat id as a concatenation between the two users IDs, starting from the lowest one
        when(Me!! < PartnerID!!)
        {
            true -> ChatID = "$Me$KeysSeparator$PartnerID"
            false -> ChatID = "$PartnerID$KeysSeparator$Me"
        }

        //------------------------------------
        //  Finally, load the chat
        //------------------------------------

        load()
    }

    private fun load()
    {
        MainReference.child(ChatID).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?) { }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 != null)
                {
                    when(p0.exists())
                    {
                        true -> setUps()
                        else -> createConversation()
                    }
                }
                else createConversation()
            }
        })
    }

    private fun updateMyStatus(status: Boolean = true, last : String = "", inChat : String = "home")
    {
        //  FINALLY WE CAN SET DEFAULT PARAMETER VALUES IN KOTLIN!!!

        //  The class name
        val className = (fun() : String
        {
           when(status)
           {
               true -> return this.localClassName
               false -> return ""
           }
        }())

        //  Set my status as online and here
        val u = UserStatus(status, last, className, inChat)
        MainReference.parent.child("users").child(Me).child("status").setValue(u){ err, _ ->
            if(err != null)
            {
                Log.d("CHAT", "error while trying to set me online")
                return@setValue
            }
        }
    }

    private fun setUps()
    {
        //  Set up messages listener
        setUpMessagesListener()

        //  Typing observer
        setUpTypingObserver()

        //  Status listener
        setUpStatusListener()

        //  Set up typing listener
        setUpTypingListener()
    }

    override fun onResume()
    {
        super.onResume()

        //  Update my status: set me online here.
        //  when(stuff) ecc is very good to do... but a ternary operator would be better...

        val onChat = ( fun() : String
        {
            when(::ChatID.isInitialized)
            {
                true -> return ChatID
                false ->  return ""
            }
        }())

        updateMyStatus(true, "", onChat)
    }

    override fun onPause()
    {
        super.onPause()

        //  Update my status: I am no longer in this chat
        //  NOTE: we don't actually know where the user is right now, se we have to set offline status.
        //  If the user is getting back to the chats fragment, then the onResume there will take care of putting it online
        updateMyStatus(false, getCurrentISODate(), "0")

        //  Set my last time here
        if(::ChatID.isInitialized) MainReference
                                        .child(ChatID)
                                        .child("partecipants")
                                        .child(Me)
                                        .setValue(ChatMessageContainer.Partecipants.User(getCurrentISODate())){
                                            err, _ ->

                                            if(err != null)
                                            {
                                                Log.d("CHAT", "could not update my last here")
                                                return@setValue
                                            }
                                        }

    }

    override fun onBackPressed()
    {
        super.onBackPressed()

        //  TODO: General Idea:
        /*
            If back button is pressed: check if the previous activity is a chat related activity (check the intent).
            If yes -> keep my status as online.
            If no -> set my status as offline
         */
        //  UPDATE: this is probably useless.
    }

    private fun setUpStatusListener()
    {
        val t = this

        PartnerReference.child("status").addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?){ }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 == null) return

                val u = p0.getValue(UserStatus::class.java)

                synchronized(t.Adapter)
                {
                    if(u!!.isOnline)
                    {
                        PartnerStatus.text = resources.getString(R.string.chat_status_online)

                        //  Is the user here?
                        when(u.in_chat.compareTo(ChatID) == 0)
                        {
                            true -> Adapter.here()
                            false -> Adapter.notHere()
                        }
                    }
                    else
                    {
                        Adapter.notHere()
                        PartnerStatus.text = String.format(resources.getString(R.string.chat_last_seen, u.last_online))
                    }
                }
            }

        })
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
        var query = MessagesReference.orderByChild("sent")

        //  Read below why I do Take +1
        .limitToLast(Take +1)

        if(!OldestMessage.isBlank()) query.endAt(OldestMessage)

        /*synchronized(this.OldestMessage)
        {
            //  Basically, the very first time that you call this function, OldestMessage is still blank.
            //  All the other times (this function is called recursively-asynchronously when scrolling up),
            //  it is not blank. So, we're basically asking if this is the first time we're running this function
            if(!OldestMessage.isBlank())
            {
                Log.d("CHAT", "oldest message is not blank: $OldestMessage and newest is $NewestMessage")
                query.endAt(OldestMessage)
            }
            else Log.d("CHAT", "oldest message is blank")
        }*/

        query.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?) { }

            override fun onDataChange(p0: DataSnapshot?)
            {
                synchronized(t.Initialized)
                {
                    //  Ok, this means: we have loaded the first messages.
                    //  So, next time don't load this messages again
                    Initialized = true
                }

                if(p0 == null || !p0.hasChildren()) return

                synchronized(t.Adapter)
                {
                    //  Is there only one message?
                    if(p0.childrenCount > 1) Adapter.bulkPush(p0.children.drop(1))

                    //  No messages loaded? Then stop doing the scroll to top thing
                    if(p0.childrenCount < Take)
                    {
                        RV.clearOnScrollListeners()

                        //  Push the first message (it is hidden because we use it as offset on the endAt)...
                        //  well... this is the way firebase wants it so...
                        Adapter.push(p0.children.first(), true)
                    }
                }

                //  Update oldest Message.
                //  NOTE: why do we need two locks (NewestMessage and OldestMessage)?
                //  Because the first is used to listen for new message async and the other for scroll to top async
                synchronized(t.OldestMessage)
                {
                    //  Little trick to know if this is the first time we are doing this
                    //if(OldestMessage.compareTo(NewestMessage) == 0)
                    if(OldestMessage.isBlank())
                    {
                        Log.d("CHAT", "line 419")
                        ViewManager.scrollToPosition(0)
                        RV.smoothScrollToPosition(0)

                        //  Update the newest message, so that we won't take this twice in listenForNewMessages
                        //  No need to synchronize the newest message because this is the first time, so no one is accessing this
                        NewestMessage = p0.children.last().key
                        MostRecentTime = p0.children.last().child("sent").value as String

                        //listenForNewMessages()
                    }

                    OldestMessage = p0.children.first().key
                }

                //  Did we get as many elements as we wanted?
                if(p0.childrenCount >= Take)
                {
                    //  Scroll to top to load previous messages
                    RV.addOnScrollListener(object : RecyclerView.OnScrollListener()
                    {
                        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int)
                        {
                            super.onScrolled(recyclerView, dx, dy)

                            synchronized(t.ScrollListener)
                            {
                                if (ViewManager.isViewPartiallyVisible(RV.getChildAt(RV.childCount - 1), true, true))
                                {
                                    //  Stop listening for now
                                    RV.clearOnScrollListeners()
                                    getFirstMessages()
                                }
                            }
                        }
                    })
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

        .startAt(NewestMessage).limitToLast(1)

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
                synchronized(t.NewestMessage)
                {
                    //  Discard the message if it is already the same as the last one (like: the one that we just posted)
                    //  NOTE: kotlin suggests doing compareTo() instead of equals(), so I did it like that
                    if(newMessage.key.compareTo(NewestMessage) == 0 || newMessage.child("sent").value.toString() <= MostRecentTime )
                    {
                        return
                    }

                    NewestMessage = newMessage.key
                    MostRecentTime = newMessage.child("sent").value.toString()
                }

                synchronized(t.Adapter)
                {
                    Adapter.push(newMessage)

                    //  TODO: play sound after new messages is received?
                }
            }
        })
    }

    private fun setUpTypingListener()
    {
        val t = this

        //  Get the reference
        PartnerIsTyping = MainReference.child(ChatID).child("partecipants").child(PartnerID)

        //  Set up listener
        PartnerIsTyping.addValueEventListener(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?){ }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 == null) return

                if(p0.hasChild("is_typing"))
                {
                    //  TODO: add fade in & fade out animations here
                    if(p0.child("is_typing").getValue(Boolean::class.java) == true) TypingNotifier.visibility = View.VISIBLE
                    else TypingNotifier.visibility = View.GONE
                }

                if(p0.hasChild("last_here"))
                {
                    synchronized(t.Adapter)
                    {
                        Adapter.setLastHere(p0.child("last_here").value.toString())
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

        //  Set the typing reference
        IAmTyping = MainReference.child(ChatID).child("partecipants").child(Me)

        //  Set the countdown timer
        StopTyping = object : CountDownTimer(Seconds, Seconds)
        {
            override fun onFinish()
            {
                synchronized(t.CountDownRunning)
                {
                    IAmTyping.child("is_typing").setValue(false) { error, _ ->
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

        Typer.setOnKeyListener(View.OnKeyListener { _, _, keyEvent ->
            if(keyEvent.action == KeyEvent.ACTION_DOWN)
            {
                synchronized(t.CountDownRunning)
                {
                    if(!CountDownRunning)
                    {
                        IAmTyping.child("is_typing").setValue(true) { p0, _ ->
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

            postMessage()
        })
    }

    private fun listenForChatCreation()
    {
        ChatCreationListener = MainReference.parent.child("chats")
                .child(Me)
                .child(PartnerID)
                .addValueEventListener(object : ValueEventListener
                {
                    override fun onCancelled(p0: DatabaseError?)
                    {
                    }

                    override fun onDataChange(p0: DataSnapshot?)
                    {
                        if(p0 == null) return

                        if(p0.hasChild("chat") && p0.child("chat").value != null)
                        {
                            //  These checks are getting obvious... but kotlin wants them for non-null thing
                            if(p0.hasChild("last_message"))
                            {
                                //  Was this just created by my partner?
                                if (p0.child("last_message").child("by").getValue(String::class.java).equals(PartnerID))
                                {
                                    //  If so then update the chatID and start setting up stuff
                                    ChatID = p0.child("chat").value as String
                                    updateMyStatus(true, "", ChatID)
                                    Initialized = true
                                    setUps()
                                }

                                //  Finally, stop listening for changes
                                MainReference.parent.child("chats")
                                        .child(Me)
                                        .child(PartnerID).removeEventListener(ChatCreationListener)
                            }
                        }
                    }
                })
    }

    private fun getCurrentISODate() : String
    {
        //  Locale with a lambda initialization, like Javascript
        val locale = ( fun() : Locale
        {
            //  It doesn't matter if it says that it is deprecated. We are supporting from API 15, so we have to do it like this
            //  Testing it with a when expression
            when(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                true -> return applicationContext.resources.configuration.locales[0]
                false -> return applicationContext.resources.configuration.locale
            }
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return applicationContext.resources.configuration.locales[0]
            else return applicationContext.resources.configuration.locale*/
        }())

        //  The timezone
        //val tz = TimeZone.getDefault()

        //  The dateformat
        val dateFormat : DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale)

        //  UPDATE: all dates will be formatted according to UTC. Later, at display, it will be adjusted with user's timezone
        //dateFormat.timeZone = TimeZone.getDefault()

        return dateFormat.format(Calendar.getInstance(locale).time)
    }

    private fun createConversation()
    {
        val me : ChatMessageContainer.Partecipants.User = ChatMessageContainer.Partecipants.User(getCurrentISODate())
        val p : ChatMessageContainer.Partecipants = ChatMessageContainer.Partecipants()

        //  At this point, we are pretty sure that we are logged, that's why I use a !!
        p.addPartecipant(Me!!, me)
        p.addPartecipant(PartnerID!!, ChatMessageContainer.Partecipants.User("0"))

        MainReference.child(ChatID).setValue(p){ error, _ ->
            if(error != null)
            {
                //  TODO: dialog: could not create conversation
                return@setValue
            }

            //  Set up stuff
            //  NOTE: no need to synchronize this block, because at this point I am the only one who modifies it
            Initialized = true

            //  The chat has been created! Let's update this status
            updateMyStatus(true, "", ChatID)
            setUps()
        }
    }

    private fun postMessage()
    {
        val t = this

        //  Again, the !! is pretty useless because we're sure that the user is authenticated at this point
        val time = getCurrentISODate()
        val c = ChatMessage(Typer.text.toString(), Me!!, time)

        //  First, push the id
        val p = MessagesReference.push().key
        var previousRecentMessage = ""

        //  This must happen in a synchronized way
        synchronized(this.NewestMessage)
        {
            previousRecentMessage = NewestMessage
            NewestMessage = p
        }

        //  Now push the actual message
        MessagesReference.child(p).setValue(c) { error, _ ->
            synchronized(t.NewestMessage)
            {
                if(error != null)
                {
                    // restore it
                    NewestMessage = previousRecentMessage
                    //  TODO: Dialog: "Sorry we could not send your message, please try again later"
                }

                MostRecentTime = time

                //  Listen for received updates
                //  This listens for updates from Firebase Functions: if the element "received" was changed,
                //  then we know that a notification to the partner has been sent and the message received.
                //  It's the equivalent of the second grey tick in whatsapp
                //  UPDATE: I decided to not implement this yet... this is just a book chat...
                //          let's not make this too complicated, alright?
                /*MessagesReference.child(p).addValueEventListener(object : ValueEventListener
                {
                    override fun onCancelled(p0: DatabaseError?)
                    {

                    }

                    override fun onDataChange(p0: DataSnapshot?)
                    {
                        //  I got a data, stop listening for changes!
                        //  NOTE: this does *not* point to the class, but to ValueEventListener!
                        MessagesReference.child(p).removeEventListener(this)

                        if(p0 != null)
                        {
                            if(p0.hasChild("received") && p0.child("received").getValue(String::class.java)!!.length > 1)
                            {
                                //  Set my new message as "Received"
                                Adapter.setAsReceived(p)
                            }
                        }
                    }
                })*/
            }

            synchronized(t.Adapter)
            {
                Adapter.push(c)
            }

            //  Reset the editext
            Typer.text.clear()

            //  Error or not, user must be able to write again
            setUpButtonEvent()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if(::ChatID.isInitialized) return

        val t = this

        //  Reset: Update my last time here and set that I am not writing
        PartnerIsTyping.parent.child(Me!!).setValue(ChatMessageContainer.Partecipants.User(getCurrentISODate()))
        { err, _ ->
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
