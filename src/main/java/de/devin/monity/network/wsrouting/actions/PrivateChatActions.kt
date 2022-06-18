package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.chat.*
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.Error
import de.devin.monity.util.MessageStatus
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.*
import de.devin.monity.util.toJSON
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PrivateChatSendMessage: Action {

    override val name: String
        get() = "chat:private:send:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"), Parameter("embedID"), Parameter("content"), Parameter("sent"), Parameter("related"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        val content = request.getString("content")
        val sent = request.getLong("sent")
        val embedID = request.getString("embedID")
        val relatedID = request.getString("related")

        if (UserContactDB.hasBlocked(sender, targetUUID)) return Error.USER_BLOCKED_TARGET.toJson()
        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()

        if (!ChatDB.hasPrivateChat(sender, targetUUID)) {
            //chat does not exist yet, so create one
            val chat = ChatData(sender, targetUUID, ChatDB.newID(), System.currentTimeMillis(), listOf())
            ChatDB.insert(chat)
            UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatCreatedNotification(sender, chat))
        }

        val chat = ChatDB.getPrivateChatBetween(sender, targetUUID)

        val messageID = MessageDB.getFreeUUID()
        val index = MessageDB.getNextIndex(chat.id)
        val embeds = if (embedID.isNotEmpty()) MediaDB.get(UUID.fromString(embedID)) else listOf()
        val related = if (relatedID.isNotEmpty()) MessageDB.get(UUID.fromString(relatedID)) else null

        val status = if (UserHandler.isOnline(targetUUID)) MessageStatus.RECEIVED else MessageStatus.PENDING

        val message = MessageData(sender, messageID, chat.id, content, related, embeds, listOf(), index, sent, false, status)


        MessageDB.insert(message)
        UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatMessageReceivedNotification(sender, chat.id, message))

        return toJSON(message).put("author", UserDB.get(sender).username)
    }
}

class PrivateChatGetLatestMessages: Action {
    override val name: String
        get() = "chat:private:get:messages:latest"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")

        if (chatIDRaw.isEmpty())
            return JSONObject().put("messages", JSONArray())

        val chatID = UUID.fromString(chatIDRaw)

        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        val chat = ChatDB.get(chatID)

        val messages = chat.messages.sortedByDescending { it.index }.slice(0..Math.min(50, chat.messages.size - 1))

        chat.messages.forEach { if(it.sender != sender) MessageDB.editMessageStatus(it.messageID, MessageStatus.READ)}

        val json = JSONObject()
        val messagesArray = JSONArray()
        messages.forEach { messagesArray.put(toJSON(it).put("author", UserDB.get(it.sender).username)) }

        val target = if (chat.initiator == sender) chat.otherUser else chat.initiator

        UserHandler.sendNotificationIfOnline(target, PrivateChatMessageReadNotification(sender, chatID))

        return json.put("messages", messagesArray)
    }
}

class PrivateChatGetMessages: Action {
    override val name: String
        get() = "chat:private:get:messages"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("start"), Parameter("amount"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)

        val start = request.getInt("start")
        val amount = request.getInt("amount")

        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()

        val chat = ChatDB.get(chatID)

        val messages = chat.messages.sortedByDescending { it.index }.slice(start..start+amount)
        return toJSON(messages)
    }
}

class PrivateChatDeleteMessage: Action {

    override val name: String
        get() = "chat:private:delete:message"
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
        val chat = ChatDB.get(chatID)
        val otherUser = if (chat.initiator == sender) chat.otherUser else chat.initiator

        UserHandler.sendNotificationIfOnline(otherUser, PrivateChatMessageDeletedNotification(sender, chatID, messageID))

        return Error.NONE.toJson()
    }
}

class PrivateChatReactMessage: Action {

    override val name: String
        get() = "chat:private:react:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("messageID"), Parameter("reaction"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)
        val reaction = request.getString("reaction")

        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()
        if (ReactionDB.hasUserReacted(messageID, sender, reaction)) return Error.USER_ALREADY_REACTED.toJson()

        val reactionData = ReactionDB.addReactionToMessage(messageID, sender, reaction)

        val chat = ChatDB.get(MessageDB.get(messageID).chat)
        UserHandler.sendNotificationIfOnline( if (chat.initiator == sender) chat.otherUser else chat.initiator, PrivateChatUserReactedToMessageNotification(sender,reactionData , chat.id ))

        return toJSON(reactionData)
    }
}

class PrivateChatEditMessage: Action {
    override val name: String
        get() = "chat:private:edit:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("messageID"), Parameter("chatID"), Parameter("newContent"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)

        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)

        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()

        MessageDB.editMessageContent(messageID, request.getString("newContent"))

        val editMessage = MessageDB.get(messageID)

        val chat = ChatDB.get(chatID)
        val otherUser = if (chat.initiator == sender) chat.otherUser else chat.initiator

        UserHandler.sendNotificationIfOnline(otherUser, PrivateChatMessageEditNotification(sender, chatID, editMessage))

        return toJSON(editMessage)
    }
}

class PrivateChatMessageRead: Action {

    override val name: String
        get() = "chat:private:read:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatID = UUID.fromString(request.getString("chatID"))
        val target = UUID.fromString(request.getString("target"))

        val chat = ChatDB.get(chatID)

        for (message in chat.messages) {
            if (message.sender != sender) {
                MessageDB.editMessageStatus(message.messageID, MessageStatus.READ)
            }
        }

        UserHandler.sendNotificationIfOnline(target, PrivateChatMessageReadNotification(sender, chatID))
        return Error.NONE.toJson()
    }
}