package de.zettelgeist.app.data.repository

import de.zettelgeist.app.data.db.AppDatabase
import de.zettelgeist.app.data.model.ChatMessage
import de.zettelgeist.app.data.model.Conversation
import de.zettelgeist.app.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ChatRepository(private val db: AppDatabase) {

    private val chatDao = db.chatDao()

    fun getAllConversations(): Flow<List<Conversation>> =
        chatDao.getAllConversations()

    suspend fun getConversationById(id: String): Conversation? =
        chatDao.getConversationById(id)

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesForConversation(conversationId)

    suspend fun getRecentMessages(conversationId: String, limit: Int = 10): List<ChatMessage> =
        chatDao.getRecentMessages(conversationId, limit)

    suspend fun createConversation(firstMessage: String): Conversation {
        val now = Instant.now().toString()
        val conversation = Conversation(
            id = IdGenerator.generate(),
            title = firstMessage.take(50),
            createdAt = now,
            updatedAt = now
        )
        chatDao.insertConversation(conversation)
        return conversation
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        contextNoteIds: String? = null
    ): ChatMessage {
        val now = Instant.now().toString()
        val message = ChatMessage(
            id = IdGenerator.generate(),
            conversationId = conversationId,
            role = role,
            content = content,
            contextNoteIds = contextNoteIds,
            createdAt = now
        )
        chatDao.insertMessage(message)
        chatDao.insertConversation(
            chatDao.getConversationById(conversationId)?.copy(updatedAt = now)
                ?: Conversation(
                    id = conversationId,
                    title = content.take(50),
                    createdAt = now,
                    updatedAt = now
                )
        )
        return message
    }

    suspend fun deleteConversation(id: String) {
        chatDao.deleteMessagesForConversation(id)
        chatDao.deleteConversation(id)
    }
}
