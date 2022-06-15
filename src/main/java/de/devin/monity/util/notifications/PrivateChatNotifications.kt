package de.devin.monity.util.notifications

import de.devin.monity.model.Message
import de.devin.monity.network.db.ChatData
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

class PrivateChatCreatedNotification(private val creator: UUID, private val chat: ChatData): Notification {

    override val from: UUID = creator
    override val name: String = "notification:chat:private:created"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(UserDB.get(creator))

        json.put("content", JSONObject().put("from", userJson).put("chat", toJSON(chat)))
        return json
    }
}
class PrivateChatMessageReceivedNotification(private val sender: UUID, private val message: Message): Notification {

    override val from: UUID
        get() = sender
    override val name: String
        get() = "notification:chat:private:message:received"

    override fun toJson(): JSONObject {
        TODO("Not yet implemented")
    }
}