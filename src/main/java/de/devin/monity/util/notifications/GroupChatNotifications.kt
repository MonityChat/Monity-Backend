package de.devin.monity.util.notifications

import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

class GroupChatMessageReceivedNotification(private val sender: UUID, private val chatID: UUID, private val message: MessageData): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:message:received"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(sender))

        json.put("content", JSONObject().put("from", userJson).put("message", toJSON(message)).put("chat", chatID.toString()))
        return json
    }
}

class GroupChatMessageDeletedNotification(private val sender: UUID, private val chatID: UUID, private val message: UUID): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:message:deleted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(sender))

        json.put("content", JSONObject().put("from", userJson).put("message", message.toString()).put("chat", chatID.toString()))
        return json
    }
}

class GroupChatMessageEditNotification(private val sender: UUID, private val chatID: UUID, private val message: MessageData): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:message:edit"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(sender))

        json.put("content", JSONObject().put("from", userJson).put("message", toJSON(message)).put("chat", chatID.toString()))
        return json
    }
}