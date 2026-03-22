package com.lovecoach.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val chats: MutableList<ChatData>,
    private val onChatClick: (ChatData) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chats.size

    fun addChat(chat: ChatData) {
        chats.add(0, chat) // Add to the beginning of the list
        notifyItemInserted(0)
    }

    fun addChats(chatList: List<ChatData>) {
        chats.addAll(chatList)
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(R.id.chat_name)
        private val lastMessage: TextView = itemView.findViewById(R.id.last_message)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)

        fun bind(chat: ChatData) {
            chatName.text = chat.name
            lastMessage.text = chat.lastMessage
            timestamp.text = formatTimestamp(chat.timestamp)

            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return sdf.format(date)
        }
    }
}
