package mad24.polito.it.chats

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import mad24.polito.it.R
import mad24.polito.it.models.ChatMessage
import java.util.*

class MessagesRecyclerAdapter constructor(_lastAccess : String): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>()
{
    lateinit var RootView : View
    lateinit var Holder : ViewHolder
    lateinit var Me : String

    private var PartnerLastAccess = _lastAccess
    private var ParnerIsHere : Boolean = false
    private var Messages : ArrayList<ChatMessage> = ArrayList()
    private var LastReadMessage : String? = null

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder
    {
        RootView  = LayoutInflater.from(parent!!.context).inflate(R.layout.adapter_chat_layout, parent, false)
        Holder = ViewHolder(RootView)
        Me = FirebaseAuth.getInstance().currentUser!!.uid

        return ViewHolder(RootView)
    }

    fun here()
    {
        ParnerIsHere = true

        //  Read all unread
        Log.d("CHAT", "partner is here")
        var end = 0
        for(c in Messages)
        {
            //  Kotlin sometimes suggests to do a compareTo and sometimes to do just a < or > ... boh
            //  If partner is here, then PartnerLastAccess contains the moment they were here *before* that
            //  Or "0", if never entered this chat
            Log.d("CHAT", "partner is here: ${c.sent} and ${PartnerLastAccess}")
            when(c.sent < PartnerLastAccess || PartnerLastAccess.compareTo("0") == 0)
            {
                true -> notifyItemRangeChanged(0, ++end)
                false -> ++end
            }
        }
    }

    fun notHere()
    {
        ParnerIsHere = false
    }

    fun setLastHere(last : String){ PartnerLastAccess = last }

    fun bulkPush(/*messages: DataSnapshot*/messages : Iterable<DataSnapshot>)
    {
        val count : Int = Messages.size

        for(m in messages.reversed())
        {
            val c : ChatMessage? = m.getValue(ChatMessage::class.java)
            Messages.add(c!!)
        }

        notifyItemRangeInserted(count, Messages.size)
    }

    fun push(message : DataSnapshot, onTop : Boolean = false)
    {
        val c : ChatMessage? = message.getValue(ChatMessage::class.java)
        push(c!!, onTop)
    }

    fun push(message : ChatMessage, onTop : Boolean = false)
    {
        if(!onTop)
        {
            Messages.add(0, message)
            notifyItemInserted(0)
            return
        }

        Messages.add(Messages.size, message)
        notifyItemInserted(Messages.size -1)
    }

    override fun getItemCount(): Int
    {
        return Messages.size
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        if(holder == null) return

        when(Messages[position].by.compareTo(Me) == 0)
        {
            //  Was this message sent by me?
            true ->
            {
                Log.d("CHAT", "message sent in ${Messages[position].sent} and last access $PartnerLastAccess")
                holder.Align.text = "DEBUG: my message, so this goes on right"

                //  Did my partner connect to this chat after this message?
                if(PartnerLastAccess > Messages[position].sent || ParnerIsHere)
                {
                    holder.Read.text = "DEBUG: message has been read"
                    holder.Read.visibility = View.VISIBLE
                }
            }
            false ->
            {
                holder.Align.text = "DEBUG: Partner's message, so this goes on left"
            }
        }

        //  Set the content
        holder.Content.text = Messages[position].content

        //  For debug purposes
        holder.Sent.text = "DEBUG: sent: ${Messages[position].sent}"
        holder.Received.text = "DEBUG: received: ${Messages[position].received}"
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var Content : TextView

        //  Just for test:
        var Align : TextView
        var Sent : TextView
        var Received : TextView
        var Read : TextView

        init
        {
            Content = itemView.findViewById(R.id.messageContent)
            Align = itemView.findViewById(R.id.messageAlignment)
            Sent = itemView.findViewById(R.id.messageSent)
            Received = itemView.findViewById(R.id.messageReceived)
            Read = itemView.findViewById(R.id.messageIsRead)
        }
    }

}