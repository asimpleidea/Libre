package mad24.polito.it.chats

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import mad24.polito.it.R
import mad24.polito.it.models.Chat

class ConversationsAdapter constructor(_context : Context): RecyclerView.Adapter<ConversationsAdapter.ViewHolder>()
{
    private val Conversations = ArrayList<Chat>()
    private lateinit var RootView : View
    private lateinit var Holder : ViewHolder
    private val context = _context
    private var UsersToLoad : Int = 0
    private val UserReference = FirebaseDatabase.getInstance().reference.child("users")

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

    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        if(holder == null) return

        holder.Preview.text = Conversations[position].preview

        //  Already loaded?
        if(UserNames.containsKey(Conversations[position].partner_id)) holder.PartnerName.text = UserNames[Conversations[position].partner_id]
        else loadUser(Conversations[position].partner_id, holder)

        //  Set the image
        if(UserNames.containsKey(Conversations[position].partner_id)) Glide.with(context).load(UserPictures[Conversations[position].partner_id]).into(holder.PartnerImage)
        else loadPicture(Conversations[position].partner_id, holder)

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
                    UserNames[id] = name
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
                        UserPictures[id] = task.result
                        Glide.with(context).load(task.result).into(holder.PartnerImage)
                    }
                }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val PartnerName = itemView.findViewById<TextView>(R.id.partnerName)
        val Preview = itemView.findViewById<TextView>(R.id.contentPreview)
        val PartnerImage = itemView.findViewById<CircleImageView>(R.id.partnerImage)
    }
}