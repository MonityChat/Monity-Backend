package de.devin.monity.util.notifications

import de.devin.monity.network.db.user.UserProfile
import de.devin.monity.util.toJSON
import org.json.JSONObject
import java.util.*

/**
 * Occurs when a friendsrequest is incoming
 * @param user the who sent the request
 */
class ContactAddRequestNotification(val user: UserProfile): Notification {

    override val from: UUID = user.uuid
    override val name: String = "friend:request:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(user)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}

/**
 * Occurs when a friendsrequest was accepted
 * @param user the user who accepted the friendrequest
 */
class ContactAddNotification(val user: UserProfile): Notification {

    override val from: UUID = user.uuid
    override val name: String = "friend:accept:income"

    override fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("notification", name)

        val userJson = toJSON(user)

        json.put("content", JSONObject().put("from", userJson))
        return json
    }
}