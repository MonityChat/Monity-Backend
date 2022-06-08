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

        if (UserContactDB.has(sender)) {
            val friendData = UserContactDB.get(sender)
            if (friendData.any { it.to == targetUUID }) return Error.ALREADY_MADE_CONTACT.toJson()

            if (friendData.any { it.to == targetUUID && it.status == FriendStatus.PENDING }) {
                UserContactDB.updateStatus(sender, targetUUID, FriendStatus.ACCEPTED)
                UserHandler.sendNotificationIfOnline(targetUUID, ContactAddNotification(senderUserProfile))
                return toJSON(senderUserProfile)
            }
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

        val target = request.getString("target")
        val targetUUID = UUID.fromString(target)

        if (UserContactDB.has(sender)) {
            val friendData = UserContactDB.get(sender)
            if (friendData.any { it.to == targetUUID && it.status == FriendStatus.ACCEPTED}) return Error.ALREADY_MADE_CONTACT.toJson()
            if (friendData.any { it.to == targetUUID && it.status == FriendStatus.BLOCKED}) return Error.USER_BLOCKED_TARGET.toJson()
        } else {
            return Error.INVALID_FRIEND_ACCEPT_REQUEST.toJson()
        }
        
        return Error.NONE.toJson()
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