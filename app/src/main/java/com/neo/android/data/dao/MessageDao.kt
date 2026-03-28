package com.neo.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neo.android.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAtMillis ASC")
    fun getMessagesForChatFlow(chatId: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)
}
