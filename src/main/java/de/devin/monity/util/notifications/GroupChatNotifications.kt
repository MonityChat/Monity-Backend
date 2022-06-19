package de.devin.monity.util.notifications

import de.devin.monity.network.db.chat.GroupProfileDB
import de.devin.monity.network.db.chat.MessageData
import de.devin.monity.network.db.user.DetailedUserDB
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

