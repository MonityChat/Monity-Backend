package de.devin.monity.util.notifications

import de.devin.monity.network.db.chat.ChatData
import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.chat.ReactionData
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

/**
 * Occurs when a new private chat was created
 * @param creator the user who created the chat
 * @param chat the chat which was created
 */
class PrivateChatCreatedNotification(private val creator: UUID, private val chat: ChatData): Notification {

    override val from: UUID = creator
    override val name: String = "chat:private:created"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(creator).username).put("chat", toJSON(chat)))
        return json
    }
}

/**
 * Occurs when a new message is incoming
 * @param sender the sender of the message
 * @param chatID the chat in which the message was sent
 * @param message the message which was sent
 */
class PrivateChatMessageReceivedNotification(private val sender: UUID, private val chatID: UUID, val message: MessageData): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("chatID", chatID.toString()).put("message", toJSON(message)))
        return json
    }
}

/**
 * Occurs when a user received a message
 * @param sender the user who received the message
 */
class PrivateChatUserReceivedMessagesNotification(private val sender: UUID,private val chatID: UUID): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:received"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("chatID", chatID.toString()))
        return json
    }
}

/**
 * Occurs when a message was deleted
 * @param sender the user who deleted the message
 * @param chatID in which chat the message was deleted
 * @param message the ID of the deleted message
 */
class PrivateChatMessageDeletedNotification(private val sender: UUID, private val chatID: UUID,  val message: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:deleted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("messageID", message).put("chat", chatID.toString()))
        return json
    }
}

/**
 * Occurs when a message was reacted
 * @param sender the reactor
 * @param message the message as finished JSON Object
 */
class PrivateChatUserReactedToMessageNotification(private val sender: UUID, private val message: JSONObject): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:reacted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("message", message))
        return json
    }
}

/**
 * Occurs when a message was edited
 * @param sender the user who edited the message
 * @param message the message as finished JSONObject
 */
class PrivateChatMessageEditNotification(private val sender: UUID, private val message: JSONObject): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:edit"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("message", message))
        return json
    }
}

/**
 * Occurs when a message was read
 * @param sender the user who read the message
 * @param chatID in which chat he read the messages
 */
class PrivateChatMessageReadNotification(private val sender: UUID, private val chatID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:read"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("chat", chatID.toString()))
        return json
    }
}