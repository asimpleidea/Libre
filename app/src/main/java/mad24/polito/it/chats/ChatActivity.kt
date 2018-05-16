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
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import mad24.polito.it.R
import mad24.polito.it.models.*
import mad24.polito.it.registrationmail.LoginActivity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity()
{
    lateinit var MainReference : DatabaseReference
    lateinit var PartnerReference : DatabaseReference

    val FireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var ChatMessagesDocument : DocumentReference
    lateinit var MessagesCollection : CollectionReference
    lateinit var IAmTyping : DocumentReference
    lateinit var PartnerIsTyping : DocumentReference
    lateinit var NewMessagesListener : ListenerRegistration

    private lateinit var RV: RecyclerView
    private lateinit var Adapter: MessagesRecyclerAdapter
    private lateinit var ViewManager: RecyclerView.LayoutManager
    private var ScrollListener : Boolean = true

    lateinit var ChatID : String
    lateinit var OldestMessage : String
    lateinit var NewestMessage : String

    val Take : Long = 20
    lateinit var Me : String
    lateinit var PartnerID : String
    private val KeysSeparator = '&'

    lateinit var User : UserMail

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
            val i = Intent(applicationContext, LoginActivity::class.java)
            startActivity(i)
            finish()
        }

        //  At this point the user is logged, so there is no point in doing !!, but kotlin wants it so...
        Me = FirebaseAuth.getInstance().currentUser!!.uid

        //------------------------------------
        //  Init
        //------------------------------------

        setContentView(R.layout.activity_chat)

        //  Initialize oldest message, so that we can properly load messages on srollToTop
        //  We want to get messages older than now, so now is the oldestMessage for now
        OldestMessage = getCurrentISODate()
        NewestMessage = getCurrentISODate()

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
        ChatToolbar.setNavigationIcon(R.drawable.ic_white_back_arrow)
        ChatToolbar.setNavigationOnClickListener(
        {
            finish()
        })

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

        loadPartnerData()

        //  The chat ID
        //  We create a chat id as a concatenation between the two users IDs, starting from the lowest one
        when(Me < PartnerID)
        {
            true -> ChatID = "$Me$KeysSeparator$PartnerID"
            false -> ChatID = "$PartnerID$KeysSeparator$Me"
        }

        //------------------------------------
        //  Finally, load the chat
        //------------------------------------

        load()
    }

    private fun loadPartnerData()
    {
        PartnerReference.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?) { }

            override fun onDataChange(p0: DataSnapshot?)
            {
                //  p0 null?? well, this is weird
                if(p0 == null) return

                //  Got user data
                Log.d("CHAT", "Got user data")

                //  Set my partner's name
                findViewById<TextView>(R.id.theirName).text = p0.child("name").value as String

                //  Put user's image
                var prefix = (fun() : String?
                {
                    if(p0.hasChild("thumbnail_exists")) return "thumb_"
                    if(p0.hasChild("has_image")) return ""
                    return null
                }())

                if(prefix == null) Glide.with(applicationContext).load(R.drawable.unknown_user).into(findViewById(R.id.theirImage))
                else
                {
                    FirebaseStorage.getInstance().getReference("profile_pictures")
                            .child("$prefix$PartnerID.jpg")
                            .downloadUrl
                            .addOnSuccessListener {url ->
                                Glide.with(applicationContext).load(url).into(findViewById(R.id.theirImage))
                            }.addOnFailureListener {
                                Glide.with(applicationContext).load(R.drawable.unknown_user).into(findViewById(R.id.theirImage))
                            }
                }
            }
        })
    }

    private fun load()
    {
        ChatMessagesDocument = FireStore.collection("chat_messages").document(ChatID)

        ChatMessagesDocument.collection("partecipants").get().addOnCompleteListener{ task ->

            //  Everything ok?
            if(!task.isSuccessful)
            {
                //  Not ok. Get back, there's no point in going further
                //  TODO: alert user that the chat is not available right now
                finish()
            }

            //  Does the chat exist?
            when(task.result.size() == 2)
            {
                true -> setUps()
                false -> createConversation()
            }
        }
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
        updateMyStatus(true, "", ChatID)
    }

    override fun onPause()
    {
        super.onPause()

        //  Update my status: I am no longer in this chat
        //  NOTE: we don't actually know where the user is right now, se we have to set offline status.
        //  If the user is getting back to the chats fragment, then the onResume there will take care of putting it online
        updateMyStatus(false, getCurrentISODate(), "0")

        //  Set my last time here
        IAmTyping.set(ChatMessageContainer.Partecipants.User(getCurrentISODate())).addOnCompleteListener { task ->
            if(!task.isSuccessful)     Log.w("CHAT", "could not update my document in onPause")
        }
    }

    // TODO: onInstanceSaved and resumed

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
        MessagesCollection = FireStore.collection("chat_messages/$ChatID/messages")

        getFirstMessages()
    }

    private fun getFirstMessages()
    {
        //  Reference to this class
        val t = this

        //  Set up the query
        MessagesCollection

            //  FINALLY A FUCKING WHERE CLAUSE!
            .whereLessThan("sent", OldestMessage)

            //  FINALLY A FUCKING ORDER BY. FUCK YEAH
            .orderBy("sent", Query.Direction.DESCENDING)

            //  How many to get?
            .limit(Take +1)

            //  Finally get
            .get()

            //  Do something with the data
            .addOnCompleteListener { task ->

                if(!task.isSuccessful)
                {
                    Log.w("CHAT", "Could not load previous chats!")
                    return@addOnCompleteListener
                }

                //  Hmm... wonder if these two are just the same thing
                if(task.result.isEmpty || task.result.size() < 1) return@addOnCompleteListener

                val messages = task.result

                //  Push the messages
                synchronized(t.Adapter)
                {
                    //  Push the messages
                    Adapter.bulkPush(messages.toObjects(ChatMessage::class.java))

                    //  Update the oldest message, so when user scrolls up we know where to start from
                    OldestMessage = messages.last().data["sent"] as String
                }

                //  If we loaded less than we ask for, then we have to stop doing the scroll to top thing
                if(messages.size() < Take) RV.clearOnScrollListeners()

                //  Scroll on top to load older messages
                else
                {
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

                                    //  Asynchronously & recursively load older messages
                                    getFirstMessages()
                                }
                            }
                        }
                    })
                }

                //  Listen for new messages
                listenForNewMessages()
            }
    }

    private fun listenForNewMessages()
    {
        //  Reference to this class
        val t = this

        //  Listen for new messages
        NewMessagesListener = MessagesCollection.whereGreaterThan("sent", NewestMessage)
                .addSnapshotListener { s, e ->

            //  Has an exception?
            if(e != null)
            {
                Log.e("CHAT", "Error while trying to listen for new messages")

                //  TODO: what to do here?
                return@addSnapshotListener
            }

            //  Has data?
            //  NOTE: the second condition is actually useless: if you're here, it means that the value exists
            if(s != null && s.size() > 0 )
            {
                synchronized(t.Adapter)
                {
                    NewestMessage = s.first().data["sent"] as String
                    NewMessagesListener.remove()

                    //  Push the messages
                    Adapter.bulkPush(s.toObjects(ChatMessage::class.java)/*.drop(Drop)*/, false)

                    //  When you're done, redo this function again, so we can update Where clause and set it to the latest message
                    //  NOTE: doing it like this, prevents us from loading messages that are sent at the very same instant.
                    //  This is a very rare occasion, but who cares... this is still a university project, right?
                    listenForNewMessages()

                    //  TODO: play sound after new partner messages is received?
                }
            }
        }
    }

    private fun setUpTypingListener()
    {
        val t = this

        //  Get the reference
        PartnerIsTyping = ChatMessagesDocument.collection("partecipants").document(PartnerID)

        //  Set up listener
        PartnerIsTyping.addSnapshotListener { s, e ->

            //  Has an exception?
            if(e != null)
            {
                Log.e("CHAT", "Error while trying to listen for partner typing")
                return@addSnapshotListener
            }

            //  Has data?
            //  NOTE: the second condition is actually useless: if you're here, it means that the value exists
            if(s != null && s.exists() )
            {
                //  Is typing?
                //  The !! already check for null
                val data = s.data!!

                if(data.containsKey("is_typing"))
                {
                    when(data["is_typing"])
                    {
                        true -> TypingNotifier.visibility = View.VISIBLE
                        false -> TypingNotifier.visibility = View.GONE

                    }
                }

                if(data.containsKey("last_here"))
                {
                    synchronized(t.Adapter)
                    {
                        Adapter.setLastHere(data["last_here"].toString())
                    }
                }
            }
        }
    }

    private fun setUpTypingObserver()
    {
        val t = this

        //  Set the button
        setUpButtonEvent()

        //  Set the typing reference
        IAmTyping = ChatMessagesDocument.collection("partecipants").document(Me)

        //  Set the countdown timer
        StopTyping = object : CountDownTimer(Seconds, Seconds)
        {
            override fun onFinish()
            {
                synchronized(t.CountDownRunning)
                {
                    IAmTyping.update("is_typing", false).addOnCompleteListener { task ->
                        if(!task.isSuccessful) Log.w("CHAT", "could not reset the typing observer")
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
                        IAmTyping.update("is_typing", true).addOnCompleteListener { task ->

                            if(task.isSuccessful) StopTyping.start()
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
        //  In case we want to provide milliseconds, for better precision message posting (very useful in listenForNewMessages)
        //val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
        val dateFormat : DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale)

        //  UPDATE: all dates will be formatted according to UTC. Later, at display, it will be adjusted with user's timezone
        //dateFormat.timeZone = TimeZone.getDefault()

        return dateFormat.format(Calendar.getInstance(locale).time)
    }

    private fun createConversation()
    {
        val me : ChatMessageContainer.Partecipants.User = ChatMessageContainer.Partecipants.User(getCurrentISODate())
        val u = ChatMessageContainer.Partecipants.User("0")

        FireStore.collection("chat_messages/$ChatID/partecipants").document(Me).set(me)
                .addOnCompleteListener {  task ->
                    if(!task.isSuccessful)
                    {
                        //  TODO: alert user that chat is not available right now
                        finish()
                    }

                    FireStore.collection("chat_messages/$ChatID/partecipants").document(PartnerID).set(u)
                            .addOnCompleteListener {
                                if(!task.isSuccessful)
                                {
                                    //  TODO: alert user that chat is not available right now
                                    finish()
                                }

                                //  The chat has been created! Let's update this status
                                updateMyStatus(true, "", ChatID)

                                //  Set up stuff
                                setUps()
                            }
                }
    }

    private fun postMessage()
    {
        val time = getCurrentISODate()
        val c = ChatMessage(Typer.text.toString(), Me, time)

        MessagesCollection.add(c).addOnCompleteListener { task ->

            if(!task.isSuccessful)
            {
                Log.d("CHAT", "message not deployed")
                return@addOnCompleteListener
            }

            //  UPDATE: No need for this, listen for new messages will take care of this
            /*synchronized(t.Adapter)
            {
                Adapter.push(c)
            }*/

            //  Reset the editext
            Typer.text.clear()

            //  Error or not, user must be able to write again
            setUpButtonEvent()

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
    }

    override fun onDestroy()
    {
        super.onDestroy()

        val t = this

        IAmTyping.update("is_typing", false).addOnCompleteListener { task ->
            if(!task.isSuccessful) Log.w("CHAT", "could not reset the typing observer in onDestroy")

            //  Stop the counting
            synchronized(t.CountDownRunning)
            {
                if(CountDownRunning) StopTyping.cancel()
            }
        }

        //  Detach everything
        //  NOTE: I don't actually know if this is necessary or android handles this on its own but... whatever
        Typer.setOnClickListener(null)
        SubmitButton.setOnClickListener(null)
        NewMessagesListener.remove()

        //  TODO: Remove events as well? I think that is not necessary
    }
}