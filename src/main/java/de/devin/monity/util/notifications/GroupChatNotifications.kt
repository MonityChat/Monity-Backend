package de.devin.monity.util.notifications

import de.devin.monity.network.db.chat.GroupProfileDB
import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.user.DetailedUserDB
import de.devin.monity.network.db.user.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

/**
 * Occurs when a message is received
 * @param sender the user who sent the message
 * @param chatID the group where the message was sent
 * @param message the message
 */
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

/**
 * Occurs when a user deleted a message
 * @param sender the user who deleted the message
 * @param chatID the chat where the message was deleted
 * @param message id of the message which was deleted
 */
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

/**
 * Occurs when a message was edit in a group chat
 * @param sender the user who edit the message
 * @param chatID in which chat the message was edited
 * @param message the new message
 */
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

/**
 * Occurs when a user wants to join the group
 * @param sender the user who wants to join
 * @param groupID the group he wants to join
 * @param request his request
 */
class GroupChatUserRequestedJoinNotification(private val sender: UUID, private val groupID: UUID, private val request: String): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:request:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("request", request).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}

/**
 * Occurs when a user invited another user
 * @param sender the user who invited the other ser
 * @param groupID in which group he was invited
 */
class GroupChatUserInvitedYouNotification(private val sender: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:invited:you"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}

/**
 * Occurs when an invitation got declined
 * @param sender the user who declined
 * @param user the user who was declined
 * @param groupID the group the user was declined from
 */
class GroupChatRequestDeclinedNotification(private val sender: UUID, private val user: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:request:declined"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("requester",toJSON(DetailedUserDB.get(user)) ).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}

/**
 * Occurs when a user wants to join a group
 * @param sender the user who wants to join
 * @param groupID the group whe wants to join
 */
class GroupChatUserJoinedNotification(private val sender: UUID, private  val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:user:joined"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}

/**
 * Occurs when a request was accepted
 * @param sender the user who accepted the request
 * @param user the user who was accepted
 * @param groupID the group whe was accepted to
 */
class GroupChatRequestAcceptedNotification(private val sender: UUID, private val user: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:request:accepted"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("requester",toJSON(DetailedUserDB.get(user)) ).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}


/**
 * occurs when a user invited another user
 * @param sender the user who invited another user
 * @param user the user who was invited
 * @param groupID the group the user was invited to
 */
class GroupChatUserInvitedUserNotification(private val sender: UUID, private val user: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:invited:user"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("chat", toJSON(GroupProfileDB.get(groupID))).put("user", DetailedUserDB.get(user)))
        return json
    }
}

/**
 * Occurs when a user canceled the invitation of the receiver
 * @param sender the sender who canceled the invite
 * @param groupID to which group the invite was cancelled
 */
class GroupChatUserCanceledYourInvitationNotification(private val sender: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:invited:canceled:you"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("chat", toJSON(GroupProfileDB.get(groupID))))
        return json
    }
}

/**
 * Occurs when a users invite was canceled
 * @param sender the user who canceled the invitation
 * @param user the user whose invitation was canceled
 */
class GroupChatInvitationCanceledNotification(private val sender: UUID, private val user: UUID, private val groupID: UUID): Notification {
    override val from: UUID
        get() = sender
    override val name: String
        get() = "chat:group:invited:canceled"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        json.put("content", JSONObject().put("from", toJSON(DetailedUserDB.get(sender))).put("chat", toJSON(GroupProfileDB.get(groupID))).put("user", DetailedUserDB.get(user)))
        return json
    }
}

