package de.devin.monity.network.wsrouting.actions

import de.devin.monity.LocationGetter
import de.devin.monity.network.db.chat.*
import de.devin.monity.network.db.user.UserContactDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.Error
import de.devin.monity.util.MessageStatus
import de.devin.monity.util.TypingManager
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.*
import de.devin.monity.util.toJSON
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*


/**
 * Action when a user sends a message
 */
class PrivateChatSendMessageAction: Action {

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


        val embeds = if (embedID.isNotEmpty()) MediaDB.get(UUID.fromString(embedID)) else emptyList()
        val related = if (relatedID.isNotEmpty()) MessageDB.get(UUID.fromString(relatedID)) else null

        val status = if (UserHandler.isOnline(targetUUID)) MessageStatus.RECEIVED else MessageStatus.PENDING

        val message = MessageData(sender, messageID, chat.id, content, related, embeds, emptyList(), index, sent, false, status)



        MessageDB.insert(message)
        UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatMessageReceivedNotification(sender, chat.id, message))

        if (TypingManager.isTyping(sender, targetUUID, chat.id))
            TypingManager.stoppedTypingBecauseSendMessage(sender, targetUUID, chat.id)

        val json = toJSON(message)
        if (message.relatedTo != null) {
            json.getJSONObject("relatedTo").put("relatedAuthor", UserDB.get(message.relatedTo.sender).username)
        }

        return json.put("author", UserDB.get(sender).username)
    }
}

/**
 * Return the 50 latest messages of the given chat
 */
class PrivateChatGetLatestMessagesAction: Action {
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
        if (!ChatDB.isInChat(sender, chatID)) return Error.UNAUTHORIZED.toJson()
        val chat = ChatDB.get(chatID)

        val messages = chat.messages.sortedByDescending { it.index }.slice(0..50.coerceAtMost(chat.messages.size - 1))

        chat.messages.forEach { if(it.sender != sender) MessageDB.editMessageStatus(it.messageID, MessageStatus.READ)}

        val json = JSONObject()
        val messagesArray = JSONArray()
        messages.forEach {
            val json = toJSON(it)
            if (it.relatedTo != null) {
                json.getJSONObject("relatedTo").put("relatedAuthor", UserDB.get(it.relatedTo.sender).username)
            }

            messagesArray.put(json.put("author", UserDB.get(it.sender).username))
        }

        val target = if (chat.initiator == sender) chat.otherUser else chat.initiator

        UserHandler.sendNotificationIfOnline(target, PrivateChatMessageReadNotification(sender, chatID))

        return json.put("messages", messagesArray)
    }
}

/**
 * Action when a user gets messages from a chat
 */
class PrivateChatGetMessagesAction: Action {
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
        if (!ChatDB.isInChat(sender, chatID)) return Error.UNAUTHORIZED.toJson()

        val chat = ChatDB.get(chatID)

        val messages = mutableListOf<MessageData>()
        for (number in (start - amount).coerceAtLeast(0)..start) {
            if (chat.messages.any {it.index == number.toLong()})
                messages += chat.messages.first {it.index == number.toLong()}
        }
        val returnList = messages.sortedByDescending { it.index }

        val json = JSONObject()
        val messagesArray = JSONArray()
        returnList.forEach {
            val json = toJSON(it)
            if (it.relatedTo != null) {
                json.getJSONObject("relatedTo").put("relatedAuthor", UserDB.get(it.relatedTo.sender).username)
            }
            messagesArray.put(json.put("author", UserDB.get(it.sender).username))
        }

        return json.put("messages", messagesArray)
    }
}

/**
 * Action when a user deletes a message
 */
class PrivateChatDeleteMessageAction: Action {

    override val name: String
        get() = "chat:private:delete:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("chatID"), Parameter("messageID"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)

        val chat = ChatDB.get(chatID)
        val targetUUID = if (chat.initiator == sender) chat.otherUser else chat.initiator


        if (UserContactDB.hasBlocked(sender, targetUUID)) return Error.USER_BLOCKED_TARGET.toJson()
        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()
        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()
        if (!ChatDB.isInChat(sender, chatID)) return Error.UNAUTHORIZED.toJson()


        val message = MessageDB.get(messageID)

        if (message.sender != sender) return Error.NOT_MESSAGE_AUTHOR.toJson()

        for (media in message.attachedMedia) {
            val file = File(LocationGetter().getLocation().absolutePath + "/../data${media.filePath}")
            file.delete()
        }

        MediaDB.deleteIfExists(messageID)
        MessageDB.removeMessage(messageID)
        if (message.attachedMedia.isNotEmpty()) {
            ReactionDB.deleteReactions(message.attachedMedia[0].id)
        }

        val otherUser = if (chat.initiator == sender) chat.otherUser else chat.initiator


        UserHandler.sendNotificationIfOnline(otherUser, PrivateChatMessageDeletedNotification(sender, chatID, messageID))

        return JSONObject().put("message", messageID)
    }
}

/**
 * Action when a user reacts to a message
 * @see ReactionData
 */
class PrivateChatReactMessageAction: Action {

    override val name: String
        get() = "chat:private:react:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("messageID"), Parameter("reaction"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)
        val reaction = request.getString("reaction")

        var message = MessageDB.get(messageID)

        val chat = ChatDB.get(message.chat)
        val targetUUID = if (chat.initiator == sender) chat.otherUser else chat.initiator

        if (UserContactDB.hasBlocked(sender, targetUUID)) return Error.USER_BLOCKED_TARGET.toJson()
        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()
        if (!ChatDB.isInChat(sender, message.chat)) return Error.UNAUTHORIZED.toJson()

        ReactionDB.addReactionToMessage(messageID, reaction)

        message = MessageDB.get(messageID)

        val messageJSON = JSONObject()
        messageJSON.put("message", toJSON(message))
        messageJSON.getJSONObject("message").put("author", UserDB.get(message.sender).username)
        if (message.relatedTo != null) {
            messageJSON.getJSONObject("message").getJSONObject("relatedTo").put("relatedAuthor", UserDB.get(message.relatedTo!!.sender).username)
        }

        UserHandler.sendNotificationIfOnline( if (chat.initiator == sender) chat.otherUser else chat.initiator, PrivateChatUserReactedToMessageNotification(sender, messageJSON))

        return messageJSON
    }
}

/**
 * Action when a user edits an already sent message
 */
class PrivateChatEditMessageAction: Action {
    override val name: String
        get() = "chat:private:edit:message"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("messageID"), Parameter("chatID"), Parameter("newContent"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val messageIDRaw = request.getString("messageID")
        val messageID = UUID.fromString(messageIDRaw)

        val chatIDRaw = request.getString("chatID")
        val chatID = UUID.fromString(chatIDRaw)
        if (!ChatDB.isInChat(sender, chatID)) return Error.UNAUTHORIZED.toJson()

        val chat = ChatDB.get(chatID)
        val targetUUID = if (chat.initiator == sender) chat.otherUser else chat.initiator

        if (UserContactDB.hasBlocked(sender, targetUUID)) return Error.USER_BLOCKED_TARGET.toJson()
        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()
        if (!ChatDB.has(chatID)) return Error.CHAT_NOT_FOUND.toJson()
        if (!MessageDB.has(messageID)) return Error.MESSAGE_NOT_FOUND.toJson()
        if (!ChatDB.isInChat(sender, chatID)) return Error.UNAUTHORIZED.toJson()

        val message = MessageDB.get(messageID)
        if (message.sender != sender) return Error.NOT_MESSAGE_AUTHOR.toJson()


        MessageDB.editMessageContent(messageID, request.getString("newContent"))

        val editMessage = MessageDB.get(messageID)

        val otherUser = if (chat.initiator == sender) chat.otherUser else chat.initiator


        val messageJSON = JSONObject()
        messageJSON.put("message", toJSON(editMessage))
        messageJSON.getJSONObject("message").put("author", UserDB.get(editMessage.sender).username)
        if (editMessage.relatedTo != null) {
            messageJSON.getJSONObject("message").getJSONObject("relatedTo").put("relatedAuthor", UserDB.get(editMessage.relatedTo.sender).username)
        }

        UserHandler.sendNotificationIfOnline(otherUser, PrivateChatMessageEditNotification(sender, messageJSON))

        return messageJSON
    }
}

/**
 * Action when a users read a chat by entering it
 */
class PrivateChatMessageReadAction: Action {

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