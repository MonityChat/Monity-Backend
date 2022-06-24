package de.devin.monity.network.wsrouting.actions

import de.devin.monity.network.db.user.*
import de.devin.monity.util.Status
import de.devin.monity.util.dataconnectors.UserHandler
import de.devin.monity.util.notifications.UserUpdatesProfileNotification
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*
import de.devin.monity.util.Error

/**
 * Updates the profile of the User
 * @see UserProfile
 */
class ProfileUpdateAction: Action {

    override val name: String
        get() = "profile:update"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("description"), Parameter("status"), Parameter("shortStatus"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val description = request.getString("description")
        val status = Status.valueOf(request.getString("status"))
        val shortStatus = request.getString("shortStatus")

        val oldProfile = DetailedUserDB.get(sender)
        val newProfile = UserProfile(oldProfile.userName, oldProfile.profileImageLocation, sender, description, status, shortStatus, oldProfile.lastSeen, status)
        DetailedUserDB.updateProfile(sender, newProfile)

        if (UserSettingsDB.get(sender).dataOptions != DataOptions.NONE) {
            for (contact in UserHandler.getOnlineUser(sender).contacts) {
                UserHandler.sendNotificationIfOnline(contact, UserUpdatesProfileNotification(sender))
            }
        }

        return toJSON(newProfile)
    }
}

/**
 * Returns the profile of another user
 * @see UserProfile
 */
class ProfileGetOtherAction: Action {
    override val name: String
        get() = "profile:get:other"
    override val parameters: List<Parameter>
        get() = listOf(Parameter("target"))

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val targetUUID = UUID.fromString(request.getString("target"))

        if (UserContactDB.hasBlocked(targetUUID, sender)) return Error.TARGET_BLOCKED_USER.toJson()

        val profile = DetailedUserDB.get(targetUUID)
        return toJSON(profile)
    }
}

/**
 * Returns the profile of the user
 * @see UserProfile
 */
class ProfileGetSelfAction: Action {

    override val name: String
        get() = "profile:get:self"
    override val parameters: List<Parameter>
        get() = listOf()

    override fun execute(sender: UUID, request: JSONObject): JSONObject {
        val profile = DetailedUserDB.get(sender)
        return toJSON(profile)
    }
}