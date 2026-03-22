package com.lovecoach.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class ChatManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("ChatManagerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val chats = mutableListOf<ChatData>()
    private val chatHistories = mutableMapOf<String, MutableList<ChatMessage>>()
    private val chatAnalyses = mutableMapOf<String, ChatAnalysisResult>()

    init {
        loadChats()
        loadChatHistories()
    }

    fun getChats(): List<ChatData> = chats

    fun getChatById(id: String): ChatData? = chats.find { it.id == id }

    fun addChat(chat: ChatData) {
        chats.add(0, chat)
        chatHistories[chat.id] = mutableListOf()
        saveChats()
    }

    fun removeChat(id: String) {
        chats.removeIf { it.id == id }
        chatHistories.remove(id)
        chatAnalyses.remove(id)
        saveChats()
        saveChatHistories()
    }

    fun addMessage(chatId: String, message: ChatMessage) {
        val history = chatHistories[chatId] ?: mutableListOf()
        history.add(message)
        chatHistories[chatId] = history
        
        // Update last message in chat data
        val chat = getChatById(chatId)
        chat?.let {
            val updatedChat = it.copy(
                lastMessage = message.content,
                timestamp = message.timestamp
            )
            chats.remove(it)
            chats.add(0, updatedChat)
            saveChats()
        }
        
        saveChatHistories()
    }

    fun getChatHistory(chatId: String): List<ChatMessage> = chatHistories[chatId] ?: emptyList()

    fun saveChatAnalysis(chatId: String, analysis: ChatAnalysisResult) {
        chatAnalyses[chatId] = analysis
    }

    fun getChatAnalysis(chatId: String): ChatAnalysisResult? = chatAnalyses[chatId]

    private fun saveChats() {
        val json = gson.toJson(chats)
        sharedPreferences.edit().putString("chats", json).apply()
    }

    private fun loadChats() {
        val json = sharedPreferences.getString("chats", "")
        if (json?.isNotEmpty() == true) {
            val type = object : TypeToken<List<ChatData>>() {}.type
            val loadedChats = gson.fromJson<List<ChatData>>(json, type)
            chats.addAll(loadedChats)
        }
    }

    private fun saveChatHistories() {
        val json = gson.toJson(chatHistories)
        sharedPreferences.edit().putString("chat_histories", json).apply()
    }

    private fun loadChatHistories() {
        val json = sharedPreferences.getString("chat_histories", "")
        if (json?.isNotEmpty() == true) {
            val type = object : TypeToken<Map<String, List<ChatMessage>>>() {}.type
            val loadedHistories = gson.fromJson<Map<String, List<ChatMessage>>>(json, type)
            loadedHistories.forEach { (chatId, messages) ->
                chatHistories[chatId] = messages.toMutableList()
            }
        }
    }
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
) {
    constructor(content: String, isUser: Boolean) : this(
        UUID.randomUUID().toString(),
        content,
        isUser,
        System.currentTimeMillis()
    )
}
