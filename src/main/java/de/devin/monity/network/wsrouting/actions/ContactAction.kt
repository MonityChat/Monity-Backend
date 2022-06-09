package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.DetailedUserDB
import de.devin.monity.network.db.FriendData
import de.devin.monity.network.db.UserContactDB
import de.devin.monity.network.db.UserDB
import de.devin.monity.util.toJSON
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import de.devin.monity.util.Error
import de.devin.monity.util.FriendStatus
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.ContactAddNotification
import de.devin.monity.util.notifications.ContactAddRequestNotification

class ContactAddAction: Action {

    override val name: String = "contact:add"
    override val parameters: List<Parameter> = listOf(Parameter("target"))
    override fun execute(sender: UUID, request: JSONObject): JSONObject {

        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        val senderUserProfile = DetailedUserDB.get(sender)

        if (UserContactDB.areFriends(sender, targetUUID)) {
            return Error.ALREADY_MADE_CONTACT.toJson()
        }

        if (UserContactDB.sendRequest(targetUUID, sender)) {
            UserContactDB.updateStatus(sender, targetUUID, FriendStatus.ACCEPTED)
            UserHandler.sendNotificationIfOnline(targetUUID, ContactAddNotification(senderUserProfile))
            return toJSON(senderUserProfile)
        }

        val friendData = FriendData(sender, targetUUID, FriendStatus.PENDING)
        UserContactDB.insert(listOf(friendData))

        UserHandler.sendNotificationIfOnline(targetUUID, ContactAddRequestNotification(senderUserProfile))
        return Error.NONE.toJson()
    }
}

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
        return toJSON(DetailedUserDB.get(targetUUID))
    }
}

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

class ContactGetAction: Action {

    override val name: String
        get() = "contact:get"
    override val parameters: List<Parameter>
        get() = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val contacts = UserContactDB.getContactsFrom(sender)

        val contactsArray = JSONArray()
        contacts.forEach { contactsArray.put(toJSON(DetailedUserDB.get(it))) }
        return JSONObject().put("contacts", contactsArray)
    }
}

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

class ContactSearchAction: Action {
    override val name: String = "contact:search"
    override val parameters: List<Parameter> = listOf(Parameter("keyword"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val keyWord = request.getString("keyword")

        val users = UserDB.getUsersLike(keyWord)

        val userArray = JSONArray()

        for (user in users) {
            userArray.put(toJSON(DetailedUserDB.get(UUID.fromString(user.uuid))))
        }
        return JSONObject().put("users", userArray)
    }
}