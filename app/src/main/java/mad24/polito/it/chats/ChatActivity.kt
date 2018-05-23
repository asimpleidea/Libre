package mad24.polito.it.chats

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.os.CountDownTimer
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.github.curioustechizen.ago.RelativeTimeTextView
import kotlinx.android.synthetic.main.activity_chat.*


class ChatActivity : AppCompatActivity()
{
    private lateinit var MainReference : DatabaseReference
    private lateinit var PartnerReference : DatabaseReference

    private val FireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var ChatMessagesDocument : DocumentReference
    private lateinit var MessagesCollection : CollectionReference
    private lateinit var IAmTyping : DocumentReference
    private lateinit var PartnerIsTyping : DocumentReference
    private lateinit var NewMessagesListener : ListenerRegistration

    private lateinit var RV: RecyclerView
    private lateinit var Adapter: MessagesRecyclerAdapter
    private lateinit var ViewManager: RecyclerView.LayoutManager
    private var ScrollListener : Boolean = true

    private lateinit var ChatID : String
    private lateinit var OldestMessage : String
    private lateinit var NewestMessage : String
    private lateinit var BookID : String
    private lateinit var Topic : Book

    private val Take : Long = 10
    private lateinit var Me : String
    private lateinit var PartnerID : String
    private val KeysSeparator = '&'
    private val BookSeparator = ':'

    private lateinit var User : UserMail

    //  TODO: Change this type: it won't be a textview on production, of course
    private lateinit var TypingNotifier : TextView
    private lateinit var Typer : AppCompatEditText
    private lateinit var SubmitButton : ImageButton
    private lateinit var ChatToolbar : Toolbar
    private lateinit var BorrowButton : ImageButton
    private lateinit var PartnerStatus : RelativeTimeTextView //TextView
    private lateinit var BookInfo : LinearLayout
    private lateinit var ShowInfo : ImageButton

    lateinit var StopTyping : CountDownTimer
    private var CountDownRunning : Boolean = false
    private val Seconds : Long = 3 *1000
    private var StuffLoaded : Int = 0
    private var StuffToLoad : Int = 4

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
            return
        }

        //  Do I have any information about the book we're going to chat about?
        if(!intent.hasExtra("book_id") || (intent.hasExtra("book_id") && intent.getStringExtra("book_id").isBlank()))
        {
            //  As above
            finish()
            return
        }

        PartnerID = intent.getStringExtra("partner_id")
        BookID = intent.getStringExtra("book_id")

        //------------------------------------
        //  Views
        //------------------------------------

        //  The online status
        PartnerStatus = findViewById(R.id.theirStatus)

        //  Set typing text
        TypingNotifier = findViewById(R.id.userIsTyping)

        //  The typer (edit text)
        Typer = findViewById(R.id.typeText)

        //  The submitter
        SubmitButton = findViewById(R.id.submitButton)

        //  Borrow Button
        BorrowButton = findViewById(R.id.borrowButton)

        //  The about "the book"
        BookInfo = findViewById(R.id.aboutTheBook)

        //  The show info button
        ShowInfo = findViewById(R.id.showInfo)
        ShowInfo.setOnClickListener { _ ->
            ShowInfo.setOnClickListener(null)
            animateBookInfo(BookInfo.visibility == View.VISIBLE)
        }

        //  Put a back button
        ChatToolbar = findViewById(R.id.chatToolbar)
        ChatToolbar.setNavigationIcon(R.drawable.ic_white_back_arrow)
        ChatToolbar.setNavigationOnClickListener(
        {
            finish()
        })

        //  Put the book cover
        FirebaseStorage.getInstance().getReference("bookCovers")
                .child("thumb_$BookID.jpg")
                .downloadUrl
                .addOnSuccessListener {url ->
                    Glide.with(applicationContext).load(url).into(BookInfo.findViewById(R.id.aboutBookImage))
                }.addOnFailureListener {
                    Glide.with(applicationContext).load(R.drawable.generic_book).into(BookInfo.findViewById(R.id.aboutBookImage))
                }

        //------------------------------------
        //  The recycler adapter
        //------------------------------------

        ViewManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, true)

        //  This is just temporary, it will be updated later, in statusListener
        Adapter = MessagesRecyclerAdapter(applicationContext, "0")

        RV = findViewById<RecyclerView>(R.id.messagesContainer).apply {
            layoutManager = ViewManager
            adapter = Adapter
            setHasFixedSize(true)
        }

        //------------------------------------
        //  Set up references
        //------------------------------------

        //  Initialize the main DB reference
        //  TODO: this is useless now, but I don't want to rewrite everything again...
        MainReference = FirebaseDatabase.getInstance().getReference("chat_messages")

        //  Put the receiver Reference
        PartnerReference = MainReference.parent.child("users").child(PartnerID)

        loadPartnerData()

        loadBookData()

        //  The chat ID
        //  We create a chat id as a concatenation between the two users IDs, starting from the lowest one
        when(Me < PartnerID)
        {
            true -> ChatID = "$BookID$BookSeparator$Me$KeysSeparator$PartnerID"
            false -> ChatID = "$BookID$BookSeparator$PartnerID$KeysSeparator$Me"
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
                val name = p0.child("name").value as String
                TypingNotifier.text = String.format(getString(R.string.partner_is_typing), name)

                //  Set my partner's name
                findViewById<TextView>(R.id.theirName).text = name

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

                BookInfo.findViewById<TextView>(R.id.youAndPartner).text = String.format(getString(R.string.you_and_x_are_talking), name)
                progressiveLoad("loadPartnerData")
            }
        })
    }

    private fun loadBookData()
    {
        MainReference.parent.child("books").child(BookID).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?) { }

            override fun onDataChange(p0: DataSnapshot?)
            {
                //  p0 null?? well, this is weird
                if(p0 == null) return

                //  These are weird as well, but it is pretty impossible at this point
                if(!p0.exists()) return
                if(p0.value == null) return

                //  Get the book
                val t = p0.getValue(Book::class.java)
                if(t == null) return

                Topic = t
                if(Topic.user_id == Me) BorrowButton.visibility = View.VISIBLE
                else
                {
                    //  Else remove the button at all
                    val b = BorrowButton.parent as RelativeLayout
                    (BorrowButton.parent.parent as ViewGroup).removeView(b)
                }

                BookInfo.findViewById<TextView>(R.id.infoBookTitle).text = Topic.title

                progressiveLoad("loadBookData")
            }
        })
    }

    private fun show()
    {
        //  Show the chat
        (findViewById<ProgressBar>(R.id.loadingScreen).parent as RelativeLayout).visibility = View.GONE
        ChatToolbar.visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.messagesContainer).visibility = View.VISIBLE
        findViewById<BottomNavigationView>(R.id.navigation).visibility = View.VISIBLE
            }

    private fun animateBookInfo(hide : Boolean = false)
    {
        //---------------------------------------
        //  Init
        //---------------------------------------

        val start = if(!hide) -70 else 0
        val end = if(!hide) 0 else -70

        //  Set up the animation
        val anim = ValueAnimator.ofInt(start, end)
        anim.duration = 200
        anim.addUpdateListener { a ->
            val lp = BookInfo.layoutParams as FrameLayout.LayoutParams
            lp.topMargin =  a.animatedValue as Int
            BookInfo.layoutParams = lp
        }

        //  The listener
        anim.addListener(object : Animator.AnimatorListener
        {
            override fun onAnimationRepeat(p0: Animator?) { }

            override fun onAnimationEnd(p0: Animator?)
            {
                Log.d("CHAT", "animation ended")
                //  Showing or hiding?
                when(hide)
                {
                    //  Showing
                    false ->
                    {

                    }

                    //  Hiding
                    true ->
                    {
                        ShowInfo.setOnClickListener { _ ->
                            ShowInfo.setOnClickListener(null)
                            animateBookInfo(BookInfo.visibility == View.VISIBLE)
                        }

                        BookInfo.visibility = View.GONE
                    }
                }
            }

            override fun onAnimationCancel(p0: Animator?) { }

            override fun onAnimationStart(p0: Animator?)
            {
                Log.d("CHAT", "animation started")
            }

        })

        //  Before starting the animation...
        if(!hide) BookInfo.visibility = View.VISIBLE
        anim.start()
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
                if(p0.childrenCount < 1) return

                val u = p0.getValue(UserStatus::class.java)

                synchronized(t.Adapter)
                {
                    if(u!!.isOnline)
                    {
                        findViewById<TextView>(R.id.status_teller).text = getString(R.string.chat_status_online)
                        PartnerStatus.visibility = View.INVISIBLE

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
                        findViewById<TextView>(R.id.status_teller).text = getString(R.string.last_online)
                        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        PartnerStatus.setReferenceTime(df.parse(u.last_online).time)
                        PartnerStatus.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun setUpMessagesListener()
    {
        //  Set it up
        MessagesCollection = FireStore.collection("chat_messages/$ChatID/messages")

        getFirstMessages(true)
    }

    private fun getFirstMessages(firstLoad : Boolean = false)
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
            .limit(Take)

            //  Finally get
            .get()

            //  Do something with the data
            .addOnCompleteListener { task ->
                if(!task.isSuccessful)
                {
                    Log.w("CHAT", "Could not load previous chats!")
                    return@addOnCompleteListener
                }

                //  Get the messages
                val messages = task.result

                //  Hmm... wonder if these two are just the same thing
                if(!messages.isEmpty && messages.size() > 0)
                {
                    //  Push the messages
                    synchronized(t.Adapter)
                    {
                        //  Push the messages
                        Adapter.bulkPush(messages.toObjects(ChatMessage::class.java))

                        //  Update the oldest message, so when user scrolls up we know where to start from
                        OldestMessage = messages.last().data["sent"] as String


                    }
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

                if(firstLoad)
                {
                    progressiveLoad("getFirstMessages")

                    //  Listen for new messages
                    listenForNewMessages()
                }
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
                    //NewestMessage = s.first().data["sent"] as String

                    //  Push the messages
                    //  UPDATE: I need to do it like this, otherwise i would get all previous new messages
                    for (d in s.documentChanges)
                    {
                        if(d.type == DocumentChange.Type.ADDED) Adapter.push(d.document.toObject(ChatMessage::class.java))
                    }

                    //  If we're at the bottom, keep going bottom
                    if (Adapter.itemCount > 1 && ViewManager.isViewPartiallyVisible(RV.getChildAt(0), true, false))
                    {
                        RV.smoothScrollToPosition(0)
                    }

                    //Adapter.bulkPush(s.toObjects(ChatMessage::class.java)/*.drop(Drop)*/, false)

                    //  When you're done, redo this function again, so we can update Where clause and set it to the latest message
                    //  NOTE: doing it like this, prevents us from loading messages that are sent at the very same instant.
                    //  This is a very rare occasion, but who cares... this is still a university project, right?
                    //  UPDATE: THIS DOES NOT WORK IMMEDIATELY, MAKING ALL THE ABOVE POINTLESS. IT'S PROBABLY AN ISSUE FROM FIREBASE.
                    //listenForNewMessages()

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
                //  Prevent default
                if(Typer.text.trim(' ', '\n').isBlank()) return@OnKeyListener false

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

            if(keyEvent.action == KeyEvent.ACTION_UP)
            {
                if(Typer.text.trim(' ', '\n').isBlank())
                {
                    SubmitButton.setImageResource(R.drawable.ic_icon_send_disabled)
                    return@OnKeyListener false
                }

                SubmitButton.setImageResource(R.drawable.ic_icon_send)
            }

            return@OnKeyListener false
        })

        progressiveLoad("typingObserver")
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
        val locale = Locale.getDefault()

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

                                progressiveLoad()

                                //  Set up stuff
                                setUps()
                            }
                }
    }

    private fun postMessage()
    {
        val time = getCurrentISODate()
        val c = ChatMessage(Typer.text.toString().trim(' ', '\n'), Me, time)

        MessagesCollection.add(c).addOnCompleteListener { task ->

            if(!task.isSuccessful)
            {
                Log.d("CHAT", "message not deployed")
                return@addOnCompleteListener
            }

            //  TODO: is this really helpful?
            RV.smoothScrollToPosition(0)

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
        if(::NewMessagesListener.isInitialized) NewMessagesListener.remove()

        //  TODO: Remove events as well? I think that is not necessary
    }

    private fun progressiveLoad(from : String = "")
    {
        synchronized(StuffLoaded)
        {
            ++StuffLoaded
            if(!from.isBlank()) Log.d("CHAT", "loaded from $from: $StuffLoaded")
            if(StuffLoaded == StuffToLoad) show()
        }
    }
}
