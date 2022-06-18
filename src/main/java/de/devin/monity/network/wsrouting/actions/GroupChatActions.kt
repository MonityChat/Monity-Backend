package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.chat.*
import de.devin.monity.util.Error
import de.devin.monity.util.GroupRole
import de.devin.monity.util.MessageStatus
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.GroupChatMessageDeletedNotification
import de.devin.monity.util.notifications.GroupChatMessageEditNotification
import de.devin.monity.util.notifications.GroupChatMessageReceivedNotification
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*


class GroupChatSendMessage: Action {

    override val name: String
        get() = "chat:group:send:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("embedID"), Parameter("content"), Parameter("sent"), Parameter("related"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)

        val content = request.getString("content")
        val sent = request.getLong("sent")
        val embedID = request.getString("embedID")
        val relatedID = request.getString("related")

        val user = UserHandler.getOnlineUser(sender)
        val chats = user.groupChats

        if (chats.none {it.id == chatID}) return Error.CHAT_NOT_FOUND.toJson()
        val chat = GroupDB.get(chatID)

        if (GroupSettingDB.get(chatID).rolesOnly) {
            if (GroupMemberDB.getGroupMemberFor(chatID, sender).role.weight <= GroupRole.MEMBER.weight) return Error.UNAUTHORIZED.toJson()
        }

        val messageID = MessageDB.getFreeUUID()
        val index = MessageDB.getNextIndex(chat.id)
        val embeds = if (embedID.isNotEmpty()) MediaDB.get(UUID.fromString(embedID)) else listOf()
        val related = if (relatedID.isNotEmpty()) MessageDB.get(UUID.fromString(relatedID)) else null


        val status = if (chat.members.all { UserHandler.isOnline(it.id) }) MessageStatus.RECEIVED else MessageStatus.PENDING

        val message = MessageData(sender, messageID, chat.id, content, related, embeds, listOf(), index, sent, false, status)

        MessageDB.insert(message)

        chat.members.forEach{ UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageReceivedNotification(sender, chatID, message))}

        return toJSON(message).put("author", user.getUserName())
    }
}


class GroupChatMessageDelete: Action {
    override val name: String
        get() = "chat:group:delete:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("messageID"))
    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)


        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()

        MessageDB.removeMessage(messageID)
        val chat = GroupDB.get(chatID)

        chat.members.forEach {UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageDeletedNotification(sender, chatID, messageID)) }

        return Error.NONE.toJson()
    }
}

class GroupChatMessageEdit: Action {
    override val name: String
        get() = "chat:group:delete:edit"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("messageID"), Parameter("newContent"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)


        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()

        MessageDB.editMessageContent(messageID, request.getString("newContent"))
        val chat = GroupDB.get(chatID)

        chat.members.forEach {UserHandler.sendNotificationIfOnline(it.id, GroupChatMessageEditNotification(sender,chatID , MessageDB.get(messageID))) }

        return Error.NONE.toJson()
    }
}