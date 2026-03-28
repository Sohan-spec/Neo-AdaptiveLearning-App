package com.neo.android.data

import com.neo.android.data.dao.ChatDao
import com.neo.android.data.dao.MessageDao
import com.neo.android.data.entity.ChatEntity
import com.neo.android.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
) {
    fun getAllChatsFlow(): Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()

    fun getChatByIdFlow(chatId: Long): Flow<ChatEntity?> = chatDao.getChatByIdFlow(chatId)

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId)

    suspend fun createChat(title: String): Long =
        chatDao.insert(ChatEntity(title = title))

    suspend fun deleteChat(chatId: Long) = chatDao.deleteChat(chatId)

    suspend fun updateChatTimestamp(chatId: Long) = chatDao.updateTimestamp(chatId)

    suspend fun insertMessage(message: MessageEntity): Long =
        messageDao.insert(message)
}
