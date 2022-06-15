package de.devin.monity.network.wsrouting.actions

import de.devin.monity.model.Message
import de.devin.monity.network.db.ChatDB
import de.devin.monity.network.db.ChatData
import de.devin.monity.util.Error
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.PrivateChatCreatedNotification
import org.json.JSONObject
import java.util.*

class ChatAction: Action {

    override val name: String
        get() = "chat:send:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"), Parameter("embedID"), Parameter("content"), Parameter("sent"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        val content = request.getString("content")
        val sent = request.getLong("sent")
        val embedID = request.getString("embedID")

        val user = UserHandler.getOnlineUser(sender)
        val chats = user.privateChats


        if (chats.any { it.initiator == user.uuid || it.otherUser == user.uuid }) {
            //chat does not exist yet, so create one
            val chat = ChatData(sender, targetUUID,ChatDB.newID(), System.currentTimeMillis(), listOf())
            ChatDB.insert(chat)
            UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatCreatedNotification(sender, chat))
        }

        val message = Message(content, sender, sent, if (embedID.isNotEmpty()) UUID.fromString(embedID) else null)

        return Error.NONE.toJson()
    }
}