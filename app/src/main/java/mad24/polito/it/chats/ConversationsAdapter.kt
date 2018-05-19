package mad24.polito.it.chats

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mad24.polito.it.R
import mad24.polito.it.models.Chat

class ConversationsAdapter constructor(): RecyclerView.Adapter<ConversationsAdapter.ViewHolder>()
{
    private val Conversations = ArrayList<Chat>()
    private lateinit var RootView : View
    private lateinit var Holder : ViewHolder

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

        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int)
    {
        //holder.Preview.text = Conversations[position].
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val PartnerName = itemView.findViewById<TextView>(R.id.partnerName)
        val Preview = itemView.findViewById<TextView>(R.id.contentPreview)
        val PartnerImage = itemView.findViewById<TextView>(R.id.partnerImage)
    }
}