package mad24.polito.it.chats

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import mad24.polito.it.R
import mad24.polito.it.models.ChatMessage
import java.util.*

class MessagesRecyclerAdapter constructor(_lastAccess : String): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>()
{
    lateinit var RootView : View
    lateinit var Holder : ViewHolder

    private var TheirLastAccess = _lastAccess
    private var IsHere : Boolean = false
    private var Messages : ArrayList<ChatMessage> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder
    {
        RootView  = LayoutInflater.from(parent!!.context).inflate(R.layout.adapter_chat_layout, parent, false)
        Holder = ViewHolder(RootView)

        return ViewHolder(RootView)
    }

    fun Here() { Log.d("CHAT", "Here()"); IsHere = true }
    fun NotHere() { Log.d("CHAT", "NotHere()"); IsHere = false }
    fun setLastHere(last : String){ Log.d("CHAT", "setLastHere()"); TheirLastAccess = last }

    fun bulkPush(/*messages: DataSnapshot*/messages : Iterable<DataSnapshot>)
    {
        val count : Int = Messages.size

        for(m in messages.reversed())
        {
            val c : ChatMessage? = m.getValue(ChatMessage::class.java)
            if(m != null) Messages.add(c!!)
        }

        notifyItemRangeInserted(count, Messages.size)
    }

    fun push(message : DataSnapshot)
    {
        val c : ChatMessage? = message.getValue(ChatMessage::class.java)
        Messages.add(0, c!!)
        notifyItemInserted(0)
    }

    override fun getItemCount(): Int
    {
        return Messages.size
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        holder!!.Content.text = Messages[position].content
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var Content : TextView

        //  Just for test:
        lateinit private var Sent : TextView
        lateinit private var Received : TextView
        lateinit private var Read : TextView

        init
        {
            Content = itemView.findViewById(R.id.messageContent)
            /*tv_title = itemView.findViewById<View>(R.id.book_title) as TextView
            tv_author = itemView.findViewById<View>(R.id.book_author) as TextView
            tv_location = itemView.findViewById<View>(R.id.book_location) as TextView
            book_img = itemView.findViewById<View>(R.id.book_img) as ImageView*/
        }
    }

}