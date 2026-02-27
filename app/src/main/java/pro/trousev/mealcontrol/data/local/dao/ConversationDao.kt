package pro.trousev.mealcontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.ConversationWithMessages

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    suspend fun getAllConversations(): List<ConversationEntity>

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    fun getAllConversationsWithMessages(): List<ConversationWithMessages>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationWithMessages(conversationId: Long): ConversationWithMessages?

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversation(conversationId: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)
}
