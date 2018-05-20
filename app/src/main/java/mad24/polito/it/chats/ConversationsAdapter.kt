package mad24.polito.it.chats

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.github.curioustechizen.ago.RelativeTimeTextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import mad24.polito.it.R
import mad24.polito.it.models.Chat
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter constructor(_context : Context, _me : String): RecyclerView.Adapter<ConversationsAdapter.ViewHolder>()
{
    private val Conversations = ArrayList<Chat>()
    private lateinit var RootView : View
    private lateinit var Holder : ViewHolder
    private val context = _context
    private var UsersToLoad : Int = 0
    private val UserReference = FirebaseDatabase.getInstance().reference.child("users")
    private val Me = _me
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    private val UserNames = HashMap<String, String>()
    private val UserPictures = HashMap<String, Uri>()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder
    {
        RootView = LayoutInflater.from(parent!!.context).inflate(R.layout.conversation, parent, false)

        Holder = ConversationsAdapter.ViewHolder(RootView)

        return Holder
    }

    override fun getItemCount(): Int
    {
        return Conversations.size
    }

    fun bulkPush(chats : List<Chat>)
    {
        Conversations.addAll(chats)

        if(UsersToLoad == 0) UsersToLoad = Conversations.size

        notifyDataSetChanged()
    }

    fun push(chat : Chat)
    {
        Conversations.add(chat)
        notifyItemInserted(Conversations.size - 1)
    }

    fun swap(oldIndex : Int, newIndex : Int, change : Chat )
    {
        if(oldIndex != newIndex)
        {
            Collections.swap(Conversations, oldIndex, newIndex)
            Conversations[newIndex].preview = change.preview
            Conversations[newIndex].last_message_by = change.last_message_by
            Conversations[newIndex].last_message_id = change.last_message_id
            Conversations[newIndex].last_message_time = change.last_message_time

            //  useless
            Conversations[newIndex].my_last_here = change.my_last_here

            notifyItemMoved(oldIndex, newIndex)
        }

        notifyItemChanged(newIndex)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        //  We need the second signature
        onBindViewHolder(holder, position, null)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int, payloads: MutableList<Any>?)
    {
        if(holder == null) return

        holder.Preview.text = Conversations[position].preview
        if(Conversations[position].last_message_by == Me) holder.Preview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_icon_forward, 0, 0, 0)

        if(payloads == null || payloads.isEmpty())
        {
            //  Load the user
            loadUser(Conversations[position].partner_id, holder)

            //  Load the picture
            loadPicture(Conversations[position].partner_id, holder)
        }

        //  Set the time
        holder.MessageTime.setReferenceTime(df.parse(Conversations[position].last_message_time).time)

        if(!Conversations[position].my_last_here.isBlank() && Conversations[position].my_last_here < Conversations[position].last_message_time)
        {
            holder.Preview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unread, 0, 0, 0)
        }

        //  Set the touch event
        if(holder.itemView.hasOnClickListeners()) return
        holder.itemView.setOnClickListener {_ ->
            //  Init the intent
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("partner_id", Conversations[position].partner_id)

            //  Start the activity
            context.startActivity(intent)
        }
    }

    private fun loadUser(id : String, holder : ViewHolder)
    {
        UserReference.child(id).addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError?) { }

            override fun onDataChange(p0: DataSnapshot?)
            {
                if(p0 == null) return
                if(p0.exists())
                {
                    val name = p0.child("name").value as String
                    holder.PartnerName.text = name
                }
            }
        })
    }

    private fun loadPicture(id : String, holder : ViewHolder)
    {
        FirebaseStorage.getInstance().reference.child("profile_pictures/thumb_$id.jpg")
                .downloadUrl.
                addOnCompleteListener { task ->

                    if(task.isSuccessful && task.result != null)
                    {
                        Glide.with(context).load(task.result).into(holder.PartnerImage)
                    }
                }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val PartnerName = itemView.findViewById<TextView>(R.id.partnerName)
        val Preview = itemView.findViewById<TextView>(R.id.contentPreview)
        val PartnerImage = itemView.findViewById<CircleImageView>(R.id.partnerImage)
        val MessageTime = itemView.findViewById<RelativeTimeTextView>(R.id.messageTime)
    }
}