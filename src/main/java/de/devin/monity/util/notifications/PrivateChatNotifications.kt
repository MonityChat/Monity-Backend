package de.devin.monity.util.notifications

import de.devin.monity.network.db.chat.ChatData
import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.chat.ReactionData
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

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
class PrivateChatMessageReceivedNotification(private val sender: UUID,private val chatID: UUID, val message: MessageData): Notification {

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

class PrivateChatMessageDeletedNotification(private val sender: UUID, private val chatID: UUID,  val message: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:deleted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("messageID", toJSON(message)).put("chat", chatID.toString()))
        return json
    }
}

class PrivateChatUserReactedToMessageNotification(private val sender: UUID, private val reactionData: ReactionData, private val chatID: UUID): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:reacted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("reaction", toJSON(reactionData)).put("chat", chatID.toString()))
        return json
    }
}

class PrivateChatMessageEditNotification(private val sender: UUID, private val chatID: UUID, private val message: MessageData): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:edit"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("message", toJSON(message)).put("chat", chatID.toString()))
        return json
    }
}

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

class PrivateChatUserLoggedInNotification(private val sender: UUID, private val chatID: UUID): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:private:message:received"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", UserDB.get(sender).username).put("chat", chatID.toString()))
        return json
    }


}