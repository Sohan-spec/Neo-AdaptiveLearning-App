package com.neo.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neo.android.data.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Query("SELECT * FROM chats ORDER BY updatedAtMillis DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatByIdFlow(chatId: Long): Flow<ChatEntity?>

    @Query("UPDATE chats SET updatedAtMillis = :time WHERE id = :chatId")
    suspend fun updateTimestamp(chatId: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateTitle(chatId: Long, title: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: Long)
}
