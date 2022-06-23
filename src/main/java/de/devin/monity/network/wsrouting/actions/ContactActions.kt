package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.chat.ChatDB
import de.devin.monity.network.db.chat.ChatData
import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.user.*
import de.devin.monity.network.httprouting.UserData
import de.devin.monity.util.toJSON
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import de.devin.monity.util.Error
import de.devin.monity.util.FriendStatus
import de.devin.monity.util.MessageStatus
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.ContactAddNotification
import de.devin.monity.util.notifications.ContactAddRequestNotification
import de.devin.monity.util.notifications.PrivateChatCreatedNotification

/**
 * Action when a user sends a request to another user
 */
class ContactAddAction: Action {

    override val name: String = "contact:add"
    override val parameters: List<Parameter> = listOf(Parameter("target"))
    override fun execute(sender: UUID, request: JSONObject): JSONObject {

        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        val senderUserProfile = DetailedUserDB.get(sender)

        if (sender == targetUUID) {
            return Error.ALREADY_MADE_CONTACT.toJson()
        }

        if (UserContactDB.areFriends(sender, targetUUID)) {
            return Error.ALREADY_MADE_CONTACT.toJson()
        }

        if (UserContactDB.sendRequest(sender, targetUUID)) {
            return Error.ALREADY_SEND_REQUEST.toJson()
        }

        if (UserContactDB.sendRequest(targetUUID, sender)) {
            UserContactDB.updateStatus(targetUUID, sender, FriendStatus.ACCEPTED)
            UserHandler.sendNotificationIfOnline(targetUUID, ContactAddNotification(senderUserProfile))
            return toJSON(senderUserProfile)
        }

        val friendData = FriendData(sender, targetUUID, FriendStatus.PENDING)
        UserContactDB.insert(listOf(friendData))
        val chat = ChatData(sender, targetUUID, ChatDB.newID(), System.currentTimeMillis(), listOf())
        ChatDB.insert(chat)
        UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatCreatedNotification(sender, chat))

        UserHandler.sendNotificationIfOnline(targetUUID, ContactAddRequestNotification(senderUserProfile))
        return Error.NONE.toJson()
    }
}

/**
 * Action when a user accepts a request
 */
class ContactAcceptAction: Action {

    override val name: String
        get() = "contact:accept"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        //sender nimmt anfrage von target an
        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        if (UserContactDB.areFriends(targetUUID, sender)) {
            return Error.ALREADY_MADE_CONTACT.toJson()
        }

        if (UserContactDB.hasBlocked(sender, targetUUID)) {
            return Error.USER_BLOCKED_TARGET.toJson()
        }

        UserHandler.sendNotificationIfOnline(targetUUID, ContactAddNotification(DetailedUserDB.get(sender)))
        UserContactDB.updateStatus(targetUUID, sender, FriendStatus.ACCEPTED)
        val chat = ChatData(sender, targetUUID, ChatDB.newID(), System.currentTimeMillis(), listOf())
        ChatDB.insert(chat)
        UserHandler.sendNotificationIfOnline(targetUUID, PrivateChatCreatedNotification(sender, chat))
        return toJSON(DetailedUserDB.get(targetUUID))
    }
}

/**
 * Action when a user declines a request
 */
class ContactDeclineAction: Action {
    override val name: String
        get() = "contact:decline"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        //sender lehnt anfrage von target ab
        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        if (UserContactDB.areFriends(sender, targetUUID)) {
            return Error.ALREADY_MADE_CONTACT.toJson()
        }

        UserContactDB.removeRequest(targetUUID, sender)
        return Error.NONE.toJson()
    }
}

/**
 * Action when a user blocks another user
 */
class ContactBlockAction: Action {
    override val name: String
        get() = "contact:block"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        if (UserContactDB.hasBlocked(sender, targetUUID)) {
            return Error.USER_BLOCKED_TARGET.toJson()
        }

        UserContactDB.updateStatus(sender, targetUUID, FriendStatus.BLOCKED)
        return Error.NONE.toJson()
    }
}

/**
 * Action when a user gets his contacts
 */
class ContactGetAction: Action {

    override val name: String
        get() = "contact:get"
    override val parameters: List<Parameter>
        get() = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val contacts = UserContactDB.getContactsFrom(sender)

        var chatID: UUID? = null

        val contactsArray = JSONArray()
        contacts.forEach { contact ->
            var amount = 0
            var latestUnread: MessageData? = null
            if (ChatDB.hasPrivateChat(sender, contact)) {
                val chat = ChatDB.getPrivateChatBetween(sender, contact)
                chatID = chat.id
                amount = chat.messages.count { it.sender != sender && (it.status == MessageStatus.PENDING || it.status == MessageStatus.RECEIVED) }
                latestUnread = chat.messages.maxByOrNull { it.index }
            }

            contactsArray.put(toJSON(DetailedUserDB.get(contact))
                .put("chatID", if (chatID == null) "null" else chatID)
                .put("unreadMessages", amount)
                .put("lastUnread", if (latestUnread == null) "null" else toJSON(latestUnread)))
        }

        return JSONObject().put("contacts", contactsArray)
    }
}

/**
 * Action when a user gets his open requests
 */
class ContactGetOpenRequest: Action {
    override val name: String
        get() = "contact:get:request"
    override val parameters: List<Parameter>
        get() = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val requests = UserContactDB.getOpenRequestsFrom(sender)

        val requestsArray = JSONArray()
        requests.forEach { requestsArray.put(toJSON(DetailedUserDB.get(it))) }
        return JSONObject().put("requests", requestsArray)
    }
}

/**
 * Action to search users based on their name
 */
class ContactSearchAction: Action {
    override val name: String = "contact:search"
    override val parameters: List<Parameter> = listOf(Parameter("keyword"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val keyWord = request.getString("keyword")

        val users = UserDB.getUsersLike(keyWord).toMutableList()
        users -= UserDB.get(sender)

        val userArray = JSONArray()

        for (user in users) {
            val settings = UserSettingsDB.get(user.uuid)

            if (settings.friendRequestLevel == FriendRequestLevel.ALL) {
                val profile = getUserProfileBasedOnSettings(settings, user, sender)
                userArray.put(toJSON(profile))
            }
            if (settings.friendRequestLevel == FriendRequestLevel.FRIENDS_OF_FRIENDS) {
                val friendsOfUser = UserHandler.getOfflineUser(user.uuid).contacts
                if (friendsOfUser.any { UserContactDB.areFriends(sender, it) }) {
                    val profile = getUserProfileBasedOnSettings(settings, user, sender)
                    userArray.put(toJSON(profile))
                }
            }

        }
        return JSONObject().put("users", userArray)
    }

    private fun getUserProfileBasedOnSettings(settings: UserSettings, user: UserData, sender: UUID): UserProfile {
        return when (settings.dataOptions) {
            DataOptions.NONE -> { createAnonymousUser(user.username, user.uuid) }
            DataOptions.ALL -> { DetailedUserDB.get(user.uuid) }
            DataOptions.ONLY_CONTACTS -> {
                if (UserContactDB.areFriends(sender, user.uuid)) {
                    DetailedUserDB.get(user.uuid)
                } else {
                    createAnonymousUser(user.username, user.uuid)
                }
            }
        }
    }

}