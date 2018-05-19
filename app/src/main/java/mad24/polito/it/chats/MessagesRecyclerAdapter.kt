package mad24.polito.it.chats

import android.content.Context
import android.os.Message
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import mad24.polito.it.R
import mad24.polito.it.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class MessagesRecyclerAdapter constructor(_context : Context, _lastAccess : String): RecyclerView.Adapter<MessagesRecyclerAdapter.ViewHolder>()
{
    lateinit var RootView : View
    lateinit var Holder : ViewHolder
    val Me : String = FirebaseAuth.getInstance().currentUser!!.uid

    private var PartnerLastAccess = _lastAccess
    private var context = _context
    private var ParnerIsHere : Boolean = false
    private var Messages : ArrayList<ChatMessage> = ArrayList()
    private var LastDate : String? = null
    private var PreviousHolder : ViewHolder? = null

    private val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMMM", Locale.getDefault())
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    private val MY_MESSAGE = 0
    private val PARTNER_MESSAGE = 1

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder
    {
        val layout = (fun() : Int
        {
            if(viewType == MY_MESSAGE) return R.layout.my_message
            return R.layout.partner_message
        }())

        RootView = LayoutInflater.from(parent!!.context).inflate(layout, parent, false)

        Holder = ViewHolder(RootView)

        return ViewHolder(RootView)
    }

    fun here()
    {
        ParnerIsHere = true

        //  Read all unread
        var end = 0
        for(c in Messages)
        {
            //  Kotlin sometimes suggests to do a compareTo and sometimes to do just a < or > ... boh
            //  If partner is here, then PartnerLastAccess contains the moment they were here *before* that
            //  Or "0", if never entered this chat
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

    fun bulkPush(messages : List<ChatMessage>, queue: Boolean = true)
    {
        val count : Int = Messages.size
        var pushed = 0

        for(m in messages)
        {
            val date = df.parse(m.sent)

            m.time = hourFormat.format(date)
            m.date = dateFormat.format(date)

            if(queue) Messages.add(m)
            else
            {
                Messages.add(0, m)
                ++pushed
            }
        }

        if(queue)
        {
            notifyItemRangeInserted(count, Messages.size)
            return
        }

        notifyItemRangeInserted(0, pushed)
    }

    fun push(message : ChatMessage, onTop : Boolean = false)
    {
        val date =  df.parse(message.sent)

        message.time = hourFormat.format(date)
        message.date = dateFormat.format(date)

        if(!onTop)
        {
            Messages.add(0, message)
            notifyItemInserted(0)
            return
        }

        Messages.add(Messages.size, message)
        notifyItemInserted(Messages.size -1)
    }

    override fun getItemViewType(position: Int): Int
    {
        //return super.getItemViewType(position)

        if(Messages[position].by == Me) return MY_MESSAGE
        return PARTNER_MESSAGE
    }

    override fun getItemCount(): Int
    {
        return Messages.size
    }


    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        if(holder == null) return

        if(Messages[position].by.compareTo(Me) == 0)
        {
            //  Was this message sent by me?
            //  Did my partner connect to this chat after this message?
            if(PartnerLastAccess > Messages[position].sent || ParnerIsHere) holder.Read.setImageResource(R.drawable.ic_read)
        }

        if(Messages.size == 1 || Messages[position].firstOfTheDay)
        {
            holder.DateDivider.text = Messages[position].date
            (holder.DateDivider.parent as FrameLayout).visibility = View.VISIBLE
        }

        //  Set the content
        holder.Content.text = Messages[position].content

        //  Set the time
        holder.Sent.text = Messages[position].time

        PreviousHolder = holder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var Content : TextView
        var Container : LinearLayout
        var Card : CardView
        var DateDivider : TextView
        var Sent : TextView
        var Read : ImageView


        init
        {
            Content = itemView.findViewById(R.id.messageContent)
            Container = itemView.findViewById(R.id.messageContainer)
            Card = itemView.findViewById(R.id.messageContentCard)
            DateDivider = itemView.findViewById(R.id.dateDivider)
            Read = itemView.findViewById(R.id.messageIsRead)
            Sent = itemView.findViewById(R.id.messageSent)
        }
    }

}